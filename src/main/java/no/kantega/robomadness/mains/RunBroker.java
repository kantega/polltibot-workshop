package no.kantega.robomadness.mains;

import no.kantega.robomadness.broker.Broker;
import no.kantega.robomadness.Util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunBroker {
    public static void main(String[] args) {

        Broker          broker          = new Broker();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        broker.start(executorService).bind(server ->
          Util.println("Server started").execute()
        );

    }
}
