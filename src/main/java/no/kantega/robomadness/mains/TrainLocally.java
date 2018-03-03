package no.kantega.robomadness.mains;

import no.kantega.robomadness.ai.GameAI;
import no.kantega.robomadness.ai.pipeline.Dl4jUtils;
import no.kantega.robomadness.ai.pipeline.persistence.PipelineConfig;
import no.kantega.robomadness.ai.pipeline.training.MLTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Paths.get;

public class TrainLocally {

    static Logger logger = LoggerFactory.getLogger(TrainLocally.class);

    static final Path home =
            get(System.getProperty("user.home") + "/pollti-workshop").toAbsolutePath();

    static final Path confPath =
            home.resolve("conf.pipeline");

    public static void main(String[] args) {
        try {
            Files.createDirectories(home);

            GameAI.train()
                    .bind(PipelineConfig::write)
                    .bind(bytes -> MLTask.trySupply(() -> Files.write(confPath, bytes)))
                    .bind(u -> MLTask.trySupply(() -> Files.readAllBytes(confPath)))
                    .bind(PipelineConfig::read)
                    .map(conf -> GameAI.toAgent(conf))
                    .execute()
                    .thenAccept(f -> {
                        System.out.println("The next value in the sequence 4,6,8 is " + f.f(Dl4jUtils.array(4, 6, 8)));
                    }).toCompletableFuture().get();

        } catch (Exception e) {
            logger.error("Well this is a disappointment!", e);
        }
    }
}
