package ru.ncapital.gateways.moexfast.connection.multicast;

/**
 * Created by egore on 12/15/15.
 */
public class MessageReaderStarter implements Runnable {
    private MessageReader messageReader;

    public MessageReaderStarter(MessageReader messageReader) {
        this.messageReader = messageReader;
    }

    @Override
    public void run() {
        messageReader.start();
    }
}
