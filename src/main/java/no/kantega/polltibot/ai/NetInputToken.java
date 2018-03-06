package no.kantega.polltibot.ai;

import jdk.nashorn.internal.runtime.arrays.ArrayIndex;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.Map;

public interface NetInputToken {

    INDArray toInput();

    static NetInputToken endOfTweet(){
        return new EndOfTweet();
    }

    static NetInputToken toToken(String word, Map<String,INDArray> fastText){
        if(fastText.containsKey(word))
            return new Word(word,fastText.get(word));
        return new UnknownWord();
    }

    class Word implements NetInputToken{
        public final String word;
        public final INDArray vector;

        public Word(String word, INDArray vector) {
            this.word = word;
            this.vector = vector;
        }

        @Override
        public INDArray toInput() {
            return Nd4j.zeros(302).get(NDArrayIndex.interval(0,300)).assign(vector);
        }
    }

    class EndOfTweet implements  NetInputToken{

        @Override
        public INDArray toInput() {
            return Nd4j.zeros(302).putScalar(301,1);
        }
    }

    class UnknownWord implements  NetInputToken{

        @Override
        public INDArray toInput() {
            return Nd4j.zeros(302).putScalar(302,2);
        }
    }
}
