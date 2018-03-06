package no.kantega.polltibot.ai;

import no.kantega.polltibot.ai.pipeline.training.MLTask;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public class FastTextMap {

    final Map<String, INDArray> word2Vec;
    final Map<INDArray, String> vec2word;

    public FastTextMap(Map<String, INDArray> word2Vec, Map<INDArray, String> vec2word) {
        this.word2Vec = word2Vec;
        this.vec2word = vec2word;
    }

    public static MLTask<FastTextMap> load(Path path) {
        return MLTask.trySupply(() -> {
            Map<String, INDArray> dict = new TreeMap<>();
            Map<INDArray, String> revDict = new TreeMap<>();
            Files.lines(path).forEach(line -> {
                String[] parts = line.split(" ");
                String word = parts[0];
                INDArray vector = Nd4j.zeros(300);
                for (int i = 0; i < 300; i++) {
                    vector.putScalar(i, Float.valueOf(parts[i + 1]));
                }
                dict.put(word, vector);
                revDict.put(vector, word);
            });

            return new FastTextMap(dict, revDict);
        });


    }

}
