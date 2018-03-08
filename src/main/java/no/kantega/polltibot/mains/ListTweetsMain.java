package no.kantega.polltibot.mains;

import no.kantega.polltibot.Corpus;
import no.kantega.polltibot.twitter.TwitterStore;
import org.kantega.kson.JsonResult;
import org.kantega.kson.json.JsonValue;
import org.kantega.kson.parser.JsonParser;

import java.time.Duration;

public class ListTweetsMain {
    public static void main(String[] args) {
        TwitterStore store = TwitterStore.getStore();

        store.tweets(Corpus.politi).executeAndAwait().foreachDoEffect(pair->{
            JsonResult<JsonValue> json =JsonParser.parse(pair._3());
            String text = json.fieldAsString("text").orThrow();
            String date = json.fieldAsString("created_at").orThrow();
            System.out.println(date+" "+text);
        });
    }
}
