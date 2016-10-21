package ru.ncapital.gateways.fortsfast;

import com.google.inject.Singleton;
import ru.ncapital.gateways.fortsfast.connection.messageprocessors.FortsIncrementalProcessor;
import ru.ncapital.gateways.fortsfast.connection.messageprocessors.FortsSnapshotProcessor;
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
 * Created by Egor on 03-Oct-16.
 */
@Singleton
public class FortsMarketDataManager extends MarketDataManager<Long> {

    public MarketDataManager configure(IGatewayConfiguration configuration) {
        super.configure(configuration);

        IMessageHandler<Long> messageHandlerForOrderList = messageHandlerFactory.createOrderListMessageHandler(configuration);
        IMessageHandler<Long> messageHandlerForStatistics = messageHandlerFactory.createStatisticsMessageHandler(configuration);
        IMessageHandler<Long> messageHandlerForPublicTrades = messageHandlerFactory.createPublicTradesMessageHandler(configuration);

        IMessageSequenceValidator<Long> sequenceValidatorForOrderList = messageSequenceValidatorFactory.createMessageSequenceValidatorForOrderList();
        IMessageSequenceValidator<Long> sequenceValidatorForStatistics = messageSequenceValidatorFactory.createMessageSequenceValidatorForStatistics();
        IMessageSequenceValidator<Long> sequenceValidatorForPublicTrades = messageSequenceValidatorFactory.createMessageSequenceValidatorForPublicTrades();

        snapshotProcessorForOrderList = new FortsSnapshotProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        snapshotProcessorForStatistics = new FortsSnapshotProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);

        incrementalProcessorForOrderList = new FortsIncrementalProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        incrementalProcessorForStatistics = new FortsIncrementalProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);
        incrementalProcessorForPublicTrades = new FortsIncrementalProcessor(messageHandlerForPublicTrades, sequenceValidatorForPublicTrades);

        return this;
    }

    @Override
    protected OrderDepthEngine<Long> createDepthEngine() {
        return new OrderDepthEngine<Long>() {
            @Override
            public DepthLevel<Long> createSnapshotDepthLevel(Long exchangeSecurityId) {
                return new DepthLevel<Long>(instrumentManager.getSecurityId(exchangeSecurityId), exchangeSecurityId) {
                    { setMdUpdateAction(MdUpdateAction.SNAPSHOT); }
                };
            }
        };
    }

    @Override
    public BBO<Long> createBBO(Long exchangeSecurityId) {
        return new BBO<>(
                instrumentManager.getSecurityId(exchangeSecurityId),
                exchangeSecurityId
        );
    }

    @Override
    public DepthLevel<Long> createDepthLevel(Long exchangeSecurityId) {
        return new DepthLevel<>(
                instrumentManager.getSecurityId(exchangeSecurityId),
                exchangeSecurityId
        );
    }

    @Override
    public PublicTrade<Long> createPublicTrade(Long exchangeSecurityId) {
        return new PublicTrade<>(
                instrumentManager.getSecurityId(exchangeSecurityId),
                exchangeSecurityId
        );
    }
}
