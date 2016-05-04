package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.Logger;

/**
 * Created by egore on 12/15/15.
 */
public class MessageReaderStarter implements Runnable {
    private MessageReader mcr;

    public MessageReaderStarter(Logger logger, MessageReader mcr) {
        this.mcr = mcr;

        logger.info("START " + mcr);
    }

    @Override
    public void run() {
        mcr.start();
    }
}
