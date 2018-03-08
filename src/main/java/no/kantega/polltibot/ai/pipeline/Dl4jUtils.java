package no.kantega.polltibot.ai.pipeline;

import fj.Unit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.function.Function;

public class Dl4jUtils {

    public static int[] atIndex(int a, int ... as){
        return ArrayUtil.consArray(a,as);
    }

    public static INDArrayIndex[] atIndex(INDArrayIndex idx, INDArrayIndex ... idxs){
        int len = idxs.length;
        INDArrayIndex[] nas = new INDArrayIndex[len + 1];
        nas[0] = idx;
        System.arraycopy(idxs, 0, nas, 1, len);
        return nas;
    }

    public static INDArray array(double... vals) {
        return Nd4j.create(vals);
    }

    public static INDArray array(INDArray... rows) {
        return Nd4j.concat(0, rows);
    }

    public static MLTask<Unit> log(String msg, Logger logger) {
        return MLTask.supplier(() -> {
            logger.info(msg);
            return Unit.unit();
        });
    }

    public static <A> Function<A, MLTask<A>> logK(String msg, Logger logger) {
        return a -> MLTask.supplier(() -> {
            logger.info(msg);
            return a;
        });
    }

}
