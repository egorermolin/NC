package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;

/**
 * Created by egore on 16.02.2016.
 */
public interface IMicexGatewayConfiguration extends IGatewayConfiguration{
    TradingSessionId[] getAllowedTradingSessionIds();

    ProductType[] getAllowedProductTypes();
}
