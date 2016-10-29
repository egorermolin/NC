package ru.ncapital.gateways.fortsfast.connection.messageprocessors;

import org.openfast.Message;
import ru.ncapital.gateways.fortsfast.domain.FortsInstrument;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

public class FortsSnapshotProcessor extends SnapshotProcessor<Long> {
    public FortsSnapshotProcessor(IMessageHandler<Long> messageHandler, IMessageSequenceValidator<Long> sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected Long getExchangeSecurityId(Message readMessage) {
        Long securityID = readMessage.getLong("SecurityID");

        return FortsInstrument.getExchangeSecurityId(securityID);
    }
}
