package no.kantega.robomadness.mains;

import no.kantega.robomadness.Client;
import no.kantega.robomadness.board.*;
import no.kantega.robomadness.events.DirectionChange;
import no.kantega.robomadness.events.GameEvent;
import no.kantega.robomadness.events.NaturalEvent;
import org.kantega.niagara.Source;
import org.kantega.niagara.Sources;

import java.net.URISyntaxException;

import static no.kantega.robomadness.events.GameEvent.*;
import static no.kantega.robomadness.events.ShipEvent.*;

public class TestProgress {

    public static void main(String[] args) throws URISyntaxException {

        AgentId a = new AgentId("a", "atle");
        AgentId b = new AgentId("b", "batle");

        Source<GameEvent> events =
                Sources.tryCallback(cb -> {
                    cb.f(() -> new ShipActivity(a, new ShipAppeared(new GridLocation(0, 0), Heading.south)));
                    cb.f(() -> new Turn());
                    Thread.sleep(1000);
                    cb.f(()->new ShipActivity(a, new ShipTurned(DirectionChange.left)));
                    cb.f(()->new Turn());
                    Thread.sleep(1000);
                    cb.f(()->new ShipActivity(a, new ShipMoved(new GridLocation(1, 0))));
                    cb.f(()->new NaturalActivity("b1", new NaturalEvent.BlackHoleAppeared(new GridLocation(4, 4))));
                    cb.f(()->new Turn());
                    Thread.sleep(1000);
                    cb.f(() -> new ShipActivity(b, new ShipAppeared(new GridLocation(-10, 0), Heading.north)));
                    cb.f(() -> new Turn());
                    Thread.sleep(1000);
                    cb.f(()->new ShipActivity(a, new ShipMoved(new GridLocation(2, 0))));
                    cb.f(() -> new ShipActivity(b, new ShipMoved(new GridLocation(-10, 1))));
                    cb.f(()->new Turn());
                });
        Client.run(
                Client.websocket("localhost", 8080),
                "jalla",
                events.map(msg -> msg.encode().asString())
        );


    }
}
