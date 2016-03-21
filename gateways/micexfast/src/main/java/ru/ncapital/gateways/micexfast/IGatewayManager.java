package ru.ncapital.gateways.micexfast;

import com.google.inject.ImplementedBy;
import ru.ncapital.gateways.micexfast.domain.Instrument;

/**
 * Created by egore on 12/28/15.
 */
@ImplementedBy(GatewayManager.class)
public interface IGatewayManager {
    void start();

    void stop();

    void subscribeForMarketData(String subscriptionCode);
}
