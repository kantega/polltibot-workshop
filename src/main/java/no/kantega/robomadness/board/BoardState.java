package no.kantega.robomadness.board;

import fj.Ord;
import fj.P;
import fj.P2;
import fj.data.List;
import fj.data.TreeMap;
import no.kantega.robomadness.engine.AgentResponse;
import no.kantega.robomadness.events.GameEvent;


public class BoardState {

    final List<GameEvent> updates;
    final List<GameEvent> history;
    final TreeMap<String, ShipStatus> ships;
    final TreeMap<String, POI> pois;

    public static BoardState newBoard() {
        return new BoardState(List.nil(), List.nil(), TreeMap.empty(Ord.stringOrd), TreeMap.empty(Ord.stringOrd));
    }


    public BoardState(List<GameEvent> updates, List<GameEvent> history, TreeMap<String, ShipStatus> ships, TreeMap<String, POI> pois) {
        this.updates = updates;
        this.history = history;
        this.ships = ships;
        this.pois = pois;
    }

    public BoardState handleIntents(AgentResponse actions) {
//action->event
        return this;
    }

    public BoardState applyEvents() {
//event-> addition event
        return this;
    }

    public P2<BoardState, List<GameEvent>> turn() {
        //Distribute events, reset updates and emit events
        return P.p(this, List.nil());
    }
}
