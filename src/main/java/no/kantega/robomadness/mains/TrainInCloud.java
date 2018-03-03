package no.kantega.robomadness.mains;

import com.google.cloud.storage.*;
import no.kantega.robomadness.ai.GameAI;
import no.kantega.robomadness.ai.pipeline.persistence.PipelineConfig;
import no.kantega.robomadness.ai.pipeline.training.MLTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TrainInCloud {

    static Logger logger = LoggerFactory.getLogger(TrainInCloud.class);

    public static void main(String[] args) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            List<Acl> acls = new ArrayList<>();
            acls.add(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));


            GameAI.train()
                    .bind(PipelineConfig::write)
                    .bind(bytes -> MLTask.supplier(() ->
                            storage.create(
                                    BlobInfo.newBuilder("pollti-bot-workshop", "eval.pipeline").setAcl(acls).build(),
                                    bytes)))
                    .executeAndAwait();
        } catch (Exception e) {
            logger.error("Well this is a disappointment!", e);
        }

    }
}
