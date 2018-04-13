package no.kantega.polltibot.mains;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import no.kantega.polltibot.ai.pipeline.MLTask;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.workshop.tools.FastTextMap;
import no.kantega.polltibot.workshop.tools.VaeTraining;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TrainInCloud {

    static Logger logger = LoggerFactory.getLogger(TrainInCloud.class);

    public static void main(String[] args) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            List<Acl> acls = new ArrayList<>();
            acls.add(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));


            FastTextMap.load(Paths.get("/opt/pollti-workshop/temp/wiki.no.vec"),100000)
                    .bind(fastText ->
                            VaeTraining.trainVAE(fastText)
                                    .bind(pair -> PipelineConfig.asBytes(pair._1()))
                                    .bind(bytes -> MLTask.supplier(() ->
                                            storage.create(
                                                    BlobInfo.newBuilder("pollti-bot-workshop", "eval.pipeline").setAcl(acls).build(),
                                                    bytes)))
                    )
                    .executeAndAwait();
        } catch (Exception e) {
            logger.error("Well this is a disappointment!", e);
        }

    }


}