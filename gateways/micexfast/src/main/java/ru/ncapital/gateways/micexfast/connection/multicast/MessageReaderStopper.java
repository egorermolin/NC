package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.Logger;

/**
 * Created by egore on 12/15/15.
 */
public class MessageReaderStopper implements Runnable {
    private MessageReader mcr;

    public MessageReaderStopper(Logger logger, MessageReader mcr) {
        this.mcr = mcr;

        logger.info("STOP " + mcr);
    }

    @Override
    public void run() {
        mcr.stop();
    }
}
