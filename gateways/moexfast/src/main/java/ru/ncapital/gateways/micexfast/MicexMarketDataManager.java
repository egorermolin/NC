package ru.ncapital.gateways.micexfast;

import com.google.inject.Singleton;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexIncrementalProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexSnapshotProcessor;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.OrderDepthEngine;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.domain.MdUpdateAction;
import ru.ncapital.gateways.moexfast.domain.impl.BBO;
import ru.ncapital.gateways.moexfast.domain.impl.DepthLevel;
import ru.ncapital.gateways.moexfast.domain.impl.PublicTrade;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by egore on 12/7/15.
 */

@Singleton
public class MicexMarketDataManager extends MarketDataManager<String> {

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        super.configure(configuration);

        IMessageHandler<String> messageHandlerForOrderList = messageHandlerFactory.createOrderListMessageHandler(configuration);
        IMessageHandler<String> messageHandlerForStatistics = messageHandlerFactory.createStatisticsMessageHandler(configuration);
        IMessageHandler<String> messageHandlerForPublicTrades = messageHandlerFactory.createPublicTradesMessageHandler(configuration);

        IMessageSequenceValidator<String> sequenceValidatorForOrderList = messageSequenceValidatorFactory.createMessageSequenceValidatorForOrderList();
        IMessageSequenceValidator<String> sequenceValidatorForStatistics = messageSequenceValidatorFactory.createMessageSequenceValidatorForStatistics();
        IMessageSequenceValidator<String> sequenceValidatorForPublicTrades = messageSequenceValidatorFactory.createMessageSequenceValidatorForPublicTrades();

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
                return new DepthLevel<String>(exchangeSecurityId, exchangeSecurityId) {
                    { setMdUpdateAction(MdUpdateAction.SNAPSHOT); }
                };
            }
        };
    }

    @Override
    public BBO<String> createBBO(String exchangeSecurityId) {
        return new BBO<>(exchangeSecurityId, exchangeSecurityId);
    }

    @Override
    public DepthLevel<String> createDepthLevel(String exchangeSecurityId) {
        return new DepthLevel<>(exchangeSecurityId, exchangeSecurityId);
    }

    @Override
    public PublicTrade<String> createPublicTrade(String exchangeSecurityId) {
        return new PublicTrade<>(exchangeSecurityId, exchangeSecurityId);
    }
}