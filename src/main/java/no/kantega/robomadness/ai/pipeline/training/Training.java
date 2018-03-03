package no.kantega.robomadness.ai.pipeline.training;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.DataSet;

import java.util.stream.Stream;

public class Training {

    public static ProcessingStateMachine<MultiLayerNetwork> train(
        Stream<DataSet> records,
        MultiLayerNetwork net) {

        return ProcessingStateMachine.work(() -> {
            records.forEach(net::fit);
            return ProcessingStateMachine.value(net);
        });
    }


}






