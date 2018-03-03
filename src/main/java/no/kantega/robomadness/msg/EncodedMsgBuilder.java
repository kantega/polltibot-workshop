package no.kantega.robomadness.msg;

import fj.data.LazyString;
import fj.data.List;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EncodedMsgBuilder {

    final LazyString buffer;


    private EncodedMsgBuilder(LazyString buffer) {
        this.buffer = buffer;
    }

    public static EncodedMsgBuilder builder() {
        return new EncodedMsgBuilder(LazyString.empty);
    }

    private EncodedMsgBuilder append(String append) {
        return new EncodedMsgBuilder(buffer.append(append));
    }

    private EncodedMsgBuilder append(EncodedMsgBuilder append) {
        return new EncodedMsgBuilder(buffer.append(append.buffer));
    }

    private EncodedMsgBuilder append(List<EncodedMsgBuilder> append) {
        if (append.isEmpty())
            return this;
        if (append.isSingle())
            return append(append.head());
        else
            return append(append.head()).comma().append(append.tail());
    }

    public EncodedMsgBuilder lparen() {
        return append("(");
    }

    public EncodedMsgBuilder rparen() {
        return append(")");
    }

    public EncodedMsgBuilder comma() {
        return append(",");
    }

    public EncodedMsgBuilder quot() {
        return append("\"");
    }

    public String asString(){
        return buffer.eval();
    }

    public static EncodedMsgBuilder value(String value) {
        return builder().quot().append(value).quot();
    }

    public static EncodedMsgBuilder value(int value) {
        return builder().append(String.valueOf(value));
    }

    public static EncodedMsgBuilder value(double value) {
        return value(BigDecimal.valueOf(value));
    }

    public static EncodedMsgBuilder value(BigDecimal bd) {
        return builder().append(bd.setScale(10, RoundingMode.HALF_UP).toString());
    }

    public static EncodedMsgBuilder value(boolean val) {
        return val ? keyword("true") : keyword("false");
    }

    public static EncodedMsgBuilder keyword(String keyword){
        return builder().append(keyword);
    }

    public static EncodedMsgBuilder msg(
            String msgType,
            EncodedMsgBuilder... contents
    ) {
        return builder().append(msgType).lparen().append(List.arrayList(contents)).rparen();
    }

}
