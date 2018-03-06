package no.kantega.polltibot.ai.pipeline;

public class UnlabeledRecord<A> {

    public final A value;

    public UnlabeledRecord(A value) {
        this.value = value;
    }
}
