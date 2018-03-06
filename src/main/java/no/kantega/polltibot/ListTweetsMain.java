package no.kantega.polltibot;

import no.kantega.polltibot.twitter.TwitterStore;
import org.kantega.kson.JsonResult;
import org.kantega.kson.json.JsonValue;
import org.kantega.kson.parser.JsonParser;

import java.time.Duration;

public class ListTweetsMain {
    public static void main(String[] args) {
        TwitterStore store = TwitterStore.getStore();

        store.list(Corpus.politi).executeAndAwait(Duration.ofSeconds(30)).orThrow().foreachDoEffect(pair->{
            JsonResult<JsonValue> json =JsonParser.parse(pair._3());
            String text = json.fieldAsString("text").orThrow();
            String date = json.fieldAsString("created_at").orThrow();
            System.out.println(date+" "+text);
        });
    }
}
