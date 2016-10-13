package ru.ncapital.gateways.fortsfast;

import ru.ncapital.gateways.moexfast.IGatewayConfiguration;

/**
 * Created by Egor on 03-Oct-16.
 */
public interface IFortsGatewayConfiguration extends IGatewayConfiguration {

    String[] getAllowedUnderlyings();
}
