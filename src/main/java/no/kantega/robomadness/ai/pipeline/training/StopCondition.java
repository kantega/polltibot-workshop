package no.kantega.robomadness.ai.pipeline.training;

import fj.data.Either;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;

import static fj.data.Either.left;
import static fj.data.Either.right;

public interface StopCondition<O> extends Function<O, Either<String, StopCondition<O>>> {


    default StopCondition<O> or(StopCondition<O> other) {
        return StopCondition.or(this, other);
    }

    static <O> StopCondition<O> condition(String name, Predicate<O> pred) {
        return value ->
            pred.test(value)
            ? left(name)
            : right(condition(name, pred));
    }

    static <O> StopCondition<O> checkEvery(int delay, int interval, StopCondition<O> sc) {
        return value ->
            delay >= interval
            ? sc.apply(value).right().map(next -> checkEvery(delay + 1, interval, sc))
            : right(checkEvery(delay + 1, interval, sc));
    }


    static <O> StopCondition<O> or(StopCondition<O> one, StopCondition<O> other) {
        return value -> {
            Either<String, StopCondition<O>> oneResult   = one.apply(value);
            Either<String, StopCondition<O>> otherResult = other.apply(value);

            return oneResult.either(
                oneStop ->
                    otherResult.either(
                        otherStop -> left(oneStop + " and " + otherStop),
                        otherCont -> left(oneStop)),
                oneCont ->
                    otherResult.either(
                        otherStop -> left(otherStop),
                        otherCont -> right(or(oneCont, otherCont))
                    )
            );
        };
    }


    static <O> StopCondition<O> times(int max) {
        return value ->
            max <= 0
            ? left("Max iterations reached")
            : right(times(max - 1));
    }


    static <O> StopCondition<O> maxElapedTime(Duration maxDuration) {
        return (o) -> right(until(Instant.now().plus(maxDuration)));
    }

    static <O> StopCondition<O> until(Instant endTime) {
        return condition("End time " + endTime + " reached", val -> Instant.now().isAfter(endTime));
    }

}
