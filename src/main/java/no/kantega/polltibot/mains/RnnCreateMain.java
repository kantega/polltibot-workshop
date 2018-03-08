package no.kantega.polltibot.mains;

import no.kantega.polltibot.FastTextMap;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RnnCreateMain {

    static final Path fastTextPath =
            Paths.get(System.getProperty("user.home") + "/data/fastText/wiki.no.vec").toAbsolutePath();

    static final Path modelPath =
            Paths.get(System.getProperty("user.home") + "/data/rnn/pollti.rnn").toAbsolutePath();

    public static void main(String[] args) {
        List<String> words =
                FastTextMap.load(fastTextPath).time("Loading FastText Vectors",System.out)
                        .bind(ftm ->
                                PipelineConfig.read(modelPath).time("Reading model", System.out)
                                        .bind(config -> Training.generate(ftm, config, "jalla", 15)).time("Generating tweet", System.out)).executeAndAwait();

        String tweet = words.stream().reduce((a, b) -> (a + " " + b)).orElse("Nothing tweeted!");

        System.out.println(tweet);
    }
}
