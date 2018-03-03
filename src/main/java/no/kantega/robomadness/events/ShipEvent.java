package no.kantega.robomadness.events;

import no.kantega.robomadness.board.GridLocation;
import no.kantega.robomadness.board.Heading;
import no.kantega.robomadness.msg.EncodedMsgBuilder;
import no.kantega.robomadness.msg.Message;

import static no.kantega.robomadness.msg.EncodedMsgBuilder.*;

public abstract class ShipEvent implements Message {

    public static class ShipAppeared extends ShipEvent {

        final GridLocation location;
        final Heading heading;

        public ShipAppeared(GridLocation location, Heading heading) {
            this.location = location;
            this.heading = heading;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return msg("ShipAppeared", location.encode(), keyword(heading.name()));
        }
    }

    public static class ShipExploded extends ShipEvent {

        @Override
        public EncodedMsgBuilder encode() {
            return msg("ShipExploded");
        }
    }

    public static class ShipLeft extends ShipEvent {

        @Override
        public EncodedMsgBuilder encode() {
            return msg("ShipLeft");
        }
    }

    public static class ShipMoved extends ShipEvent {

        final GridLocation location;

        public ShipMoved(GridLocation location) {
            this.location = location;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return msg("ShipMoved", location.encode());
        }
    }

    public static class ShipTurned extends ShipEvent {

        final DirectionChange directionChange;

        public ShipTurned(DirectionChange directionChange) {
            this.directionChange = directionChange;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return msg("ShipTurned", keyword(directionChange.name()));
        }
    }
}
