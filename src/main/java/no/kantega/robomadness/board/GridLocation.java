package no.kantega.robomadness.board;

import no.kantega.robomadness.msg.EncodedMsgBuilder;
import no.kantega.robomadness.msg.Message;

import static no.kantega.robomadness.msg.EncodedMsgBuilder.*;

public class GridLocation implements Message {


    final int x;
    final int y;

    public GridLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public EncodedMsgBuilder encode() {
        return msg("GridLocation", value(x), value(y));
    }
}
