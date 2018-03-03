package no.kantega.robomadness.board;

import no.kantega.robomadness.board.GridLocation;
import no.kantega.robomadness.board.Heading;

public class ShipStatus {

    public final int energy;
    public final String id;
    public final GridLocation location;
    public final Heading heading;

    public ShipStatus(int energy, String id, GridLocation location, Heading heading) {
        this.energy = energy;
        this.id = id;
        this.location = location;
        this.heading = heading;
    }
}
