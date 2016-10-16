package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class MicexSnapshotProcessor extends SnapshotProcessor<String> {
    public MicexSnapshotProcessor(IMessageHandler<String> messageHandler, IMessageSequenceValidator<String> sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected String getExchangeSecurityId(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }
}
