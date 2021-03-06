package no.kantega.polltibot.ai.pipeline;

import com.codahale.metrics.Timer;
import fj.Unit;
import fj.data.Either;
import fj.function.Try0;
import no.kantega.polltibot.ai.pipeline.training.StopCondition;
import no.kantega.polltibot.workshop.tools.Util;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.DataSet;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Stream;

public interface MLTask<A> {

    CompletionStage<A> execute();

    static <A> MLTask<Stream<A>> loadEpoch(Supplier<Stream<A>> records) {
        return supplier(records::get);
    }

    static <A, B> MLTask<B> collect(B initState, Stream<A> records, BiFunction<B, A, B> collector) {
        return MLTask.supplier(() -> {
            AtomicReference<B> state = new AtomicReference<>(initState);
            records.forEach(a -> state.updateAndGet(current -> collector.apply(current, a)));
            return state.get();
        });
    }

    static <A> MLTask<A> value(A value) {
        return () -> CompletableFuture.completedFuture(value);
    }

    static MLTask<Unit> run(Runnable r) {
        return supplier(() -> {
            r.run();
            return Unit.unit();
        });
    }

    static <A> MLTask<A> fail(Exception e) {
        return () -> {
            CompletableFuture<A> fut = new CompletableFuture<>();
            fut.completeExceptionally(e);
            return fut;
        };
    }

    static <A> MLTask<A> supplier(Supplier<A> supplier) {
        return () -> CompletableFuture.completedFuture(supplier.get());
    }

    static <A> MLTask<A> trySupply(Try0<A, Exception> block) {
        return supplier(() -> {
            try {
                return block.f();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static MLTask<MultiLayerNetwork> fit(MultiLayerNetwork net, Stream<DataSet> records) {

        return supplier(() -> {
            records.forEach(net::fit);
            return net;
        });
    }

    default MLTask<A> print(String msg){
        return bind(b-> Util.println(msg).thenJust(b));
    }

    default MLTask<A> time(String name, PrintStream out) {
        return time(
                start -> out.println("Start " + name + " at " + start),
                (start, maybeEx) ->
                        out.println("End   " + name + " " + Duration.between(start, Instant.now()) + maybeEx.map(ex -> " with " + ex.getClass().getSimpleName() + ":" + ex.getMessage()).orElse(""))
        );
    }

    default MLTask<A> time(Timer metricsTimer) {
        return () -> {
            Timer.Context time = metricsTimer.time();
            return execute().whenComplete((aOrNull, exorNull) -> {
                time.stop();
            });
        };
    }

    default MLTask<A> time(Consumer<Instant> onStart, BiConsumer<Instant, Optional<Throwable>> onEnd) {
        return () -> {
            Instant instant = Instant.now();
            onStart.accept(instant);
            return execute().whenComplete((aOrNull, exorNull) -> {
                onEnd.accept(instant, Optional.ofNullable(exorNull));
            });
        };
    }

    default <B> MLTask<B> pipe(MLPipe<A, B> pipe) {
        return pipe.apply(this);
    }

    default MLTask<A> async(Executor executor) {
        return () -> {
            CompletableFuture<A> fut = new CompletableFuture<>();
            executor.execute(() -> execute()
                    .whenComplete((aORNull, exOrNull) -> {
                        if (aORNull != null) fut.complete(aORNull);
                        else fut.completeExceptionally(exOrNull);
                    }));
            return fut;
        };
    }

    default <B> MLTask<B> map(Function<A, B> f) {
        return () -> execute().thenApply(f);
    }

    default <B> MLTask<B> bind(Function<A, MLTask<B>> f) {
        return () -> execute().thenCompose(a -> f.apply(a).execute());
    }


    default <B> MLTask<B> handle(Function<Throwable,MLTask<B>> onException,Function<A,MLTask<B>> onSuccess){
        return ()-> execute().handle(
                (aOrNull, exOrNull)->
                        Optional.ofNullable(aOrNull)
                                .map(onSuccess)
                                .orElseGet(()->onException.apply(exOrNull))).thenCompose(MLTask::execute);
    }

    default <B> MLTask<B> andThen(Function<A, MLTask<B>> f) {
        return bind(f);
    }

    default <B> MLTask<B> andThen(MLTask<B> next) {
        return bind(u -> next);
    }

    default <B> MLTask<B> thenJust(B value) {
        return bind(u -> MLTask.value(value));
    }

    default <S> MLTask<S> apply(S initState, BiFunction<S, A, MLTask<S>> update) {
        return bind(a -> update.apply(initState, a));
    }

    default MLTask<A> onExecution(Consumer<A> effect) {
        return () -> execute().thenApply(a -> {
            effect.accept(a);
            return a;
        });
    }

    default MLTask<A> repeat(Supplier<StopCondition<A>> cond) {
        return bind(a -> {
            A last = a;
            Either<String, StopCondition<A>> res = cond.get().apply(a);
            while (res.isRight()) {
                last = executeAndAwait();
                res = res.right().value().apply(a);
            }
            return value(last);
        });
    }

    default A executeAndAwait() {
        try {
            return execute().toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default void executeAndAwait(Consumer<A> onSuccess,Consumer<Exception> onFail) {
        try {
            onSuccess.accept(execute().toCompletableFuture().get());
        } catch (Exception e) {
            onFail.accept(e);
        }
    }
}
