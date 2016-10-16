package ru.ncapital.gateways.micexfast;

import ru.ncapital.gateways.micexfast.domain.ProductType;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;
import ru.ncapital.gateways.moexfast.NullGatewayConfiguration;

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

    @Override
    public String[] getAllowedSecurityIds() {
        return new String[0];
    }
}
