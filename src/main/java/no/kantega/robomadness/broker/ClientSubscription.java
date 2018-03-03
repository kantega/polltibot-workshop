package no.kantega.robomadness.broker;

import fj.F;
import fj.Unit;
import fj.data.List;
import io.vertx.core.http.ServerWebSocket;
import no.kantega.robomadness.Client;
import org.kantega.kson.parser.JsonWriter;
import org.kantega.niagara.Sink;
import org.kantega.niagara.Task;

public class ClientSubscription implements Sink<ConsumerRecord> {

    public final F<String, Boolean> spec;
    public final ServerWebSocket          webSocket;

    public ClientSubscription(F<String, Boolean> spec, ServerWebSocket webSocket) {
        this.spec = spec;
        this.webSocket = webSocket;
    }

    public static ClientSubscription subscription(ServerWebSocket webSocket, List<String> prefixes) {
        return new ClientSubscription(
          incTopic->
            prefixes.exists(incTopic::startsWith),
          webSocket);
    }

    public static ClientSubscription firehose(ServerWebSocket webSocket) {
        return new ClientSubscription(str -> true, webSocket);
    }

    @Override
    public Task<Unit> consume(ConsumerRecord consumerRecord) {
        return
          spec.f(consumerRecord.topic.name) ?
            Task.tryRunnableTask(() -> {
                System.out.println("Sending msg "+consumerRecord.msg);
            String msg = JsonWriter.write(Client.consumerRecordCodec.encode(consumerRecord));
            System.out.println("Sending json "+msg);
            webSocket.writeTextMessage(msg);
            }) :
            Task.noOp;
    }
}
