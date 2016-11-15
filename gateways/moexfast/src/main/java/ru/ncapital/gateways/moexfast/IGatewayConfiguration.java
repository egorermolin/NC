package ru.ncapital.gateways.moexfast;

import ru.ncapital.gateways.moexfast.connection.MarketType;
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

    IGatewayPerformanceLogger getPerformanceLogger();

    boolean isAsynchChannelReader();

    boolean isListenSnapshotChannelOnlyIfNeeded();

    long getFeedDownTimeout();

    boolean restartOnAllFeedDown();

    boolean publicTradesFromOrdersList();
}
