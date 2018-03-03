package no.kantega.robomadness.ai.pipeline.labelling;

import fj.Ord;
import fj.data.Set;
import fj.data.TreeMap;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.function.Function;

public class OneHotCategoryLabelEncoder implements Function<String, INDArray> {
    public final TreeMap<String, Integer> headers;
    public final int                      size;

    public OneHotCategoryLabelEncoder(Set<String> headers) {
        this.headers = headers.toList().zipIndex().foldLeft((map, pair) -> map.set(pair._1(), pair._2()), TreeMap.<String, Integer>empty(Ord.stringOrd));
        this.size = headers.size();
    }

    @Override
    public INDArray apply(String a) {

        INDArray arr =
            Nd4j.zeros(size);
        headers.get(a).foreachDoEffect(i -> arr.putScalar(i, 1.0));
        return arr;

    }
}
