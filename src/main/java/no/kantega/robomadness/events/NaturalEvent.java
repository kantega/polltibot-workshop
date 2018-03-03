package no.kantega.robomadness.events;

import no.kantega.robomadness.board.GridLocation;
import no.kantega.robomadness.msg.EncodedMsgBuilder;
import no.kantega.robomadness.msg.Message;

import static no.kantega.robomadness.msg.EncodedMsgBuilder.*;

public abstract class NaturalEvent implements Message {


    public static class BlackHoleAppeared extends NaturalEvent {

        final GridLocation location;

        public BlackHoleAppeared(GridLocation location) {
            this.location = location;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return msg("BlackHoleAppeared",location.encode());
        }
    }

    public static class BlackHoleDisappeared extends NaturalEvent{

        @Override
        public EncodedMsgBuilder encode() {
            return msg("BlackHoleDisappeared");
        }
    }

    public static class EnergyAppeared extends NaturalEvent{
        final GridLocation location;

        public EnergyAppeared(GridLocation location) {
            this.location = location;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return msg("EnergyAppeared",location.encode());
        }
    }

    public static class EnergyDisappeared extends NaturalEvent{

        @Override
        public EncodedMsgBuilder encode() {
            return msg("EnergyDisappeared");
        }
    }

    public static class RepairstationAppeared extends NaturalEvent{
         final GridLocation location;

        public RepairstationAppeared(GridLocation location) {
            this.location = location;
        }

        @Override
        public EncodedMsgBuilder encode() {
            return msg("RepairstationAppeared",location.encode());
        }
    }

    public static class RepairstationDisappeared extends NaturalEvent{

        @Override
        public EncodedMsgBuilder encode() {
            return msg("RepairstationDisappeared");
        }
    }
}
