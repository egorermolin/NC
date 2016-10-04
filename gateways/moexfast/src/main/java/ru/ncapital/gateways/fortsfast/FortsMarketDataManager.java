package ru.ncapital.gateways.fortsfast;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.fortsfast.connection.messageprocessors.FortsIncrementalProcessor;
import ru.ncapital.gateways.fortsfast.connection.messageprocessors.FortsSnapshotProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexIncrementalProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexSnapshotProcessor;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by Egor on 03-Oct-16.
 */
@Singleton
public class FortsMarketDataManager extends MarketDataManager {

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        super.configure(configuration);

        IMessageHandler messageHandlerForOrderList = messageHandlerFactory.createOrderListMessageHandler(configuration);
        IMessageHandler messageHandlerForStatistics = messageHandlerFactory.createStatisticsMessageHandler(configuration);
        IMessageHandler messageHandlerForPublicTrades = messageHandlerFactory.createPublicTradesMessageHandler(configuration);

        IMessageSequenceValidator sequenceValidatorForOrderList = messageSequenceValidatorFactory.createMessageSequenceValidatorForOrderList();
        IMessageSequenceValidator sequenceValidatorForStatistics = messageSequenceValidatorFactory.createMessageSequenceValidatorForStatistics();
        IMessageSequenceValidator sequenceValidatorForPublicTrades = messageSequenceValidatorFactory.createMessageSequenceValidatorForPublicTrades();

        snapshotProcessorForOrderList = new FortsSnapshotProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        snapshotProcessorForStatistics = new FortsSnapshotProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);

        incrementalProcessorForOrderList = new FortsIncrementalProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        incrementalProcessorForStatistics = new FortsIncrementalProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);
        incrementalProcessorForPublicTrades = new FortsIncrementalProcessor(messageHandlerForPublicTrades, sequenceValidatorForPublicTrades);

        return this;
    }

    @Override
    public Logger getLogger() {
        return LoggerFactory.getLogger("FortsMarketDataManager");
    }

}
