package ru.ncapital.gateways.fortsfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.micexfast.MicexMarketDataManager;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.messagehandlers.OrderListMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class FortsOrderListMessageHandler extends OrderListMessageHandler {
    @AssistedInject
    public FortsOrderListMessageHandler(MarketDataManager marketDataManager, @Assisted IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected String getSecurityId(Message readMessage) {
        long securityId = readMessage.getLong("SecurityID");

        // TODO lookup for instrument symbol
        return String.valueOf(securityId);
    }

    @Override
    protected String getSecurityId(GroupValue mdEntry) {
        long securityId = mdEntry.getLong("SecurityID");

        // TODO lookup for instrument symbol
        return String.valueOf(securityId);
    }
}
