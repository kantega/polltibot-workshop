package no.kantega.robomadness.engine;

import no.kantega.robomadness.board.GridLocation;
import no.kantega.robomadness.board.Heading;

public abstract class Entity {

    public static class Ship extends Entity {
        public final GridLocation location;
        public final Heading heading;

        public Ship(GridLocation location, Heading heading) {
            this.location = location;
            this.heading = heading;
        }
    }

    public static class Blackhole extends Entity {
        public final GridLocation location;

        public Blackhole(GridLocation location) {
            this.location = location;
        }
    }

    public static class Repairstation extends Entity {
        public final GridLocation location;

        public Repairstation(GridLocation location) {
            this.location = location;
        }
    }

}
