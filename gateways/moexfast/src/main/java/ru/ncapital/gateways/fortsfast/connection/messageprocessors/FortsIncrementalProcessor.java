package ru.ncapital.gateways.fortsfast.connection.messageprocessors;

import org.openfast.GroupValue;
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
    protected void handleTrade(GroupValue mdEntry, String tradeId) {
    }

    @Override
    protected String getTradeId(GroupValue mdEntry) {
        return mdEntry.getString("TradeId");
    }

    @Override
    protected Long getExchangeSecurityId(GroupValue mdEntry) {
        return mdEntry.getLong("SecurityID");
    }
}
