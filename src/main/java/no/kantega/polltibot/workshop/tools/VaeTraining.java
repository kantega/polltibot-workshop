package no.kantega.polltibot.workshop.tools;

import com.codahale.metrics.ConsoleReporter;
import fj.P;
import fj.P2;
import no.kantega.polltibot.ai.pipeline.MLTask;
import no.kantega.polltibot.ai.pipeline.persistence.PipelineConfig;
import no.kantega.polltibot.ai.pipeline.training.StopCondition;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.variational.GaussianReconstructionDistribution;
import org.deeplearning4j.nn.conf.layers.variational.VariationalAutoencoder;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static no.kantega.polltibot.workshop.Settings.maxWords;
import static no.kantega.polltibot.workshop.Settings.miniBatchSize;
import static no.kantega.polltibot.workshop.tools.StreamTransformers.*;

public class VaeTraining {


    public static MLTask<P2<PipelineConfig, FastTextMap>> trainVAE(Path pathToFastTextFile) {
        return FastTextMap.load(pathToFastTextFile,1000000).time("FastTextMapping", System.out)
                .bind(VaeTraining::trainVAE);
    }

    public static MLTask<P2<PipelineConfig, FastTextMap>> trainVAE(FastTextMap fastText) {
        return
                Util.loadTweets()
                        .map(words())
                        .map(transformList(fastText::asToken))
                        .map(nonEmpty())
                        .map(truncate(maxWords))
                        .map(padRight(Token.padding(), maxWords))
                        .map(batch(miniBatchSize))
                        .map(transformer(list -> VaeTraining.toVAEDataSet(fastText, list)))
                        .apply(createVAE(), MLTask::fit).time("Training epoch", System.out)
                        .repeat(() -> StopCondition.times(20))
                        .map(PipelineConfig::newConfig)
                        .map(cfg -> P.p(cfg, fastText));
    }


    public static MLTask<List<String>> generateVae(FastTextMap ftm, PipelineConfig config) {
        return MLTask.trySupply(() -> {//Create input for initialization
            MultiLayerNetwork net = config.net;
            org.deeplearning4j.nn.layers.variational.VariationalAutoencoder vae
                    = (org.deeplearning4j.nn.layers.variational.VariationalAutoencoder) net.getLayer(0);
            INDArray out = vae.generateAtMeanGivenZ(getLatentSpaceGrid(-5, 5, 10));

            List<String> tweets = new ArrayList<>();
            for (int i = 0; i < out.size(0); i++) {
                INDArray row = out.getRow(i);
                StringBuilder builder = new StringBuilder();
                INDArray[] words = new INDArray[maxWords];
                for (int wordIndex = 0; wordIndex < maxWords; wordIndex++) {
                    INDArray wordArray = row.get(NDArrayIndex.interval(wordIndex * 300, (wordIndex + 1) * 300));
                    words[wordIndex] = wordArray;
                }

                List<List<String>> topWords =
                        ftm.wordsForVec(words, 2);

                topWords.forEach(topWord -> builder.append(topWord.get(0)).append(" ")); //Get the closest match

                tweets.add(builder.toString());
            }
            tweets.forEach(System.out::println);
            ConsoleReporter.forRegistry(FastTextMap.registry).build().report();
            return tweets;
        });
    }

    //This simply returns a 2d grid: (x,y) for x=plotMin to plotMax, and y=plotMin to plotMax
    private static INDArray getLatentSpaceGrid(double plotMin, double plotMax, int plotSteps) {
        INDArray data = Nd4j.create(plotSteps * plotSteps, 2);
        INDArray linspaceRow = Nd4j.linspace(plotMin, plotMax, plotSteps);
        for (int i = 0; i < plotSteps; i++) {
            data.get(NDArrayIndex.interval(i * plotSteps, (i + 1) * plotSteps), NDArrayIndex.point(0)).assign(linspaceRow);
            int yStart = plotSteps - i - 1;
            data.get(NDArrayIndex.interval(yStart * plotSteps, (yStart + 1) * plotSteps), NDArrayIndex.point(1)).assign(linspaceRow.getDouble(i));
        }
        return data;
    }

    public static MultiLayerNetwork createVAE() {
        MultiLayerConfiguration multiLayerConfiguration =
                new NeuralNetConfiguration.Builder()
                        .seed(12345)
                        .learningRate(1e-3)
                        .updater(Updater.RMSPROP)
                        .weightInit(WeightInit.XAVIER)
                        .regularization(true).l2(1e-4)
                        .list()
                        .layer(0, new VariationalAutoencoder.Builder()
                                .nIn(300 * maxWords)
                                .activation(Activation.LEAKYRELU)
                                .encoderLayerSizes(300 * maxWords / 5)
                                .decoderLayerSizes(300 * maxWords / 5)
                                .pzxActivationFunction(Activation.IDENTITY)
                                .reconstructionDistribution(new GaussianReconstructionDistribution(Activation.TANH))
                                .nOut(2)
                                .build())
                        .pretrain(true)
                        .backprop(false)
                        .build();

        MultiLayerNetwork mnn = new MultiLayerNetwork(multiLayerConfiguration);
        mnn.init();
        return mnn;
    }

    public static DataSet toVAEDataSet(FastTextMap fastTextMap, List<List<Token>> batch) {
        INDArray[] features = new INDArray[batch.size()];
        INDArray[] masks = new INDArray[batch.size()];

        for (int i = 0; i < batch.size(); i++) {
            List<Token> tweet = batch.get(i);
            INDArray[] arrs = new INDArray[tweet.size()];
            INDArray[] mask = new INDArray[tweet.size()];
            for (int wordN = 0; wordN < tweet.size(); wordN++) {
                arrs[wordN] = tweet.get(wordN).toInput();
                mask[wordN] = tweet.get(wordN).isWord() ? Nd4j.ones(1, 300) : Nd4j.zeros(1, 300);
            }

            features[i] = Nd4j.concat(1, arrs);
            masks[i] = Nd4j.concat(1, mask);
        }

        INDArray featuresBatch = Nd4j.concat(0, features);
        INDArray maskBatch = Nd4j.concat(0, masks);
        return new DataSet(featuresBatch, featuresBatch);
    }


}
