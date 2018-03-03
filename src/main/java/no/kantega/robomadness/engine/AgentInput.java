package no.kantega.robomadness.engine;


import fj.data.List;

public class AgentInput {

    final long turn;
    final List<SensorInput> input;

    public AgentInput(long turn,  List<SensorInput> input) {
        this.turn = turn;
        this.input = input;
    }
}
