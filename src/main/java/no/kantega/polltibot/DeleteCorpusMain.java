package no.kantega.polltibot;

import no.kantega.polltibot.twitter.TwitterStore;

public class DeleteCorpusMain {

    public static void main(String[] args) {
        TwitterStore.getStore().deleteCorpus(Corpus.politi).execute();
    }
}
