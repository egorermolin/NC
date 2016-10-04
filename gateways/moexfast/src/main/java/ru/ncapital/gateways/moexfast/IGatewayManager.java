package ru.ncapital.gateways.moexfast;

/**
 * Created by egore on 12/28/15.
 */
public interface IGatewayManager {
    void start();

    void stop();

    void subscribeForMarketData(String subscriptionCode);
}
