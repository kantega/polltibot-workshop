package no.kantega.polltibot.mains;

import fj.P;
import fj.P2;
import no.kantega.polltibot.Corpus;
import no.kantega.polltibot.FastTextMap;
import no.kantega.polltibot.NetInputToken;
import no.kantega.polltibot.StreamTransformers;
import no.kantega.polltibot.ai.pipeline.MLTask;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.ai.pipeline.training.StopCondition;
import no.kantega.polltibot.twitter.TwitterStore;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static no.kantega.polltibot.NetInputToken.Word;
import static no.kantega.polltibot.NetInputToken.toToken;
import static no.kantega.polltibot.ai.pipeline.Dl4jUtils.atIndex;
import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

public class Training {

    static final Random random = new Random();
    static final int maxWords = 5;

    public static MLTask<P2<PipelineConfig, FastTextMap>> train(Path pathToFastTextFile) {
        return FastTextMap.load(pathToFastTextFile).time("FastTextMapping",System.out)
                .bind(fastText ->
                        TwitterStore.getStore().corpus(Corpus.politi)
                                .map(StreamTransformers.words())
                                .map(StreamTransformers.transformer(words -> words.map(fastText::asToken).collect(Collectors.toList())))
                                .map(StreamTransformers.padRight(NetInputToken.padding(), maxWords))
                                .map(StreamTransformers.truncate(maxWords))
                                .map(StreamTransformers.batch(4))
                                .map(StreamTransformers.transformer(Training::toRnnDataSet))
                                .apply(createNet(), MLTask::fit).time("Training epoch",System.out)
                                .repeat(() -> StopCondition.times(2))
                                .map(PipelineConfig::newEmptyConfig)
                                .map(cfg -> P.p(cfg, fastText))).time("Total",System.out);
    }


    public static MLTask<List<String>> generate(FastTextMap ftm, PipelineConfig config, String initWord, int count) {
        return MLTask.trySupply(() -> {//Create input for initialization
            MultiLayerNetwork net = config.net;
            String inputWord = initWord;
            List<String> generated = new ArrayList<>();
            net.rnnClearPreviousState();
            INDArray initializationInput = Nd4j.zeros(1, 300);
            net.rnnTimeStep(initializationInput);

            for (int i = 0; i < count; i++) {
                INDArray input = ftm.asToken(inputWord).asWord().vector;

                INDArray output = net.rnnTimeStep(input);

                List<String> topWords = ftm.wordForVec(output, 10);
                System.out.println("Found "+ topWords);
                String topWord = topWords.get(random.nextInt(topWords.size()));
                generated.add(topWord);
                inputWord = topWord;
            }
            return generated;
        });
    }

    public static MultiLayerNetwork createNet() {
        int lstmLayerSize = 400;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
                .learningRate(0.1)
                .seed(12345)
                .regularization(true)
                .l2(0.001)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.RMSPROP)
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(300).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(1, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(2, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.IDENTITY)
                        .nIn(lstmLayerSize).nOut(300).build())
                .backpropType(BackpropType.Standard)
                .pretrain(false).backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();


        return net;
    }


    public static DataSet toRnnDataSet(List<List<NetInputToken>> batch) {
        int exampleLength = batch.get(0).size();
        int currMinibatchSize = batch.size();
        //Allocate space:
        //Note the order here:
        // dimension 0 = number of examples in minibatch
        // dimension 1 = size of each vector (i.e., number of characters)
        // dimension 2 = length of each time series/example
        //Why 'f' order here? See http://deeplearning4j.org/usingrnns.html#data section "Alternative: Implementing a custom DataSetIterator"
        INDArray features = Nd4j.create(new int[]{currMinibatchSize, 300, exampleLength}, 'f');
        INDArray labels = Nd4j.create(new int[]{currMinibatchSize, 300, exampleLength}, 'f');

        INDArray featuresMask = Nd4j.create(new int[]{currMinibatchSize, exampleLength}, 'f');
        INDArray labelsMask = Nd4j.create(new int[]{currMinibatchSize, exampleLength}, 'f');

        for (int i = 0; i < currMinibatchSize; i++) {
            List<NetInputToken> tokens = batch.get(i);
            Word prevToken = tokens.get(0).asWord();

            for (int j = 1; j < tokens.size(); j++) {
                NetInputToken token = tokens.get(i);
                if (token.isWord()) {
                    Word nextToken = token.asWord();    //Next character to predict
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
