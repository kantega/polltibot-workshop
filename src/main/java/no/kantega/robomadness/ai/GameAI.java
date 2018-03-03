package no.kantega.robomadness.ai;

import fj.F;
import no.kantega.robomadness.ai.pipeline.Dl4jUtils;
import no.kantega.robomadness.ai.pipeline.persistence.PipelineConfig;
import no.kantega.robomadness.ai.pipeline.training.MLTask;
import no.kantega.robomadness.ai.pipeline.training.StopCondition;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static no.kantega.robomadness.ai.pipeline.Dl4jUtils.array;

public class GameAI {

    static Logger logger = LoggerFactory.getLogger(GameAI.class);

    public static MLTask<PipelineConfig> train() {
        int numInputs = 3;
        int numOutput = 1;

        INDArray trainingQ = array(
                array(1, 2, 3),
                array(4, 7, 10),
                array(13, 15, 17)
        );

        INDArray trainingS = array(array(4), array(13), array(19));

        DataSet trainingDs = new DataSet(trainingQ, trainingS);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(System.currentTimeMillis())
                .weightInit(WeightInit.XAVIER)
                .activation(new ActivationReLU())
                .learningRate(0.0003)
                .regularization(true).l2(1e-4)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(3)
                        .build())
                .layer(1, new DenseLayer.Builder().nIn(3).nOut(3)
                        .build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(3).nOut(1).build())
                .backprop(true).pretrain(false)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        return Dl4jUtils.log("Starting training", logger)
                .bind(u -> MLTask.loadEpoch(() -> Stream.of(trainingDs))
                        .bind(s -> MLTask.fit(net, s))
                        .repeat(() -> StopCondition.times(1000))
                        .bind(n->Dl4jUtils.log("Score :"+n.score(),logger).thenJust(n))
                        .repeat(() -> StopCondition.times(10))
                        .bind(Dl4jUtils.logK("Done training 100k iterations", logger))
                        .map(PipelineConfig::newEmptyConfig));


    }

    public static F<INDArray, Double> toAgent(PipelineConfig config) {
        return input -> config.net.output(input).getDouble(0);
    }


}
