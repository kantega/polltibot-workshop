package no.kantega.robomadness.ai.pipeline.preprocessing;

import fj.data.List;
import no.kantega.robomadness.ai.pipeline.OffsetINDArrayPart;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Converter<A> implements Function<A, INDArray>,Serializable {

    public final List<String>                headers;
    public final int size;
    public final BiConsumer<A, INDArrayPart> writer;

    public Converter(List<String> headers, BiConsumer<A, INDArrayPart> writer) {
        this.headers = headers;
        this.writer = writer;
        this.size = headers.length();
    }

    @Override
    public INDArray apply(A a) {

        INDArray arr =
            Nd4j.create(size);
        writer.accept(a, new OffsetINDArrayPart(arr, 0));
        return arr;

    }

    /**
     * Create a converter that converts to arrays
     *
     * @param headers
     * @param writer
     * @param <A>
     * @return
     */
    static <A> Converter<A> converter(List<String> headers, BiConsumer<A, INDArrayPart> writer) {
        return new Converter<>(headers, writer);
    }


}
