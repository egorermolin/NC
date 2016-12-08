package ru.ncapital.gateways.fortsfast.messagehandlers;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.fortsfast.domain.FortsInstrument;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.messagehandlers.StatisticsMessageHandler;

/**
 * Created by Egor on 30-Sep-16.
 */
public class FortsStatisticsMessageHandler extends StatisticsMessageHandler<Long> {
    @AssistedInject
    public FortsStatisticsMessageHandler(MarketDataManager<Long> marketDataManager, @Assisted IGatewayConfiguration configuration) {
        super(marketDataManager, configuration);
    }

    @Override
    protected Long getExchangeSecurityId(Message readMessage) {
        return FortsInstrument.getExchangeSecurityId(readMessage.getLong("SecurityID"));
    }

    @Override
    protected Long getExchangeSecurityId(GroupValue mdEntry) {
        return FortsInstrument.getExchangeSecurityId(mdEntry.getLong("SecurityID"));
    }

    @Override
    protected String getTradeId(GroupValue mdEntry) {
        return null;
    }

    @Override
    protected SequenceValue getMdEntries(Message readMessage) {
        return readMessage.getSequence("MDEntries");
    }
}
