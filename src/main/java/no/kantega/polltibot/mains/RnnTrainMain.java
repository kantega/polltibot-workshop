package no.kantega.polltibot.mains;

import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RnnTrainMain {

    static final Path fastTextPath =
            Paths.get(System.getProperty("user.home") + "/data/fastText/wiki.no.vec").toAbsolutePath();

    static final Path modelPath =
            Paths.get(System.getProperty("user.home") + "/data/rnn/pollti.rnn").toAbsolutePath();

    public static void main(String[] args) {
        Training.train(fastTextPath).bind(output -> PipelineConfig.save(output._1(), modelPath).time("Storing to disk", System.out)).executeAndAwait();
    }
}
