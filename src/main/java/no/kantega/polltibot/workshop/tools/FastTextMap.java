package no.kantega.polltibot.workshop.tools;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import fj.function.Try0;
import no.kantega.polltibot.ai.pipeline.MLTask;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Counter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class FastTextMap {

    public static MetricRegistry registry = new MetricRegistry();
    static Timer createtranspose = registry.timer("createtranspose");
    static Timer calcnorm = registry.timer("calcnorm");
    static Timer dotprodtimer = registry.timer("dotprod");
    static Timer sort = registry.timer("sort");

    final SortedMap<String, Integer> wordToIndex;
    final SortedMap<Integer, String> indexToWord;
    final INDArray dict;
    final INDArray dictNorm2;

    public FastTextMap(SortedMap<String, Integer> wordToIndex, SortedMap<Integer, String> indexToWord, INDArray features, INDArray normsCol) {
        this.wordToIndex = wordToIndex;
        this.indexToWord = indexToWord;
        this.dict = features;
        this.dictNorm2 = normsCol;
    }

    public static MLTask<FastTextMap> load(byte[] bytes, long cap) {
        return load(() -> Util.load(bytes).executeAndAwait(),cap);
    }

    public static MLTask<FastTextMap> load(Path path, long cap) {
        return load(() -> Files.lines(path), cap);
    }

    public static MLTask<FastTextMap> load(Try0<Stream<String>, Exception> lines, long cap) {
        return MLTask.trySupply(() -> {

            SortedMap<String, Integer> wordToIndex = new TreeMap<>();
            SortedMap<Integer, String> indexToWord = new TreeMap<>();

            String firstLine = lines.f().limit(1).reduce("", (a, b) -> a + b);
            int entries = Integer.parseInt(firstLine.split(" ")[0]);

            INDArray table = Nd4j.create(entries, 300);
            AtomicInteger counter = new AtomicInteger();
            lines.f().skip(1).limit(cap).forEach(line -> {
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

    public Optional<Token> asToken(String word) {
        return
                Optional
                        .ofNullable(wordToIndex.get(word.toLowerCase()))
                        .map(row -> Token.toToken(word, dict.getRow(row)));
    }

    Random r = new Random();

    public Token randomWord() {
        int row = r.nextInt(wordToIndex.size());
        return Token.toToken(indexToWord.get(row), dict.getRow(row));
    }

    public List<String> wordForVec(INDArray array, int topN) {
        INDArray transposed = array.transpose();
        double arrayNorm = transposed.norm2(0).getDouble(0);

        INDArray dotprod = dict.mmul(transposed);

        Counter<String> counter = new Counter<>();
        for (int row = 0; row < dotprod.rows(); row++) {
            double norm = arrayNorm * dictNorm2.getDouble(row);
            double sim = dotprod.getDouble(row) / norm;
            counter.incrementCount(indexToWord.get(row), sim);
        }
        counter.keepTopNElements(topN);
        return counter.keySetSorted();
    }

    public List<List<String>> wordsForVec(INDArray[] arrays, int top) {
        INDArray testWords = Nd4j.create(arrays.length, 300);
        for (int i = 0; i < arrays.length; i++) {
            testWords.putRow(i, arrays[i]);
        }

        INDArray testNorm2 = testWords.norm2(1);

        INDArray products = Util.time(dotprodtimer, () -> dict.mmul(testWords.transpose()));

        INDArray norms = Util.time(calcnorm, () -> dictNorm2.mmul(testNorm2.transpose()));

        INDArray distances = products.div(norms);

        List<List<String>> topWords = new ArrayList<>();

        for (int wordIndex = 0; wordIndex < arrays.length; wordIndex++) {
            Timer.Context context = sort.time();

            double bestWordDistance = -1;
            int bestWordIndex = 0;

            for(int dictIndex = 0; dictIndex< indexToWord.size();dictIndex++){
                double currentDistance = distances.getDouble(dictIndex,wordIndex);
                if(currentDistance>bestWordDistance){
                    bestWordDistance = currentDistance;
                    bestWordIndex = dictIndex;
                }
            }

            ArrayList<String> topCurrent = new ArrayList<>();

            topCurrent.add(indexToWord.get(bestWordIndex));

            /*INDArray[] sorted = Nd4j.sortWithIndices(distances.getColumn(wordIndex), 0, false);
            INDArray sort = sorted[0];
            List<String> ret = new ArrayList<>();
            if (top > sort.length())
                top = sort.length();
            //there will be a redundant word
            int end = top;
            for (int i = 0; i < end; i++) {
                String add = indexToWord.get(sort.getInt(i));

                ret.add(add);
            }


            */
            topWords.add(topCurrent);
            context.stop();
        }

        return topWords;

    }

}
