package no.kantega.robomadness.ai.pipeline.training;

import no.kantega.robomadness.ai.pipeline.RecordSource;
import org.nd4j.linalg.primitives.Pair;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class ProcessingStateMachine<O> {

    public abstract <A> A fold(
        Function<Work<O>, A> onWork,
        Function<Value<O>, A> onValue,
        Function<Done<O>, A> onDone
    );

    public static <A> ProcessingStateMachine<Stream<A>> processEpoch(RecordSource<A> source) {
        return work(() -> value(source.get()));
    }

    public static <O> ProcessingResult<O> run(ProcessingStateMachine<O> processor) {
        ProcessingStateMachine<O> current = processor;
        O                         value   = null;
        while (!(current instanceof Done)) {
            Pair<Optional<O>, ProcessingStateMachine<O>> stepResult = current.step();
            current = stepResult.getRight();
            if (stepResult.getFirst().isPresent())
                value = stepResult.getFirst().get();
        }
        Done<O> done = (Done<O>) current;
        return new ProcessingResult<>(Optional.ofNullable(value), done.result, done.reason);
    }

    private Pair<Optional<O>, ProcessingStateMachine<O>> step() {
        return fold(
            work -> Pair.of(Optional.empty(), work.execute()),
            value -> Pair.of(Optional.ofNullable(value.value), value.next),
            done -> Pair.of(Optional.empty(), done)
        );
    }

    public static <O> ProcessingStateMachine<O> value(O value) {
        return new Value<>(value, done("No more output"));
    }

    public static <O> ProcessingStateMachine<O> value(O value, ProcessingStateMachine<O> next) {
        return new Value<>(value, next);
    }

    public static <O> ProcessingStateMachine<O> done(String reason) {
        return done(reason, Optional.empty());
    }

    public static <O> ProcessingStateMachine<O> failed(String message, Exception e) {
        return done(message, Optional.ofNullable(e));
    }

    public static <O> ProcessingStateMachine<O> done(String reason, Optional<Exception> e) {
        return new Done<>(reason, e);
    }

    public static <O, O2> ProcessingStateMachine<O2> done(Done<O> done) {
        return new Done<>(done.reason, done.result);
    }

    public static <O> ProcessingStateMachine<O> work(Supplier<ProcessingStateMachine<O>> next) {
        return new Work<>(next);
    }

    public ProcessingResult<O> run() {
        return run(this);
    }


    public <B> ProcessingStateMachine<B> map(Function<O, B> f) {
        return fold(
            cont -> ProcessingStateMachine.work(() -> cont.next.get().map(f)),
            value -> value(f.apply(value.value), value.next.map(f)),
            done -> done(done.reason, done.result)
        );
    }

    public <O2> ProcessingStateMachine<O2> bind(Function<O, ProcessingStateMachine<O2>> onValue) {
        return fold(
            work -> work(() -> work.execute().bind(onValue)),
            value -> onValue.apply(value.value).fold(
                onWork -> onWork.append(() -> value.next.bind(onValue)),
                v -> value(v.value, v.next.append(() -> value.next.bind(onValue))),
                done -> done(done.reason, done.result)
            ),
            done -> done(done.reason, done.result)
        );
    }


    public ProcessingStateMachine<O> append(Supplier<ProcessingStateMachine<O>> next) {
        return fold(
            work -> work(() -> work.execute().append(next)),
            value -> value(value.value, value.next.append(next)),
            done -> next.get()
        );
    }

    public <O2> ProcessingStateMachine<O2> append(Function<O, ProcessingStateMachine<O2>> onDone) {
        return work(() ->
            run().fold(
                (maybeVal, reason) ->
                    maybeVal.map(res ->
                        onDone.apply(res).run().fold(
                            (maybeAppendedVal, appendedReason) ->
                                maybeAppendedVal
                                    .map(appendedVal -> value(appendedVal).append(() -> done(reason + " then " + appendedReason)))
                                    .orElseGet(() -> done(reason + " then " + appendedReason)),
                            (appendedEx, appendedReason) -> ProcessingStateMachine.<O2>failed(reason + " then " + appendedReason, appendedEx)
                        )).orElseGet(() -> done(reason)),
                (ex, reason) ->
                    done(reason, Optional.of(ex))
            )
        );

    }

    public <O2> ProcessingStateMachine<O2> appendValue(Function<O, O2> f) {
        return append(o -> value(f.apply(o)));
    }


    public ProcessingStateMachine<O> repeat(StopCondition<O> stopCondition) {
        return repeat().haltOn(stopCondition);
    }

    private ProcessingStateMachine<O> repeat(){
        return append((Supplier<ProcessingStateMachine<O>>) this::repeat);
    }

    public ProcessingStateMachine<O> haltOn(StopCondition<O> stopCondition) {
        return fold(
            work -> work(()->work.execute().haltOn(stopCondition)),
            value ->
                stopCondition.apply(value.value).either(
                    ProcessingStateMachine::done,
                    cont->value(value.value,work(()->value.next.haltOn(cont)))
                ),
            done -> done
        );
    }

    public static class Done<O> extends ProcessingStateMachine<O> {

        public final String reason;
        public final Optional<Exception> result;

        private Done(String reason, Optional<Exception> result) {
            this.reason = reason;
            this.result = result;
        }


        @Override
        public <A> A fold(
            Function<Work<O>, A> onContinue,
            Function<Value<O>, A> onValue,
            Function<Done<O>, A> onDone) {
            return onDone.apply(this);
        }
    }

    public static class Work<O> extends ProcessingStateMachine<O> {
        private final Supplier<ProcessingStateMachine<O>> next;


        private Work(Supplier<ProcessingStateMachine<O>> next) {
            this.next = next;
        }


        @Override
        public <A> A fold(
            Function<Work<O>, A> onContinue,
            Function<Value<O>, A> onValue,
            Function<Done<O>, A> onDone) {
            return onContinue.apply(this);
        }

        public ProcessingStateMachine<O> execute() {
            try {
                return next.get();
            } catch (Exception e) {
                return failed("Failed while performing work", e);
            }
        }
    }

    public static class Value<O> extends ProcessingStateMachine<O> {

        final O                         value;
        final ProcessingStateMachine<O> next;

        public Value(O value, ProcessingStateMachine<O> next) {
            this.value = value;
            this.next = next;
        }

        @Override
        public <A> A fold(
            Function<Work<O>, A> onContinue,
            Function<Value<O>, A> onValue,
            Function<Done<O>, A> onDone) {
            return onValue.apply(this);
        }
    }


}
