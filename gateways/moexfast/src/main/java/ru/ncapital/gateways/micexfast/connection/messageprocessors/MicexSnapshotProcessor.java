// Copyright 2016 Itiviti Group All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;

/**
 * Created by Egor on 30-Sep-16.
 */
public class MicexSnapshotProcessor extends SnapshotProcessor {
    public MicexSnapshotProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected String getSecurityId(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }
}
