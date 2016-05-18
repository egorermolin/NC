package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.Logger;

/**
 * Created by egore on 12/15/15.
 */
public class MessageReaderStarter implements Runnable {
    private MessageReader mcr;

    public MessageReaderStarter(MessageReader mcr) {
        this.mcr = mcr;
    }

    @Override
    public void run() {
        mcr.start();
    }
}
