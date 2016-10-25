package ru.ncapital.gateways.fortsfast.connection.messageprocessors;

import org.openfast.GroupValue;
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
}
