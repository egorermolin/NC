package ru.ncapital.gateways.fortsfast.connection.messageprocessors;

import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class FortsSnapshotProcessor extends SnapshotProcessor {
    public FortsSnapshotProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected String getSecurityId(Message readMessage) {
        long securityId = readMessage.getLong("SecurityID");

        // TODO lookup for instrument symbol
        return String.valueOf(securityId);
    }
}
