package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.connection.MarketType;
import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.micexfast.performance.IGatewayPerformanceLogger;

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
    public TradingSessionId[] getAllowedTradingSessionIds() {
        return new TradingSessionId[0];
    }

    @Override
    public ProductType[] getAllowedProductTypes() {
        return new ProductType[0];
    }

    @Override
    public String[] getAllowedSecurityIds() {
        return new String[] {"*"};
    }

    @Override
    public IGatewayPerformanceLogger getPerformanceLogger() {
        return null;
    }

    @Override
    public boolean isAsynchChannelReader() {
        return false;
    }
}
