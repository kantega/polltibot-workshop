package no.kantega.robomadness.engine;

public class AgentInputMessage {

    public final String agentId;
    public final AgentInput agentInput;

    public AgentInputMessage(String agentId, AgentInput agentInput) {
        this.agentId = agentId;
        this.agentInput = agentInput;
    }
}
