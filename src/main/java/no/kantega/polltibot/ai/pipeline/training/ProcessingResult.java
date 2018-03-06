package no.kantega.polltibot.ai.pipeline.training;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ProcessingResult<O> {
    public final Optional<O> value;
    public final Optional<Exception> exception;
    public final String reason;

    public ProcessingResult(Optional<O> value, Optional<Exception> exception, String reason) {
        this.value = value;
        this.exception = exception;
        this.reason = reason;
    }

    public <A> A fold(
        BiFunction<Optional<O>, String, A> onSuccess,
        BiFunction<Exception, String, A> onException
    ) {
        return
            exception.map(ex -> onException.apply(ex, reason)).orElseGet(
                () -> onSuccess.apply(value, reason)
            );
    }

    public void onValueOrThrow(BiConsumer<O, String> consumer) {
        Runnable r = fold(
            (maybeO, reason) -> () -> consumer.accept(maybeO.orElseThrow(() -> new RuntimeException("No value: " + reason)), reason),
            (ex, reason) -> () -> {
                throw new RuntimeException(reason, ex);
            }
        );
        r.run();
    }

    public  O getOrThrow(){
        return fold(
            (maybeO, reason) -> maybeO.orElseThrow(() -> new RuntimeException("No value: " + reason)),
            (ex, reason) ->  {
                throw new RuntimeException(reason, ex);
            }
        );
    }

    public <O2> ProcessingResult<O2> map(Function<O,O2> f){
        return new ProcessingResult<>(value.map(f),exception,reason);
    }

}
