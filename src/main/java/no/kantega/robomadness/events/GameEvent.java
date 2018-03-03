package no.kantega.robomadness.events;

import no.kantega.robomadness.board.AgentId;
import no.kantega.robomadness.msg.EncodedMsgBuilder;
import no.kantega.robomadness.msg.Message;

import static no.kantega.robomadness.msg.EncodedMsgBuilder.msg;
import static no.kantega.robomadness.msg.EncodedMsgBuilder.value;

public abstract class GameEvent implements Message {


    public static class Turn extends GameEvent {
        @Override
        public EncodedMsgBuilder encode() {
            return EncodedMsgBuilder.msg("Turn");
        }
    }

    public static class ShipActivity extends GameEvent {

        final AgentId agent;
        final ShipEvent activity;

        public ShipActivity(AgentId agent, ShipEvent activity) {
            this.agent = agent;
            this.activity = activity;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return
                    msg(
                            "ShipActivity",
                            msg("Agent", value(agent.id), value(agent.name)),
                            activity.encode()
                    );
        }
    }

    public static class NaturalActivity extends GameEvent {

        final String id;
        final NaturalEvent naturalEvent;

        public NaturalActivity(String id, NaturalEvent naturalEvent) {
            this.id = id;
            this.naturalEvent = naturalEvent;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return msg("NaturalActivity", value(id), naturalEvent.encode());
        }
    }
}
