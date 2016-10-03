package ru.ncapital.gateways.moexfast;

import com.google.inject.ImplementedBy;
import ru.ncapital.gateways.micexfast.MicexGatewayManager;

/**
 * Created by egore on 12/28/15.
 */
@ImplementedBy(MicexGatewayManager.class)
public interface IGatewayManager {
    void start();

    void stop();

    void subscribeForMarketData(String subscriptionCode);
}
