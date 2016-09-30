package ru.ncapital.gateways.moexfast.connection.multicast;

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
