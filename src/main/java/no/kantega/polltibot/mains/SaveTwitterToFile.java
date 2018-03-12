package no.kantega.polltibot.mains;

import no.kantega.polltibot.ai.pipeline.MLTask;
import no.kantega.polltibot.twitter.Corpus;
import no.kantega.polltibot.twitter.TwitterStore;
import no.kantega.polltibot.workshop.Settings;

import java.io.IOException;
import java.nio.file.Files;

public class SaveTwitterToFile {


    public static void main(String[] args) {
        TwitterStore.getStore().corpus(Corpus.polititrondelag).bind(tweets ->
                MLTask.trySupply(() -> Files.write(Settings.tweetsPath, (Iterable<String>) tweets::iterator))
        ).executeAndAwait();
    }
}
