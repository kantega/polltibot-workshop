package no.kantega.polltibot.mains;

import com.codahale.metrics.ConsoleReporter;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.workshop.tools.RnnTraining;
import no.kantega.polltibot.workshop.tools.VaeTraining;

import java.nio.file.Path;
import java.nio.file.Paths;

import static no.kantega.polltibot.workshop.Settings.fastTextPath;

public class VaeTrainMain {


    static final Path modelPath =
            Paths.get(System.getProperty("user.home") + "/data/vae/pollti.net").toAbsolutePath();

    public static void main(String[] args) {
        VaeTraining.trainVAE(fastTextPath)
                .bind(output -> VaeTraining.generateVae(output._2(), output._1()).thenJust(output))
                .bind(output -> PipelineConfig.save(output._1(), modelPath)
                        .time("Storing to disk", System.out))
                .executeAndAwait();
        ConsoleReporter.forRegistry(RnnTraining.registry).build().report();
    }
}
