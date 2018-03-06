package no.kantega.polltibot.ai.pipeline;


import no.kantega.polltibot.ai.pipeline.training.MLTask;

public interface MLPipe<A,B> {

    MLTask<B> apply(MLTask<A> input);

}
