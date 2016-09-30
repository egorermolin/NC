package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.moexfast.connection.MarketType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;
import ru.ncapital.gateways.moexfast.performance.IGatewayPerformanceLogger;

/**
 * Created by egore on 16.02.2016.
 */
public interface IGatewayConfiguration {
    IMarketDataHandler getMarketDataHandler();

    String getFastTemplatesFile();

    String getNetworkInterface();

    String getConnectionsFile();

    MarketType getMarketType();

    TradingSessionId[] getAllowedTradingSessionIds();

    ProductType[] getAllowedProductTypes();

    String[] getAllowedSecurityIds();

    IGatewayPerformanceLogger getPerformanceLogger();

    boolean isAsynchChannelReader();

    boolean isListenSnapshotChannelOnlyIfNeeded();

    long getFeedDownTimeout();

    boolean restartOnAllFeedDown();
}
