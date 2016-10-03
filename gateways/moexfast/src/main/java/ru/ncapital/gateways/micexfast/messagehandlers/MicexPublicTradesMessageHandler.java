// Copyright 2016 Itiviti Group All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.messagehandlers.PublicTradesMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class MicexPublicTradesMessageHandler extends PublicTradesMessageHandler {
    @AssistedInject
    public MicexPublicTradesMessageHandler(MarketDataManager marketDataManager, @Assisted IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected String getSecurityId(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }

    @Override
    protected String getSecurityId(GroupValue mdEntry) {
        String symbol = mdEntry.getString("Symbol");
        String tradingSessionId = mdEntry.getString("TradingSessionID");

        return MicexInstrument.getSecurityId(symbol, tradingSessionId);
    }
}
