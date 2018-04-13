package no.kantega.polltibot.workshop.tools;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.function.Consumer;

public interface Token {


    default boolean isWord() {
        return this instanceof Word;
    }

    default Word asWord() {
        return (Word) this;
    }

    default Token wordOr(Token defaultWord) {
        return isWord() ? asWord() : defaultWord;
    }

    default void onWord(Consumer<Word> wordConsumer) {
        consume(wordConsumer, padding -> {
        });
    }

    INDArray toInput();


    void consume(Consumer<Word> wordConsumer, Consumer<Padding> paddingConsumer);

    static Token padding() {
        return new Padding();
    }

    static Token toToken(String word, INDArray vector) {
        return new Word(word, vector);
    }

    class Word implements Token {
        public final String word;
        public final INDArray vector;

        public Word(String word, INDArray vector) {
            this.word = word;
            this.vector = vector;
        }

        public INDArray toInput() {
            return vector;
        }

        @Override
        public void consume(Consumer<Word> wordConsumer, Consumer<Padding> paddingConsumer) {
            wordConsumer.accept(this);
        }
    }

    class Padding implements Token {


        @Override
        public void consume(Consumer<Word> wordConsumer, Consumer<Padding> paddingConsumer) {
            paddingConsumer.accept(this);
        }

        public INDArray toInput() {
            return Nd4j.zeros(1,300);
        }
    }


}
