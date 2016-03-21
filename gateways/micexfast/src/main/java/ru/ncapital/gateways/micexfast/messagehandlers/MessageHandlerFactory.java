package ru.ncapital.gateways.micexfast.messagehandlers;

import com.google.inject.name.Named;
import ru.ncapital.gateways.micexfast.domain.TradingSessionId;

/**
 * Created by egore on 2/2/16.
 */
public interface MessageHandlerFactory {
    @Named("orderlist") IMessageHandler createOrderListMessageHandler(TradingSessionId[] allowedTradingSessionIds, String[] allowedSymbols);
    @Named("statistics") IMessageHandler createStatisticsMessageHandler(TradingSessionId[] allowedTradingSessionIds, String[] allowedSymbols);
    @Named("publictrades") IMessageHandler createPublicTradesMessageHandler(TradingSessionId[] allowedTradingSessionIds, String[] allowedSymbols);
}
