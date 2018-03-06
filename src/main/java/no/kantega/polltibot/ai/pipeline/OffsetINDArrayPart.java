package no.kantega.polltibot.ai.pipeline;

import no.kantega.polltibot.ai.pipeline.preprocessing.INDArrayPart;
import org.nd4j.linalg.api.ndarray.INDArray;

public class OffsetINDArrayPart implements INDArrayPart {

    final INDArray array;
    final int      offset;

    public OffsetINDArrayPart(INDArray array, int offset) {
        this.array = array;
        this.offset = offset;
    }

    @Override
    public void write(int index, double value) {
        array.putScalar(index + offset, value);
    }
}
