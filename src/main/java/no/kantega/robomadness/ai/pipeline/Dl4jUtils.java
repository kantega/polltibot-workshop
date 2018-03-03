package no.kantega.robomadness.ai.pipeline;

import fj.Unit;
import no.kantega.robomadness.ai.pipeline.training.MLTask;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;

import java.util.function.Function;

public class Dl4jUtils {

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
