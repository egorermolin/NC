package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.name.Named;
import ru.ncapital.gateways.micexfast.IGatewayConfiguration;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

/**
 * Created by egore on 2/2/16.
 */
public interface MessageHandlerFactory {
    @Named("orderlist") IMessageHandler createOrderListMessageHandler(IGatewayConfiguration configuration);
    @Named("statistics") IMessageHandler createStatisticsMessageHandler(IGatewayConfiguration configuration);
    @Named("publictrades") IMessageHandler createPublicTradesMessageHandler(IGatewayConfiguration configuration);
}
