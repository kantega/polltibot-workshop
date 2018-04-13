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

import static fj.Function.apply;
import static no.kantega.polltibot.ai.pipeline.MLPipe.*;
import static no.kantega.polltibot.workshop.tools.StreamTransformers.*;
import static no.kantega.polltibot.workshop.tools.Util.*;

public class WorkshopTasks {


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
        return notImplemented(ANSI.redUncheck("Task 1 not implemented yet"));
    }

    static public List<Optional<Token>> toTokens(List<String> input, FastTextMap fastText) {
        return notImplemented(ANSI.redUncheck("Task 2 not implemented yet"));
    }

    static public List<Token> stripEmpties(List<Optional<Token>>  input) {
        return notImplemented(ANSI.redUncheck("Task 3 not implemented yet"));
    }

    static public List<Token> truncate(List<Token> input, int size) {
        return notImplemented(ANSI.redUncheck("Task 4 not implemented yet"));
    }

    static public List<Token> pad(List<Token> input,int wordsLength) {
        return notImplemented(ANSI.redUncheck("Task 5 not implemented yet"));
    }

    static public Stream<List<List<Token>>>  batch(Stream<List<Token>> input,int minibatchSize) {
        return notImplemented(ANSI.redUncheck("Task 6 not implemented yet"));
    }

    static public DataSet toDataset(List<List<Token>> input, FastTextMap fastTextMap) {
        return notImplemented(ANSI.redUncheck("Task 7 not implemented yet"));
    }

    static public MultiLayerNetwork fit(Stream<DataSet> input, MultiLayerNetwork net) {
        return notImplemented(ANSI.redUncheck("Task 8 not implemented yet"));
    }

    static public StopCondition<MultiLayerNetwork> repeatUntil() {
        return notImplemented(ANSI.redUncheck("Task 9 not implemented yet"));
    }

    static public PipelineConfig asConfiguration(MultiLayerNetwork input) {
        return notImplemented(ANSI.redUncheck("Task 10 not implemented yet"));
    }

    public static void main(String[] args) {
        MultiLayerNetwork net = VaeTraining.createVAE();
          loadFastText().print(ANSI.greenCheck("Loaded fasttext vectors"))
          .bind(fastText ->
            pipe(transformer(WorkshopTasks::split))
              .then(pipe(StreamTransformers.<List<String>,List<Optional<Token>>>transformer(i->WorkshopTasks.toTokens(i,fastText)))
              .then(pipe(transformer(WorkshopTasks::stripEmpties)))
              .then(pipe(transformer(list->WorkshopTasks.truncate(list,Settings.maxWords))))
              .then(pipe(transformer(list->WorkshopTasks.pad(list,Settings.maxWords))))
              .then(pipe(s->WorkshopTasks.batch(s,Settings.miniBatchSize)))
              .then(pipe(StreamTransformers.<List<List<Token>>,DataSet>transformer(batch->WorkshopTasks.toDataset(batch,fastText)))
              .then(pipe(dsStream->WorkshopTasks.fit(dsStream,net))))
              .then(task->task.repeat(WorkshopTasks::repeatUntil))
              .map(WorkshopTasks::asConfiguration)
              .append(config -> PipelineConfig.save(config, Settings.modelPath))
              .append(config -> VaeTraining.generateVae(fastText, config))
              .append(generated -> MLTask.run(() -> generated.forEach(System.out::println))))
              .apply(loadInput().print(ANSI.greenCheck("Loaded input")))

          ).executeAndAwait(
          a -> System.out.println(ANSI.ANSI_YELLOW + "Yay, you made it!" + ANSI.ANSI_RESET),
          ex -> System.err.println(ex.getCause().getMessage())

        );

    }

}
