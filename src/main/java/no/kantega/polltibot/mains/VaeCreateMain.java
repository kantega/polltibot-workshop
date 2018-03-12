package no.kantega.polltibot.mains;

import no.kantega.polltibot.workshop.Settings;
import no.kantega.polltibot.workshop.tools.FastTextMap;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.workshop.tools.VaeTraining;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class VaeCreateMain {



    static final Path modelPath =
            Paths.get(System.getProperty("user.home") + "/data/vae/pollti.net").toAbsolutePath();

    public static void main(String[] args) {
        List<String> words =
                FastTextMap.load(Settings.fastTextPath).time("Loading FastText Vectors",System.out)
                        .bind(ftm ->
                                PipelineConfig.read(modelPath).time("Reading model", System.out)
                                        .bind(config -> VaeTraining.generateVae(ftm, config))
                                        .time("Generating tweet", System.out))
                        .executeAndAwait();

        String tweet = words.stream().reduce((a, b) -> (a + " " + b)).orElse("Nothing tweeted!");

        System.out.println(tweet);
    }
}
