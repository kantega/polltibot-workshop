package no.kantega.polltibot.workshop.tools;

import com.codahale.metrics.Timer;
import fj.F;
import fj.P;
import fj.P2;
import fj.Unit;
import fj.data.List;
import fj.data.Option;
import no.kantega.polltibot.ai.pipeline.MLPipe;
import no.kantega.polltibot.ai.pipeline.MLTask;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class Util {


    public static <A> A time(Timer timer, Callable<A> a) {
        try {
            return timer.time(a);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static MLTask<Stream<String>> load(byte[] resource) {
        return load(new ByteArrayInputStream(resource));
    }

    public static MLTask<Stream<String>> load(String resource) {
        return MLTask.trySupply(() ->
                Util.class.getClassLoader().getResourceAsStream(resource))
                .bind(Util::load);
    }


    public static MLTask<Stream<String>> load(InputStream in) {
        return MLTask.trySupply(() -> new BufferedReader(new InputStreamReader(in,
                StandardCharsets.UTF_8)).lines());
    }

    public static MLTask<Stream<String>> loadTweets() {
        return load("tweets.txt");
    }

    public static <T> T notImplemented(String text) {
        throw new RuntimeException(text);
    }

    public static MLTask<Unit> println(String line) {
        return MLTask.run(() -> System.out.println(line));
    }

    public static MLTask<String> printlnThrough(String line) {
        return println(line).map(u -> line);
    }

    public static <B, C> Matcher<C> caseOf(Class<B> tpe, F<B, C> handler) {
        return new Matcher<>(List.single(v -> tpe.isInstance(v) ? Option.some(handler.f(tpe.cast(v))) : Option.none()));
    }

    public static Option<P2<String, String>> splitBefore(String string, String split) {
        if (string.contains(split)) {
            String first = string.substring(0, string.indexOf(split));
            String last = string.substring(string.indexOf(split));
            return Option.some(P.p(first, last));
        } else {
            return Option.none();
        }
    }

    public static Option<String> substringUntil(String origin, String until) {
        return indexOf(origin, until).map(index -> origin.substring(0, index));
    }

    public static Option<Integer> indexOf(String str, String test) {
        return
                Option
                        .some(str.indexOf(test))
                        .bind(index -> index == -1 ? Option.none() : Option.some(index));
    }

    public static <A> A notImplemented() {
        throw new RuntimeException("This feature is not implemented");
    }

    public static class Matcher<C> {

        public final List<F<Object, Option<C>>> casters;

        public Matcher(List<F<Object, Option<C>>> casters) {
            this.casters = casters;
        }

        public <B> Matcher<C> caseOf(Class<B> tpe, F<B, C> handler) {
            return new Matcher<>(casters.cons(v -> tpe.isInstance(v) ? Option.some(handler.f(tpe.cast(v))) : Option.none()));
        }

        public Option<C> get(Object value) {
            return casters.reverse().foldLeft((sum, elem) -> sum.orElse(elem.f(value)), Option.none());
        }

    }
}
