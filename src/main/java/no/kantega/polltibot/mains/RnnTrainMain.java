package no.kantega.polltibot.mains;

import com.codahale.metrics.ConsoleReporter;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.workshop.tools.RnnTraining;

import java.nio.file.Path;
import java.nio.file.Paths;

import static no.kantega.polltibot.workshop.Settings.fastTextPath;

public class RnnTrainMain {


    static final Path modelPath =
            Paths.get(System.getProperty("user.home") + "/data/rnn/pollti.rnn").toAbsolutePath();

    public static void main(String[] args) {
        RnnTraining.trainRnn(fastTextPath).bind(output -> PipelineConfig.save(output._1(), modelPath).time("Storing to disk", System.out)).executeAndAwait();
    }
}
