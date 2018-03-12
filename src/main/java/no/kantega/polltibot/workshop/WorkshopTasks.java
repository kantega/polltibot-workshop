package no.kantega.polltibot.workshop;

import no.kantega.polltibot.ai.pipeline.MLPipe;
import no.kantega.polltibot.ai.pipeline.MLTask;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.workshop.tools.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.DataSet;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static no.kantega.polltibot.workshop.tools.StreamTransformers.transformList;
import static no.kantega.polltibot.workshop.tools.StreamTransformers.transformer;

public class WorkshopTasks {


    static public MLTask<Stream<String>> loadInput() {
        return Util.loadTweets();
    }

    static public MLTask<FastTextMap> loadFastText() {
        return FastTextMap.load(Settings.fastTextPath);
    }

    static public MLPipe<Stream<String>, Stream<List<String>>> tokenize() {
        return MLPipe.fail(ANSI.redUncheck("Task 1 not implemented yet"));
    }

    static public MLPipe<Stream<List<String>>, Stream<List<Optional<Token>>>> toTokens(FastTextMap fastText) {
        return MLPipe.fail(ANSI.redUncheck("Task 2 not implemented yet"));
    }

    static public MLPipe<Stream<List<Optional<Token>>>, Stream<List<Token>>> stripEmpties() {
        return MLPipe.fail(ANSI.redUncheck("Task 3 not implemented yet"));
    }

    static public MLPipe<Stream<List<Token>>, Stream<List<Token>>> truncate() {
        return MLPipe.fail(ANSI.redUncheck("Task 4 not implemented yet"));
    }

    static public MLPipe<Stream<List<Token>>, Stream<List<Token>>> pad() {
        return MLPipe.fail(ANSI.redUncheck("Task 5 not implemented yet"));
    }

    static public MLPipe<Stream<List<Token>>, Stream<List<List<Token>>>> batch() {
        return MLPipe.fail(ANSI.redUncheck("Task 6 not implemented yet"));
    }

    static public MLPipe<Stream<List<List<Token>>>, Stream<DataSet>> toDataset(FastTextMap fastTextMap) {
        return MLPipe.fail(ANSI.redUncheck("Task 7 not implemented yet"));
    }

    static public MLPipe<Stream<DataSet>, MultiLayerNetwork> fit() {
        return MLPipe.fail(ANSI.redUncheck("Task 8 not implemented yet"));
    }

    static public MLPipe<MultiLayerNetwork, MultiLayerNetwork> repeatUntil() {
        return MLPipe.fail(ANSI.redUncheck("Task 9 not implemented yet"));
    }

    static public MLPipe<MultiLayerNetwork, PipelineConfig> asConfiguration() {
        return MLPipe.fail(ANSI.redUncheck("Task 10 not implemented yet"));
    }

    public static void main(String[] args) {
        loadFastText().print(ANSI.greenCheck("Loaded fasttext vectors"))
                .bind(fastText ->
                        tokenize().print(ANSI.greenCheck("Converted to words"))
                                .then(toTokens(fastText)).print(ANSI.greenCheck("Transformed to domain objects"))
                                .then(stripEmpties()).print(ANSI.greenCheck("Filtered out empty tokens"))
                                .then(truncate()).print(ANSI.greenCheck("Truncated tweets"))
                                .then(pad()).print(ANSI.greenCheck("Lists padded"))
                                .then(batch()).print(ANSI.greenCheck("Tweets put in minibatch"))
                                .then(toDataset(fastText)).print(ANSI.greenCheck("Converted to dataset"))
                                .then(fit()).print(ANSI.greenCheck("Fitted net to epoch"))
                                .then(repeatUntil())
                                .then(asConfiguration()).print(ANSI.greenCheck("Stored in config object"))
                                .append(config -> PipelineConfig.save(config, Settings.modelPath)).print(ANSI.greenCheck("Saved to file"))
                                .append(config -> VaeTraining.generateVae(fastText, config))
                                .append(generated->MLTask.run(()->generated.forEach(System.out::println)))
                                .apply(loadInput().print(ANSI.greenCheck("Loaded input")))

                ).executeAndAwait(
                a -> System.out.println(ANSI.ANSI_YELLOW + "Yay, you made it!" + ANSI.ANSI_RESET),
                ex -> System.err.println(ex.getCause().getMessage())

        );

    }

}
