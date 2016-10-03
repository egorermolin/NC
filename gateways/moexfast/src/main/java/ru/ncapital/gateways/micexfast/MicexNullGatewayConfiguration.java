package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.IMarketDataHandler;
import ru.ncapital.gateways.moexfast.NullGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.MarketType;
import ru.ncapital.gateways.moexfast.performance.IGatewayPerformanceLogger;

/**
 * Created by egore on 17.02.2016.
 */
public class MicexNullGatewayConfiguration extends NullGatewayConfiguration implements IMicexGatewayConfiguration {
    @Override
    public TradingSessionId[] getAllowedTradingSessionIds() {
        return new TradingSessionId[0];
    }

    @Override
    public ProductType[] getAllowedProductTypes() {
        return new ProductType[0];
    }
}
