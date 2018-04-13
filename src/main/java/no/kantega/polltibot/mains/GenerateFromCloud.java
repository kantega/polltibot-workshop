package no.kantega.polltibot.mains;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.workshop.Settings;
import no.kantega.polltibot.workshop.tools.FastTextMap;
import no.kantega.polltibot.workshop.tools.VaeTraining;

public class GenerateFromCloud {

    public static void main(String[] args) {
        try {

            Storage storage = StorageOptions.getDefaultInstance().getService();

            byte[] input = storage.readAllBytes("pollti-bot-workshop", "eval.pipeline");
            FastTextMap.load(Settings.fastTextPath,1000000)
                    .bind(fastText ->
                            PipelineConfig.read(input)
                                    .bind(cfg -> VaeTraining.generateVae(fastText, cfg))
                                    .onExecution(f -> f.forEach(System.out::println)))
                    .executeAndAwait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
