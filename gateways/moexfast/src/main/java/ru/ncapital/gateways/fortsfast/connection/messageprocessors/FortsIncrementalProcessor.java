package ru.ncapital.gateways.fortsfast.connection.messageprocessors;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.fortsfast.domain.FortsInstrument;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class FortsIncrementalProcessor extends IncrementalProcessor<Long> {
    public FortsIncrementalProcessor(IMessageHandler<Long> messageHandler, IMessageSequenceValidator<Long> sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected Long getExchangeSecurityId(GroupValue mdEntry) {
        return FortsInstrument.getExchangeSecurityId(mdEntry.getLong("SecurityID"));
    }

    @Override
    protected SequenceValue getMdEntries(Message readMessage) {
        return readMessage.getSequence("MDEntries");
    }

    @Override
    protected boolean isLastEntryInTransaction(GroupValue mdEntry) {
        if (mdEntry.getValue("MDFlags") == null)
            return false;

        long mdFlags = mdEntry.getLong("MDFlags");
        long lastEntryInTransaction = mdFlags & 0x1000;

        return lastEntryInTransaction != 0;
    }

    @Override
    protected boolean isLastFragment(Message readMessage) {
        return readMessage.getValue("LastFragment") == null || readMessage.getInt("LastFragment") == 1;
    }


}
