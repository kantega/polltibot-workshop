package no.kantega.polltibot.ai.pipeline.training;

import fj.data.Set;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.primitives.Pair;

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

public class Evaluate {


    public static double calculateScore(Stream<DataSet> testSet, MultiLayerNetwork network, boolean average) {
        DoubleAdder lossSum = new DoubleAdder();
        LongAdder exCount = new LongAdder();
        testSet.forEach(dataSet -> {
            int nEx = dataSet.getFeatureMatrix().size(0);
            lossSum.add(network.score(dataSet) * nEx);
            exCount.add(nEx);
        });


        if (average)
            return lossSum.doubleValue() / exCount.longValue();
        else
            return lossSum.doubleValue();
    }

    public static MLTask<Pair<MultiLayerNetwork,Evaluation>> evaluate(Stream<DataSet> testSet, MultiLayerNetwork net, Set<String> labels) {
        return MLTask.supplier(()-> {
            Evaluation eval = new Evaluation(labels.toJavaList());
            net.doEvaluation(new StreamDataSetIterator(testSet), eval);
            return Pair.of(net,eval);
        });
    }
}

