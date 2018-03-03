package no.kantega.robomadness.engine;

import fj.P;
import org.kantega.niagara.Source;

public class AgentManager {

    public final String id;
    private volatile Agent agent;

    public AgentManager(String id, Agent agent) {
        this.id = id;
        this.agent = agent;
    }

    public Source<AgentResponse> run(Source<AgentInput> agentInput) {
        return agentInput.mapWithState(agent, (curr, input) -> {
            AgentOutput output = curr.apply(input);
            return P.p(output.nextState, output.actions);
        }).map(ao -> new AgentResponse(id, ao));
    }

}
