package no.kantega.polltibot.workshop.tools;

import fj.P;
import fj.P2;
import no.kantega.polltibot.ai.pipeline.MLTask;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.ai.pipeline.training.StopCondition;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static no.kantega.polltibot.ai.pipeline.Dl4jUtils.atIndex;
import static no.kantega.polltibot.workshop.Settings.maxWords;
import static no.kantega.polltibot.workshop.Settings.miniBatchSize;
import static no.kantega.polltibot.workshop.tools.StreamTransformers.*;
import static no.kantega.polltibot.workshop.tools.Token.Word;
import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

public class RnnTraining {

    static final Random random = new Random();




    static final Path modelPath =
            Paths.get(System.getProperty("user.home") + "/data/rnn/");


    public static MLTask<P2<PipelineConfig, FastTextMap>> trainRnn(Path pathToFastTextFile) {
        return FastTextMap.load(pathToFastTextFile).time("FastTextMapping", System.out)
                .bind(fastText ->
                        Util.loadTweets()
                                .map(words())
                                .map(transformList(fastText::asToken))
                                .map(nonEmpty())
                                .map(split(maxWords))
                                .map(padRight(Token.padding(), maxWords))
                                .map(batch(miniBatchSize))
                                .map(transformer(RnnTraining::toRnnDataSet))
                                .apply(createRNN(), MLTask::fit).time("Training epoch", System.out)
                                .repeat(() -> StopCondition.times(25))
                                .bind(net -> PipelineConfig
                                        .save(PipelineConfig.newConfig(net), modelPath.resolve("pollti-" + System.currentTimeMillis() + ".rnn"))
                                        .thenJust(net))
                                //.repeat(() -> StopCondition.until(ZonedDateTime.now().plusDays(2).toInstant()))
                                .map(PipelineConfig::newConfig)
                                .map(cfg -> P.p(cfg, fastText))).time("Total", System.out);
    }



    public static MLTask<List<String>> generateRnn(FastTextMap ftm, PipelineConfig config, String initWord, int count) {
        return MLTask.trySupply(() -> {//Create input for initialization
            MultiLayerNetwork net = config.net;
            String inputWord = initWord;
            List<String> generated = new ArrayList<>();
            generated.add(initWord);
            net.rnnClearPreviousState();
            INDArray initializationInput = Nd4j.zeros(1, 300);
            net.rnnTimeStep(initializationInput);

            for (int i = 0; i < count; i++) {
                INDArray input = ftm.asToken(inputWord).orElseGet(ftm::randomWord).asWord().vector;

                INDArray output = net.rnnTimeStep(input);

                List<String> topWords = ftm.wordForVec(output, 3);
                String topWord = topWords.get(random.nextInt(topWords.size()));
                generated.add(topWord);
                inputWord = topWord;
            }
            return generated;
        });
    }




    public static MultiLayerNetwork createRNN() {
        int lstmLayerSize = 100;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                .gradientNormalizationThreshold(1.0)
                .iterations(1)
                .learningRate(0.0002)
                .seed(12345)
                .regularization(true)
                .l2(0.0001)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.RMSPROP)
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(300).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(1, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(lstmLayerSize).nOut(300).build())
                .backpropType(BackpropType.Standard)
                .pretrain(false).backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();


        return net;
    }



    //Allocate space:
    //Note the order here:
    // dimension 0 = number of examples in minibatch
    // dimension 1 = size of each vector (i.e., number of characters)
    // dimension 2 = length of each time series/example
    //Why 'f' order here? See http://deeplearning4j.org/usingrnns.html#data section "Alternative: Implementing a custom DataSetIterator"


    public static DataSet toRnnDataSet(List<List<Token>> batch) {
        int currMinibatchSize = batch.size();
        INDArray features = Nd4j.create(new int[]{miniBatchSize, 300, maxWords - 1}, 'f');
        INDArray labels = Nd4j.create(new int[]{miniBatchSize, 300, maxWords - 1}, 'f');
        INDArray featuresMask = Nd4j.zeros(new int[]{miniBatchSize, maxWords - 1}, 'f');
        INDArray labelsMask = Nd4j.zeros(new int[]{miniBatchSize, maxWords - 1}, 'f');

        for (int i = 0; i < currMinibatchSize; i++) {
            List<Token> tokens = batch.get(i);

            Word prevToken = tokens.get(0).asWord();

            for (int j = 0; j < tokens.size() - 1; j++) {
                Token token = tokens.get(j + 1);
                if (token.isWord()) {
                    Word nextToken = token.asWord();
                    features.put(atIndex(point(i), all(), point(j)), prevToken.vector);
                    labels.put(atIndex(point(i), all(), point(j)), nextToken.vector);
                    prevToken = nextToken;
                    featuresMask.putScalar(atIndex(i, j), 1.0);
                    labelsMask.putScalar(atIndex(i, j), 1.0);
                }
            }
        }
        return new DataSet(features, labels, featuresMask, labelsMask);
    }



}
