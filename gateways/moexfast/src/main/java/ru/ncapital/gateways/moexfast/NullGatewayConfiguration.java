package ru.ncapital.gateways.moexfast;

import ru.ncapital.gateways.moexfast.connection.MarketType;
import ru.ncapital.gateways.moexfast.performance.IGatewayPerformanceLogger;

/**
 * Created by egore on 17.02.2016.
 */
public class NullGatewayConfiguration implements IGatewayConfiguration {
    @Override
    public IMarketDataHandler getMarketDataHandler() {
        return null;
    }

    @Override
    public String getFastTemplatesFile() {
        return null;
    }

    @Override
    public String getNetworkInterface() {
        return null;
    }

    @Override
    public String getConnectionsFile() {
        return null;
    }

    @Override
    public MarketType getMarketType() {
        return MarketType.CURR;
    }

    @Override
    public IGatewayPerformanceLogger getPerformanceLogger() {
        return null;
    }

    @Override
    public boolean isAsynchChannelReader() {
        return true;
    }

    @Override
    public boolean isListenSnapshotChannelOnlyIfNeeded() {
        return true;
    }

    @Override
    public long getFeedDownTimeout() {
        return 60_000_000_0L; // 60s in ticks
    }

    @Override
    public boolean restartOnAllFeedDown() {
        return true;
    }

    @Override
    public boolean publicTradesFromOrdersList() {
        return false;
    }
}
