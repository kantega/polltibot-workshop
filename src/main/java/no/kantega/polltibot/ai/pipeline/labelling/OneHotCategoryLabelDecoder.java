package no.kantega.polltibot.ai.pipeline.labelling;

import fj.Ord;
import fj.data.Set;
import fj.data.TreeMap;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.function.Function;

public class OneHotCategoryLabelDecoder implements Function<INDArray, String> {

    public final TreeMap<Integer, String> headers;
    public final int                      size;

    public OneHotCategoryLabelDecoder(Set<String> headers) {
        this.headers =
            headers
                .toList()
                .zipIndex()
                .foldLeft((map, pair) -> map.set(pair._2(), pair._1()), TreeMap.<Integer, String>empty(Ord.intOrd));
        this.size = headers.size();
    }

    @Override
    public String apply(INDArray indArray) {
        int index = (int) Nd4j.argMax(indArray, 1).getDouble(0);
        return headers.get(index).orSome("Label not found");
    }
}
