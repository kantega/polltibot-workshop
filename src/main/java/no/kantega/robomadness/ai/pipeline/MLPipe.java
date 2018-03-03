package no.kantega.robomadness.ai.pipeline;


import no.kantega.robomadness.ai.pipeline.training.MLTask;

public interface MLPipe<A,B> {

    MLTask<B> apply(MLTask<A> input);

}
