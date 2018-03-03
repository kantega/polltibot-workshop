package no.kantega.robomadness.engine;

import fj.data.List;

public class AgentOutput {

    final List<AgentAction> actions;
    final Agent nextState;

    public AgentOutput(List<AgentAction> actions, Agent nextState) {
        this.actions = actions;
        this.nextState = nextState;
    }
}
