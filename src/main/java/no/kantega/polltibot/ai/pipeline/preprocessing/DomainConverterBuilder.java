package no.kantega.polltibot.ai.pipeline.preprocessing;

import fj.Ord;
import fj.data.IterableW;
import fj.data.List;
import fj.data.Set;
import no.kantega.polltibot.ai.pipeline.training.MLTask;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class DomainConverterBuilder<A> {

    public static final DomainConverterBuilder<String> category =
        new DomainConverterBuilder.CategoryConverterBuilder(Set.empty(Ord.stringOrd));

    public static final DomainConverterBuilder<Iterable<String>> categories =
        new DomainConverterBuilder.CategoriesConverterBuilder(Set.empty(Ord.stringOrd));

    public static final DomainConverterBuilder<Instant> timeOfday =
        new DomainConverterBuilder.TimeOfDayConverterBuilder(ZoneOffset.UTC);

    public static final DomainConverterBuilder<Instant> timeOfweek =
        new DomainConverterBuilder.TimeInWeekConverterBuilder(ZoneOffset.UTC);

    public MLTask<Converter<A>> fit(Stream<A> data) {
        return MLTask.supplier(() -> {
            AtomicReference<DomainConverterBuilder<A>> state = new AtomicReference<>(this);
            data.forEach(a -> state.updateAndGet(current -> current.with(a)));
            return state.get().build();
        });
    }

    protected abstract DomainConverterBuilder<A> with(A value);

    protected abstract Converter<A> build();

    public <B> DomainConverterBuilder<B> contramap(Function<B, A> f) {
        return new DomainConverterBuilder.ContramapConverterBuilder<>(f, this);
    }

    public DomainConverterBuilder<A> then(DomainConverterBuilder<A> next) {
        return new ChainConverterBuilder<>(this, next);
    }

    static class ContramapConverterBuilder<B, A> extends DomainConverterBuilder<B> {

        public final Function<B, A> contramapFunction;
        public final DomainConverterBuilder<A> wrappedConverterBuilder;

        public ContramapConverterBuilder(
            Function<B, A> contramapFunction,
            DomainConverterBuilder<A> wrappedConverterBuilder) {
            this.contramapFunction = contramapFunction;
            this.wrappedConverterBuilder = wrappedConverterBuilder;
        }

        @Override
        public DomainConverterBuilder<B> with(B value) {
            return wrappedConverterBuilder.with(contramapFunction.apply(value)).contramap(contramapFunction);
        }

        @Override
        public Converter<B> build() {
            Converter<A> wrappedConverter = wrappedConverterBuilder.build();
            return Converter.converter(
                wrappedConverter.headers,
                (a, arr) -> wrappedConverter.writer.accept(contramapFunction.apply(a), arr)
            );
        }
    }

    static class CategoryConverterBuilder extends DomainConverterBuilder<String> {

        final Set<String> categories;

        public CategoryConverterBuilder(Set<String> categories) {
            this.categories = categories;
        }

        @Override
        public DomainConverterBuilder<String> with(String value) {
            return new CategoryConverterBuilder(categories.insert(value));
        }

        @Override
        public Converter<String> build() {
            return Converter.converter(
                categories.toList().snoc("NA"),
                (a, arr) -> {
                    if (categories.member(a))
                        arr.write(categories.split(a)._1().size(), 1.0);
                    else
                        arr.write(categories.size(), 1.0);
                }
            );
        }
    }

    static class CategoriesConverterBuilder extends DomainConverterBuilder<Iterable<String>> {

        final Set<String> categories;

        public CategoriesConverterBuilder(Set<String> categories) {
            this.categories = categories;
        }

        @Override
        public DomainConverterBuilder<Iterable<String>> with(Iterable<String> values) {
            return new CategoriesConverterBuilder(IterableW.wrap(values).foldLeft(c -> c::insert, categories));
        }

        @Override
        public Converter<Iterable<String>> build() {
            return Converter.converter(
                categories.toList().snoc("NA"),
                (a, arr) -> a.forEach(str -> {
                    if (categories.member(str))
                        arr.write(categories.split(str)._1().size(), 1.0);
                    else
                        arr.write(categories.size(), 1.0);
                })
            );
        }
    }

    static class ChainConverterBuilder<A> extends DomainConverterBuilder<A> {

        final DomainConverterBuilder<A> firstBuilder;
        final DomainConverterBuilder<A> secondBuilder;

        public ChainConverterBuilder(
            DomainConverterBuilder<A> firstBuilder,
            DomainConverterBuilder<A> secondBuilder) {
            this.firstBuilder = firstBuilder;
            this.secondBuilder = secondBuilder;
        }

        @Override
        public DomainConverterBuilder<A> with(A value) {
            return new ChainConverterBuilder<>(firstBuilder.with(value), secondBuilder.with(value));
        }

        @Override
        public Converter<A> build() {
            Converter<A> first  = firstBuilder.build();
            Converter<A> second = secondBuilder.build();
            return Converter.converter(
                first.headers.append(second.headers),
                (a, arr) -> {
                    first.writer.accept(a, arr);
                    second.writer.accept(a, arr.offset(first.size));
                }
            );
        }
    }

    static class TimeOfDayConverterBuilder extends DomainConverterBuilder<Instant> {

        public static final int minutesInHour = 24 * 60;
        public final ZoneOffset zone;

        public TimeOfDayConverterBuilder(ZoneOffset zone) {
            this.zone = zone;
        }

        @Override
        public DomainConverterBuilder<Instant> with(Instant value) {
            return this;
        }

        @Override
        public Converter<Instant> build() {
            return Converter.converter(
                List.arrayList("cos(2π*minuteOfDay)", "sin(2π*minuteOfDay)"),
                (timestamp, arr) -> {
                    ZonedDateTime dateTime     = timestamp.atZone(zone);
                    double        minuteInHour = dateTime.getHour() * 60 + dateTime.getMinute();
                    double        c            = Math.cos(2 * Math.PI * (minuteInHour / minutesInHour));
                    double        s            = Math.sin(2 * Math.PI * (minuteInHour / minutesInHour));

                    double cconverted = (c + 1) / 2;
                    double sconverted = (s + 1) / 2;
                    arr.write(0, cconverted);
                    arr.write(1, sconverted);
                }
            );
        }
    }

    static class TimeInWeekConverterBuilder extends DomainConverterBuilder<Instant> {

        public static final int hoursInWeek = 7 * 24;
        public final ZoneOffset zone;

        public TimeInWeekConverterBuilder(ZoneOffset zone) {
            this.zone = zone;
        }

        @Override
        public DomainConverterBuilder<Instant> with(Instant value) {
            return this;
        }

        @Override
        public Converter<Instant> build() {
            return Converter.converter(
                List.arrayList("cos(2π*minuteOfDay)", "sin(2π*minuteOfDay)"),
                (timestamp, arr) -> {
                    ZonedDateTime dateTime   = timestamp.atZone(zone);
                    double        hourInWeek = (dateTime.getDayOfWeek().getValue() - 1) * 24 + dateTime.getHour();
                    double        c          = Math.cos(2 * Math.PI * (hourInWeek / hoursInWeek));
                    double        s          = Math.sin(2 * Math.PI * (hourInWeek / hoursInWeek));

                    double cconverted = (c + 1) / 2;
                    double sconverted = (s + 1) / 2;
                    arr.write(0, cconverted);
                    arr.write(1, sconverted);
                }
            );
        }
    }
}
