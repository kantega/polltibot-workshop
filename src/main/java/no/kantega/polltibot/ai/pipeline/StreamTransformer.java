package no.kantega.polltibot.ai.pipeline;

import java.util.function.Function;
import java.util.stream.Stream;

public interface StreamTransformer<A, B> extends Function<Stream<A>, Stream<B>> {

    Stream<B> transform(Stream<A> input);

    default Stream<B> apply(Stream<A> input) {
        return transform(input);
    }

}
