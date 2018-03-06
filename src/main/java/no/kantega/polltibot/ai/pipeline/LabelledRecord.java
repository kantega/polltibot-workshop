package no.kantega.polltibot.ai.pipeline;

import java.util.function.Function;

public class LabelledRecord<A, L> {

    public final A value;
    public final L label;

    public LabelledRecord(A value, L label) {
        this.value = value;
        this.label = label;
    }


    public <B> LabelledRecord<B, L> map(Function<A, B> f) {
        return new LabelledRecord<>(f.apply(value), label);
    }

    public <M> LabelledRecord<A, M> mapLabel(Function<L, M> f) {
        return new LabelledRecord<>(value, f.apply(label));
    }

    public static <A,L> Function<A,LabelledRecord<A,L>> toLabelledRecord(Function<A,L> labelExtractor){
        return a -> new LabelledRecord<>(a,labelExtractor.apply(a));
    }

    public static <A,B,L> StreamTransformer<LabelledRecord<A,L>,LabelledRecord<B,L>> transformFeatures(Function<A,B> converter){
        return stream->stream.map(labelled->labelled.map(converter));
    }
    public static <A,L,M> StreamTransformer<LabelledRecord<A,L>,LabelledRecord<A,M>> transformLabels(Function<L,M> converter){
        return stream->stream.map(labelled->labelled.mapLabel(converter));
    }
}
