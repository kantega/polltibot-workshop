package no.kantega.robomadness.mains;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import no.kantega.robomadness.ai.GameAI;
import no.kantega.robomadness.ai.pipeline.Dl4jUtils;
import no.kantega.robomadness.ai.pipeline.persistence.PipelineConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLocally {
    final static Logger logger = LoggerFactory.getLogger(TestLocally.class);

    public static void main(String[] args) {
        try {

            Storage storage = StorageOptions.getDefaultInstance().getService();

            byte[] input = storage.readAllBytes("pollti-bot-workshop", "eval.pipeline");

            PipelineConfig.read(input)
                    .map(GameAI::toAgent)
                    .execute()
                    .thenAccept(f -> {
                        System.out.println("The next value in the sequence 4,6,8 is " + f.f(Dl4jUtils.array(4, 6, 8)));
                    }).toCompletableFuture().get();

        } catch (Exception e) {
            logger.error("Well this is a disappointment!", e);
        }
    }
}
