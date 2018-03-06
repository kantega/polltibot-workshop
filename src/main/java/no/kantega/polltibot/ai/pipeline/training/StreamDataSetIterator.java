package no.kantega.polltibot.ai.pipeline.training;


import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A DataSetIterator that works on an Iterator<DataSet>, combining and splitting the input DataSet objects as
 * required to get a consistent batch size.
 * <p>
 * Typically used in Spark training, but may be used elsewhere.
 * NOTE: reset method is not supported here.
 */
public class StreamDataSetIterator implements DataSetIterator {

    private Iterator<DataSet> iterator;

    private DataSetPreProcessor       preProcessor;


    public StreamDataSetIterator(Stream<DataSet> source) {
        this.iterator = source.iterator();
    }

    @Override
    public boolean hasNext() {
        return  iterator.hasNext();
    }

    @Override
    public DataSet next() {
        return iterator.next();
    }

    @Override
    public DataSet next(int batchSize) {
        return next();
    }

    @Override
    public int totalExamples() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int inputColumns() {
        return -1;
    }

    @Override
    public int totalOutcomes() {
        return -1;
    }

    @Override
    public boolean resetSupported() {
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return true;
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Reset not supported");
    }

    @Override
    public int batch() {
        return 0;
    }

    @Override
    public int cursor() {
        return -1;
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return null;
    }

    @Override
    public List<String> getLabels() {
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }

}

