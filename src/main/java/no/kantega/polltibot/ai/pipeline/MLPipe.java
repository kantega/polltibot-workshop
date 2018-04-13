package no.kantega.polltibot.ai.pipeline;


import no.kantega.polltibot.ai.pipeline.training.StopCondition;
import no.kantega.polltibot.workshop.tools.Util;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface MLPipe<A, B> {

    /**
     * Applies this pipe to the task, yielding a new task.
     *
     * @param input
     * @return
     */
    MLTask<B> apply(MLTask<A> input);

    /**
     * Ignores the previous output and yields a task that fails when executed.
     *
     * @param e
     * @return
     */
    static <A, B> MLPipe<A, B> fail(Exception e) {
        return input -> input.bind(a -> MLTask.fail(e));
    }

    /**
     * Ignores the previous output and yields a task that fails when executed.
     *
     * @param e
     * @return
     */
    static <A, B> MLPipe<A, B> fail(String e) {
        return input ->
                input.handle(
                        t -> MLTask.<B>fail(new RuntimeException(t.getCause().getMessage() + "\n" + e)),
                        a -> MLTask.<B>fail(new RuntimeException(e))
                );
    }

    static <A, B> MLPipe<A, B> pipe(Function<A, B> f) {
        return input -> input.map(f);
    }

    /**
     * Fuses two pipes
     *
     * @param other
     * @param <C>
     * @return
     */
    default <C> MLPipe<A, C> then(MLPipe<B, C> other) {
        return input -> other.apply(apply(input));
    }


    default MLPipe<A, B> print(Supplier<String> msg) {
        return input -> apply(input).print(msg.get());
    }


    /**
     * Applies the output from this pipe to the function, and runs the resulting task.
     *
     * @param f
     * @param <C>
     * @return
     */
    default <C> MLPipe<A, C> append(Function<B, MLTask<C>> f) {
        return input -> apply(input).bind(f);
    }

    /**
     * Transforms the output of the task using the function. Synonym to map
     *
     * @param f
     * @param <C>
     * @return
     */
    default <C> MLPipe<A, C> transform(Function<B, C> f) {
        return map(f);
    }

    /**
     * Transforms the output of the task using the function.
     *
     * @param f
     * @param <C>
     * @return
     */
    default <C> MLPipe<A, C> map(Function<B, C> f) {
        return input -> apply(input).map(f);
    }

    /**
     * Applies the update function to the task and som initial state. If the task is reapeated, the state
     * is kept between executions.
     *
     * @param initState
     * @param update
     * @param <S>
     * @return
     */
    default <S> MLPipe<A, S> apply(S initState, BiFunction<S, B, MLTask<S>> update) {
        return input -> apply(input).apply(initState, update);
    }

    /**
     * Runs the task repeatedly until the stopcondition is met, then yields the last result.
     *
     * @param stopConditionSupplier
     * @return
     */
    default MLPipe<A, B> repeat(Supplier<StopCondition<B>> stopConditionSupplier) {
        return input -> apply(input).repeat(stopConditionSupplier);
    }


}
