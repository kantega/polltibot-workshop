package no.kantega.polltibot.mains;

import no.kantega.polltibot.twitter.Corpus;
import no.kantega.polltibot.twitter.TwitterStore;

public class DeleteCorpusMain {

    public static void main(String[] args) {
        TwitterStore.getStore().deleteCorpus(Corpus.politi).execute();
    }
}
