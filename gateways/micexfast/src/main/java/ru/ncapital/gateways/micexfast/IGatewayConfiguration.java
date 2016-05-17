package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.connection.MarketType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.performance.IGatewayPerformanceLogger;

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
}
