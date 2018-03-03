package no.kantega.robomadness.engine;

import fj.data.List;

public class AgentResponse {

    public final String agentId;
    public final List<AgentAction> actionList;

    public AgentResponse(String agentId, List<AgentAction> actionList) {
        this.agentId = agentId;
        this.actionList = actionList;
    }
}
