package no.kantega.polltibot.mains;

import no.kantega.polltibot.Corpus;
import no.kantega.polltibot.Util;
import no.kantega.polltibot.twitter.TwitterClient;
import no.kantega.polltibot.twitter.TwitterStore;
import org.kantega.kson.parser.JsonWriter;

import java.time.Duration;

public class TwitterClientMain {

    public static void main(String[] args) {

        TwitterStore store = TwitterStore.getStore();


        TwitterClient
                .tweets(args[0], args[1], args[2], args[3])
                .apply(jsonObject ->
                        Util.println(JsonWriter.write(jsonObject))
                                .thenJust(jsonObject))
                .apply(jsonObject -> store.add(jsonObject.fieldAsLong("id").orThrow(), Corpus.politi, jsonObject.fieldAsText("text").orThrow(), JsonWriter.write(jsonObject)))
                .toTask().executeAndAwait(Duration.ofSeconds(1000)).orThrow();
    }
}
