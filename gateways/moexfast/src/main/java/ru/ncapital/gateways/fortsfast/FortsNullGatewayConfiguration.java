package ru.ncapital.gateways.fortsfast;

import ru.ncapital.gateways.moexfast.NullGatewayConfiguration;

/**
 * Created by egore on 17.02.2016.
 */
public class FortsNullGatewayConfiguration extends NullGatewayConfiguration implements IFortsGatewayConfiguration {

    @Override
    public String[] getAllowedUnderlyings() {
        return new String[0];
    }

    @Override
    public boolean publicTradesFromOrdersList() {
        return true;
    }
}
