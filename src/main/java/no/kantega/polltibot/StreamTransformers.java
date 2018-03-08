package no.kantega.polltibot;

import fj.Ord;
import fj.P;
import no.kantega.polltibot.ai.pipeline.LabelledRecord;
import no.kantega.polltibot.ai.pipeline.StreamTransformer;
import no.kantega.polltibot.ai.pipeline.UnlabeledRecord;
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

    public static <A> StreamTransformer<List<A>, List<A>> padRight(A a, int size) {
        return transformer(list -> {
            while (list.size() < size) {
                list.add(a);
            }
            return list;
        });
    }

    public static <A> StreamTransformer<List<A>, List<A>> truncate(int size) {
        return
                transformer(list ->
                        (list.size() > size) ?
                                list.subList(0, size) :
                                list
                );
    }

    public static <A> StreamTransformer<List<A>,List<A>> split(int size){
        return stream-> stream.flatMap(list->{
            List<List<A>> chunkList = new ArrayList<>();
            for (int i = 0 ; i <  list.size() ; i += size) {
                chunkList.add(new ArrayList<>(list.subList(i , i + size >= list.size() ? list.size() : i + size)));
            }
            return chunkList.stream();
        });
    }


    public static <A, B> StreamTransformer<List<A>, List<B>> transformList(Function<A, B> f) {
        return transformer(list -> list.stream().map(f).collect(Collectors.toList()));
    }

    public static <A> StreamTransformer<List<Optional<A>>, List<A>> nonEmpty() {
        return transformer(list -> list.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()));
    }

    public static StreamTransformer<String, List<String>> words() {
        return transformer(s -> tokens(s));
    }

    private static List<String> tokens(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (Character.isAlphabetic(c)) {
                word.append(c);
            } else {
                if (word.length() > 0) {
                    tokens.add(word.toString());
                    word = new StringBuilder();
                }
                if (!Character.isWhitespace(c))
                    tokens.add(digitToWord(String.valueOf(c)));
            }
        }
        if (word.length() > 0) {
            tokens.add(word.toString());
        }

        return tokens;
    }
    private static fj.data.TreeMap<String,String> d2w =
            fj.data.TreeMap.treeMap(
                    Ord.stringOrd,
                    P.p("0","null"),
                    P.p("1","en"),
                    P.p("2","to"),
                    P.p("3","tre"),
                    P.p("4","fire"),
                    P.p("5","fem"),
                    P.p("6","seks"),
                    P.p("7","syv"),
                    P.p("8","åtte"),
                    P.p("9","ni")
            );

    private static String digitToWord(String digit){
        return d2w.get(digit).orSome(digit);
    }

    public static <A> StreamTransformer<A, A> prepend(Stream<A> prefix) {
        return stream -> Stream.concat(prefix, stream);
    }

    public static <A, L> StreamTransformer<A, LabelledRecord<A, L>> label(Function<A, L> labelExtractor) {
        return input -> input.map(a -> new LabelledRecord<>(a, labelExtractor.apply(a)));
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
                    int size = batch.size();
                    INDArray[] features = new INDArray[size];
                    for (int i = 0; i < size; i++) {
                        features[i] = batch.get(i).value;
                    }
                    return new DataSet(Nd4j.concat(0, features), Nd4j.zeros(1, size));
                });
    }

    public static StreamTransformer<List<LabelledRecord<INDArray, INDArray>>, DataSet> toLabelledDataset() {
        return stream ->
                stream.map(batch -> {
                    int size = batch.size();
                    INDArray[] features = new INDArray[size];
                    INDArray[] labels = new INDArray[size];
                    for (int i = 0; i < size; i++) {
                        labels[i] = batch.get(i).value;
                    }
                    return new DataSet(Nd4j.concat(0, features), Nd4j.concat(0, labels));
                });
    }

    public static <T> StreamTransformer<T, List<T>> batch(int batchSize) {
        //noinspection unchecked,rawtypes
        return source -> StreamSupport.stream(new BatchSpliterator<>(source.spliterator(), batchSize), false)
                .onClose(source::close);
    }

    public static <T> StreamTransformer<T, List<T>> slidingWindow(int windowsize) {
        //noinspection unchecked,rawtypes
        return source -> StreamSupport.stream(new SlidingWindowSpliterator<>(source.spliterator(), windowsize), false)
                .onClose(source::close);
    }

    static class BatchSpliterator<T> extends Spliterators.AbstractSpliterator<List<T>> {

        private final Spliterator<? extends T> source;
        private final int batchSize;

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

    static class SlidingWindowSpliterator<T> extends Spliterators.AbstractSpliterator<List<T>> {

        private final Spliterator<? extends T> source;
        final int windowSize;
        final Deque<T> lastWindow = new ArrayDeque<>();

        protected SlidingWindowSpliterator(Spliterator<? extends T> source, int windowSize) {
            super(1, ORDERED | IMMUTABLE);
            this.source = source;
            this.windowSize = windowSize;
        }

        @Override
        public boolean tryAdvance(Consumer<? super List<T>> action) {
            if (source.tryAdvance(lastWindow::addLast)) {
                if (lastWindow.size() > windowSize)
                    lastWindow.removeFirst();

                action.accept(new ArrayList<>(lastWindow));
                return true;
            } else {
                while (!lastWindow.isEmpty()) {
                    lastWindow.removeFirst();
                    action.accept(new ArrayList<>(lastWindow));
                }
                return false;
            }
        }
    }
}
