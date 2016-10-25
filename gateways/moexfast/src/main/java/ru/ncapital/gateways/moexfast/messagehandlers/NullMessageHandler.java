// Copyright 2016 Itiviti Group All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.moexfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.slf4j.Logger;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by Egor on 25-Oct-16.
 */
public class NullMessageHandler<T> extends AMessageHandler<T> {
    @AssistedInject
    public NullMessageHandler(MarketDataManager<T> marketDataManager, @Assisted IGatewayConfiguration gatewayConfiguration) {
        super(marketDataManager, gatewayConfiguration);
    }

    @Override
    public void flushIncrementals() {
    }

    @Override
    public MessageHandlerType getType() {
        return null;
    }

    @Override
    protected Logger getLogger() {
        return null;
    }

    @Override
    protected void onBeforeSnapshot(T exchangeSecurityId) {

    }

    @Override
    protected void onAfterSnapshot(T exchangeSecurityId) {

    }

    @Override
    protected void onSnapshotMdEntry(T exchangeSecurityId, GroupValue mdEntry) {

    }

    @Override
    protected void onIncrementalMdEntry(T exchangeSecurityId, GroupValue mdEntry, PerformanceData perfData) {

    }

    @Override
    protected T getExchangeSecurityId(Message readMessage) {
        return null;
    }

    @Override
    protected T getExchangeSecurityId(GroupValue mdEntry) {
        return null;
    }

    @Override
    protected String getTradeId(GroupValue mdEntry) {
        return null;
    }
}
