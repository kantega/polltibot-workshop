package no.kantega.polltibot.ai.pipeline;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamTransformers {



    public static <A,L> StreamTransformer<A,LabelledRecord<A,L>> label(Function<A,L> labelExtractor){
        return input-> input.map(a->new LabelledRecord<>(a,labelExtractor.apply(a)));
    }

    public static <A> StreamTransformer<A, java.util.List<A>> join() {
        return input -> {
            java.util.List<A> l = input.collect(Collectors.toList());
            return Stream.of(l);
        };
    }

    public static <A> StreamTransformer<java.util.List<A>, java.util.List<A>> shuffle() {
        return input -> input.map(list -> {
            Collections.shuffle(list);
            return list;
        });
    }

    public static <A, B> StreamTransformer<A, B> transformer(Function<A, B> f) {
        return input -> input.map(f);
    }

    public static StreamTransformer<List<UnlabeledRecord<INDArray>>, DataSet> toDataset() {
        return stream ->
            stream.map(batch -> {
                int        size = batch.size();
                INDArray[] features  = new INDArray[size];
                for (int i = 0; i < size; i++) {
                    features[i] = batch.get(i).value;
                }
                return new DataSet(Nd4j.concat(0, features), Nd4j.zeros(1, size));
            });
    }

    public static StreamTransformer<List<LabelledRecord<INDArray,INDArray>>, DataSet> toLabelledDataset() {
        return stream ->
            stream.map(batch -> {
                int        size = batch.size();
                INDArray[] features  = new INDArray[size];
                INDArray[] labels  = new INDArray[size];
                for (int i = 0; i < size; i++) {
                    labels[i] = batch.get(i).value;
                }
                return new DataSet(Nd4j.concat(0, features), Nd4j.concat(0, labels));
            });
    }

    public static <T> StreamTransformer<T, List<T>> batch(int batchSize) {
        //noinspection unchecked,rawtypes
        return source -> (Stream) StreamSupport.stream(new BatchSpliterator<>(source.spliterator(), batchSize), false)
            .onClose(source::close);
    }

    static class BatchSpliterator<T> extends Spliterators.AbstractSpliterator<List<T>> {

        private final Spliterator<? extends T> source;
        private final int                      batchSize;

        public BatchSpliterator(
            Spliterator<? extends T> source,
            int batchSize
        ) {
            super(1, ORDERED | IMMUTABLE);
            this.source = source;
            this.batchSize = batchSize;
        }

        @Override
        public boolean tryAdvance(Consumer<? super List<T>> consumer) {
            final List<T> batch = new ArrayList<>(batchSize);
            for (int i = 0; i < batchSize; i++) {
                if (!source.tryAdvance(batch::add)) {
                    if (!batch.isEmpty()) {
                        consumer.accept(batch);
                    }
                    return false;
                }
            }
            consumer.accept(batch);
            return true;
        }
    }
}
