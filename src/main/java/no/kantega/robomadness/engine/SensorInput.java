package no.kantega.robomadness.engine;

import no.kantega.robomadness.board.GridLocation;
import no.kantega.robomadness.board.Heading;

public abstract class SensorInput {


    public static class Positioning extends SensorInput {
        public final GridLocation location;

        public Positioning(GridLocation location) {
            this.location = location;
        }
    }

    public static class Compass extends SensorInput {
        public final Heading heading;

        public Compass(Heading heading) {
            this.heading = heading;
        }
    }

    public static class Presence extends SensorInput {
        public final Entity entity;

        public Presence(Entity entity) {
            this.entity = entity;
        }
    }

    public static class EnergyMeter extends SensorInput{
        public final int level;

        public EnergyMeter(int level) {
            this.level = level;
        }
    }
}
