package no.kantega.polltibot;

import fj.Ord;
import fj.P;
import no.kantega.polltibot.ai.pipeline.MLTask;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Counter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FastTextMap {

    final SortedMap<String, Integer> wordToIndex;
    final SortedMap<Integer, String> indexToWord;
    final INDArray table;
    final INDArray normsCol;

    public FastTextMap(SortedMap<String, Integer> wordToIndex, SortedMap<Integer, String> indexToWord, INDArray features, INDArray normsCol) {
        this.wordToIndex = wordToIndex;
        this.indexToWord = indexToWord;
        this.table = features;
        this.normsCol = normsCol;
    }

    public static MLTask<FastTextMap> load(Path path) {
        return MLTask.trySupply(() -> {

            SortedMap<String, Integer> wordToIndex = new TreeMap<>();
            SortedMap<Integer, String> indexToWord = new TreeMap<>();

            String firstLine = Files.lines(path).limit(1).reduce("", (a, b) -> a + b);
            int entries = Integer.parseInt(firstLine.split(" ")[0]);

            INDArray table = Nd4j.create(entries, 300);
            AtomicInteger counter = new AtomicInteger();
            Files.lines(path).skip(1).forEach(line -> {
                int row = counter.getAndIncrement();
                String[] parts = line.split(" ");
                String word = parts[0];
                wordToIndex.put(word.toLowerCase(), row);
                indexToWord.put(row, word);
                for (int i = 0; i < 300; i++) {
                    table.putScalar(row, i, Float.valueOf(parts[i + 1]));
                }

            });

            INDArray norms = table.norm2(1);

            return new FastTextMap(wordToIndex, indexToWord, table, norms);
        });
    }

    public Optional<NetInputToken> asToken(String word) {
        return
                Optional
                        .ofNullable(wordToIndex.get(word.toLowerCase()))
                        .map(row -> NetInputToken.toToken(word, table.getRow(row)));
    }
    Random r = new Random();
    public NetInputToken randomWord(){
        int row = r.nextInt(wordToIndex.size());
        return NetInputToken.toToken(indexToWord.get(row),table.getRow(row));
    }

    public List<String> wordForVec(INDArray array, int topN) {
        INDArray transposed = array.transpose();
        double arrayNorm = transposed.norm2(0).getDouble(0);

        INDArray dotprod = table.mmul(transposed);

        Counter<String> counter = new Counter<>();
        for (int row = 0; row < dotprod.rows(); row++) {
            double norm = arrayNorm * normsCol.getDouble(row);
            double sim = dotprod.getDouble(row) / norm;
            counter.incrementCount(indexToWord.get(row), sim);
        }
        counter.keepTopNElements(topN);
        return counter.keySetSorted();
    }




}
