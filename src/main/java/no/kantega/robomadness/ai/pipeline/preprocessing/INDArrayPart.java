package no.kantega.robomadness.ai.pipeline.preprocessing;

public interface INDArrayPart {

    void write(int index, double value);

    default INDArrayPart offset(int offset){
        return (index,value)->write(index+offset,value);
    }

}
