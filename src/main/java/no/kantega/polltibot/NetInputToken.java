package no.kantega.polltibot;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.function.Consumer;

public interface NetInputToken {


    default boolean isWord() {
        return this instanceof Word;
    }

    default Word asWord() {
        return (Word) this;

    }

    default void onWord(Consumer<Word> wordConsumer) {
        consume(wordConsumer, padding -> {
        });
    }


    void consume(Consumer<Word> wordConsumer, Consumer<Padding> paddingConsumer);

    static NetInputToken padding() {
        return new Padding();
    }

    static NetInputToken toToken(String word, INDArray vector) {
        return new Word(word, vector);
    }

    class Word implements NetInputToken {
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

    class Padding implements NetInputToken {


        @Override
        public void consume(Consumer<Word> wordConsumer, Consumer<Padding> paddingConsumer) {
            paddingConsumer.accept(this);
        }
    }


}
