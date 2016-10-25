package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Message;

/**
 * Created by egore on 26.01.2016.
 */
public class HeartbeatProcessor extends Processor {

    public HeartbeatProcessor() {
        super(null, null);
    }

    @Override
    public void processMessage(Message readMessage) {
    }
}
