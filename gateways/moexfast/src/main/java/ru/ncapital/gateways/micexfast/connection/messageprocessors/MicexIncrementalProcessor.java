package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class MicexIncrementalProcessor extends IncrementalProcessor<String> {

    private String lastDealNumber;

    public MicexIncrementalProcessor(IMessageHandler<String> messageHandler, IMessageSequenceValidator<String> sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected void checkTradeId(GroupValue mdEntry) {
        String tradeId = mdEntry.getString("DealNumber");
        if (tradeId != null) {
            if (!tradeId.equals(lastDealNumber)) {
                lastDealNumber = tradeId;
            } else {
                mdEntry.setString("DealNumber", null);
            }
        }
    }

    @Override
    protected String getExchangeSecurityId(GroupValue mdEntry) {
        String symbol = mdEntry.getString("Symbol");
        String tradingSessionId = mdEntry.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }

    @Override
    protected SequenceValue getMdEntries(Message readMessage) {
        return readMessage.getSequence("GroupMDEntries");
    }
}
