package ru.ncapital.gateways.moexfast.messagehandlers;

import com.google.inject.name.Named;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;

/**
 * Created by egore on 2/2/16.
 */
public interface MessageHandlerFactory<T> {
    @Named("orderlist") IMessageHandler<T> createOrderListMessageHandler(IGatewayConfiguration configuration);
    @Named("statistics") IMessageHandler<T> createStatisticsMessageHandler(IGatewayConfiguration configuration);
    @Named("publictrades") IMessageHandler<T> createPublicTradesMessageHandler(IGatewayConfiguration configuration);
}
