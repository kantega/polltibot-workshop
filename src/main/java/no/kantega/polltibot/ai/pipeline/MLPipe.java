package no.kantega.polltibot.ai.pipeline;


public interface MLPipe<A, B> {

    MLTask<B> apply(MLTask<A> input);

    default <C> MLPipe<A, C> and(MLPipe<B, C> other) {
        return input -> other.apply(apply(input));
    }

}
