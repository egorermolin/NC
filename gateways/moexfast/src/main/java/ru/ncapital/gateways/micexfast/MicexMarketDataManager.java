package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexIncrementalProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexSnapshotProcessor;
import ru.ncapital.gateways.micexfast.domain.MicexBBO;
import ru.ncapital.gateways.micexfast.domain.MicexDepthLevel;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.InstrumentManager;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.OrderDepthEngine;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.intf.IDepthLevel;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by egore on 12/7/15.
 */

@Singleton
public class MicexMarketDataManager extends MarketDataManager<String> {

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        super.configure(configuration);

        IMessageHandler messageHandlerForOrderList = messageHandlerFactory.createOrderListMessageHandler(configuration);
        IMessageHandler messageHandlerForStatistics = messageHandlerFactory.createStatisticsMessageHandler(configuration);
        IMessageHandler messageHandlerForPublicTrades = messageHandlerFactory.createPublicTradesMessageHandler(configuration);

        IMessageSequenceValidator sequenceValidatorForOrderList = messageSequenceValidatorFactory.createMessageSequenceValidatorForOrderList();
        IMessageSequenceValidator sequenceValidatorForStatistics = messageSequenceValidatorFactory.createMessageSequenceValidatorForStatistics();
        IMessageSequenceValidator sequenceValidatorForPublicTrades = messageSequenceValidatorFactory.createMessageSequenceValidatorForPublicTrades();

        snapshotProcessorForOrderList = new MicexSnapshotProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        snapshotProcessorForStatistics = new MicexSnapshotProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);

        incrementalProcessorForOrderList = new MicexIncrementalProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        incrementalProcessorForStatistics = new MicexIncrementalProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);
        incrementalProcessorForPublicTrades = new MicexIncrementalProcessor(messageHandlerForPublicTrades, sequenceValidatorForPublicTrades);

        return this;
    }

    @Override
    protected OrderDepthEngine<String> createDepthEngine() {
        return new OrderDepthEngine<String>() {
            @Override
            public DepthLevel<String> createSnapshotDepthLevel(String exchangeSecurityId) {
                return new MicexDepthLevel(exchangeSecurityId, MdUpdateAction.SNAPSHOT);
            }
        };
    }

    @Override
    public BBO<String> createBBO(String exchangeSecurityId) {
        return new MicexBBO(exchangeSecurityId);
    }

   // @Override
   // public String convertSubscriptionKeyToExchangeSecurityId(String subsriptionKey) {
    //    return subsriptionKey;
   // }

    @Override
    public Logger getLogger() {
        return LoggerFactory.getLogger("MicexMarketDataManager");
    }


}