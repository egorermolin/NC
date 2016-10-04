package ru.ncapital.gateways.fortsfast.connection.messageprocessors;

import org.openfast.GroupValue;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class FortsIncrementalProcessor extends IncrementalProcessor {
    public FortsIncrementalProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected String getSecurityId(GroupValue mdEntry) {
        long securityId = mdEntry.getLong("SecurityID");

        // TODO lookup for instrument symbol
        return String.valueOf(securityId);
    }
}
