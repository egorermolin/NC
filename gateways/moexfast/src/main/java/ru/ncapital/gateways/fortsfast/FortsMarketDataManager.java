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
import ru.ncapital.gateways.moexfast.domain.intf.IChannelStatus;
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
        IMessageHandler<Long> messageHandlerForOrderBook = messageHandlerFactory.createOrderBookMessageHandler(configuration);

        IMessageSequenceValidator<Long> sequenceValidatorForOrderList = messageSequenceValidatorFactory.createMessageSequenceValidatorForOrderList();
        IMessageSequenceValidator<Long> sequenceValidatorForStatistics = messageSequenceValidatorFactory.createMessageSequenceValidatorForStatistics();
        IMessageSequenceValidator<Long> sequenceValidatorForOrderBook = messageSequenceValidatorFactory.createMessageSequenceValidatorForOrderBook();

        snapshotProcessorForOrderList = new FortsSnapshotProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList);
        snapshotProcessorForStatistics = new FortsSnapshotProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics);
        snapshotProcessorForOrderBook = new FortsSnapshotProcessor(messageHandlerForOrderBook, sequenceValidatorForOrderBook);

        incrementalProcessorForOrderList = new FortsIncrementalProcessor(messageHandlerForOrderList, sequenceValidatorForOrderList, configuration);
        incrementalProcessorForStatistics = new FortsIncrementalProcessor(messageHandlerForStatistics, sequenceValidatorForStatistics, configuration);
        incrementalProcessorForOrderBook = new FortsIncrementalProcessor(messageHandlerForOrderBook, sequenceValidatorForOrderBook, configuration);

        return this;
    }

    @Override
    protected OrderDepthEngine<Long> createDepthEngine(IGatewayConfiguration configuration) {
        return new OrderDepthEngine<Long>(configuration) {
            @Override
            public DepthLevel<Long> createSnapshotDepthLevel(Long exchangeSecurityId) {
                return new DepthLevel<Long>(getInstrumentManager().getSecurityId(exchangeSecurityId), exchangeSecurityId) {
                    { setMdUpdateAction(MdUpdateAction.SNAPSHOT); }
                };
            }

            @Override
            protected boolean updateInRecovery(BBO<Long> previousBBO, BBO<Long> newBBO) {
                boolean changed = false;
                for (IChannelStatus.ChannelType channelType :
                        new IChannelStatus.ChannelType[] {
                                IChannelStatus.ChannelType.OrderList,
                                IChannelStatus.ChannelType.BBO,
                                IChannelStatus.ChannelType.Statistics}) {

                    if (newBBO.isInRecoverySet(channelType)) {
                        if (!previousBBO.isInRecoverySet(channelType) ||
                                (previousBBO.isInRecoverySet(channelType) && newBBO.isInRecovery(channelType) != previousBBO.isInRecovery(channelType))) {
                            changed = true;
                            previousBBO.setInRecovery(newBBO.isInRecovery(channelType), channelType);
                        }
                    }
                }
                return changed;
            }
        };
    }

    @Override
    public BBO<Long> createBBO(Long exchangeSecurityId) {
        return new BBO<>(
                getInstrumentManager().getSecurityId(exchangeSecurityId),
                exchangeSecurityId
        );
    }

    @Override
    public DepthLevel<Long> createDepthLevel(Long exchangeSecurityId) {
        return new DepthLevel<>(
                getInstrumentManager().getSecurityId(exchangeSecurityId),
                exchangeSecurityId
        );
    }

    @Override
    public PublicTrade<Long> createPublicTrade(Long exchangeSecurityId) {
        return new PublicTrade<>(
                getInstrumentManager().getSecurityId(exchangeSecurityId),
                exchangeSecurityId
        );
    }
}
