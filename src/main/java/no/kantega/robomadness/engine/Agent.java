package no.kantega.robomadness.engine;

import org.kantega.niagara.Mealy;

public interface Agent{

    AgentOutput apply(AgentInput input);

}
