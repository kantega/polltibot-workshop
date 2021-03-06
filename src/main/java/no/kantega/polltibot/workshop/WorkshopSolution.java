package no.kantega.polltibot.workshop;

import no.kantega.polltibot.ai.pipeline.MLTask;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.ai.pipeline.training.StopCondition;
import no.kantega.polltibot.workshop.tools.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.DataSet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.kantega.polltibot.ai.pipeline.MLPipe.pipe;
import static no.kantega.polltibot.workshop.tools.StreamTransformers.transformer;
import static no.kantega.polltibot.workshop.tools.Util.loadTweets;
import static no.kantega.polltibot.workshop.tools.Util.notImplemented;

public class WorkshopSolution {

    static public MLTask<Stream<String>> loadInput() {
        return loadTweets();
    }

    static public MLTask<FastTextMap> loadFastText() {
        //Setter er en begrensning på 1000 ord for å spare tid innledningsvis.
        // Settes til Long.MAX når treninges skal begynne for alvor
        long max = 1000;
        return FastTextMap.load(Settings.fastTextPath, max);
    }

    static public List<String> split(String input) {
        return StreamTransformers.tokens(input);
    }

    static public List<Optional<Token>> toTokens(List<String> input,FastTextMap fastText) {
        return input.stream().map(fastText::asToken).collect(Collectors.toList());
    }

    static public List<Token> stripEmpties(List<Optional<Token>>  input) {
        return input.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    static public List<Token> truncate(List<Token> input,int size) {
        return
          (input.size() > size) ?
            input.subList(0, size) :
            input;
    }

    static public List<Token> pad(List<Token> input,int size) {
        while (input.size() < size) {
            input.add(Token.padding());
        }
        return input;
    }

    static public Stream<List<List<Token>>>  batch(Stream<List<Token>> input,int minbatchSize) {
        return StreamTransformers.<List<Token>>batch(minbatchSize).apply(input);
    }

    static public DataSet toDataset(List<List<Token>> input, FastTextMap fastText) {
        return VaeTraining.toVAEDataSet(fastText, input);
    }

    static public MultiLayerNetwork fit(Stream<DataSet> input,MultiLayerNetwork net) {
        input.forEach(net::fit);
        return net;
    }

    static public StopCondition<MultiLayerNetwork> repeatUntil() {
        return StopCondition.times(20);
    }

    static public PipelineConfig asConfiguration(MultiLayerNetwork input) {
        return PipelineConfig.newConfig(input);
    }

    public static void main(String[] args) {
        MultiLayerNetwork net = VaeTraining.createVAE();
        loadFastText().print(ANSI.greenCheck("Loaded fasttext vectors"))
          .bind(fastText ->
            pipe(transformer(WorkshopTasks::split)).print(()->ANSI.greenCheck("Converted to words"))
              .then(pipe(StreamTransformers.<List<String>,List<Optional<Token>>>transformer(i->WorkshopTasks.toTokens(i,fastText))).print(()->ANSI.greenCheck("Transformed to domain objects"))
                .then(pipe(transformer(WorkshopTasks::stripEmpties))).print(()->ANSI.greenCheck("Filtered out empty tokens"))
                .then(pipe(transformer(list->WorkshopTasks.truncate(list,Settings.maxWords)))).print(()->ANSI.greenCheck("Truncated tweets"))
                .then(pipe(transformer(list->WorkshopTasks.pad(list,Settings.maxWords)))).print(()->ANSI.greenCheck("Lists padded"))
                .then(pipe(s->WorkshopTasks.batch(s,Settings.miniBatchSize))).print(()->ANSI.greenCheck("Tweets put in minibatch"))
                .then(pipe(StreamTransformers.<List<List<Token>>,DataSet>transformer(batch->WorkshopTasks.toDataset(batch,fastText))).print(()->ANSI.greenCheck("Converted to dataset"))
                  .then(pipe(dsStream->WorkshopTasks.fit(dsStream,net)))).print(()->ANSI.greenCheck("Fitted net to epoch"))
                .then(task->task.repeat(WorkshopTasks::repeatUntil))
                .map(WorkshopTasks::asConfiguration).print(()->ANSI.greenCheck("Stored in config object"))
                .append(config -> PipelineConfig.save(config, Settings.modelPath)).print(()->ANSI.greenCheck("Saved to file"))
                .append(config -> VaeTraining.generateVae(fastText, config))
                .append(generated -> MLTask.run(() -> generated.forEach(System.out::println))))
              .apply(loadInput().print(ANSI.greenCheck("Loaded input")))

          ).executeAndAwait(
          a -> System.out.println(ANSI.ANSI_YELLOW + "Yay, you made it!" + ANSI.ANSI_RESET),
          ex -> System.err.println(ex.getCause().getMessage())

        );

    }



}
