package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.moexfast.IGatewayConfiguration;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.MarketType;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/11/16.
 */
public abstract class IncrementalProcessor<T> extends Processor implements IIncrementalProcessor {

    private final IMessageSequenceValidator<T> sequenceValidator;

    private IMessageHandler<T> messageHandler;

    private final Utils.SecondFractionFactor sendingTimeFractionFactor;

    public IncrementalProcessor(IMessageHandler<T> messageHandler, IMessageSequenceValidator<T> sequenceValidator, IGatewayConfiguration configuration) {
        this.messageHandler = messageHandler;
        this.sequenceValidator = sequenceValidator;

        switch(configuration.getVersion()) {
            case V2016:
                sendingTimeFractionFactor = Utils.SecondFractionFactor.MILLISECONDS;
                break;
            case V2017:
            default:
                if (configuration.getMarketType() == MarketType.FUT)
                    sendingTimeFractionFactor = Utils.SecondFractionFactor.MILLISECONDS;
                else
                    sendingTimeFractionFactor = Utils.SecondFractionFactor.MICROSECONDS;

                break;
        }
    }

    private long convertSendingTimeToTicks(long sendingTime) {
        switch (sendingTimeFractionFactor) {
            case MICROSECONDS:
                return Utils.convertTodayToTicks(sendingTime % 1_00_00_00_000_000L);
            case MILLISECONDS:
                return Utils.convertTodayToTicks((sendingTime % 1_00_00_00_000L) * 1_000L);
            default:
                return 0;
        }
    }

    @Override
    protected void processMessage(Message readMessage) {
        long inTimestamp = getInTimestamp();
        long dequeTimestamp = Utils.currentTimeInTicks();
        long sendingTime = convertSendingTimeToTicks(readMessage.getLong("SendingTime"));
        boolean lastFragment = isLastFragment(readMessage);

        synchronized (sequenceValidator) {
            SequenceValue mdEntries = getMdEntries(readMessage);
            if (mdEntries == null)
                return;

            for (int i = 0; i < mdEntries.getLength(); ++i) {
                GroupValue mdEntry = mdEntries.get(i);
                if (mdEntry.getValue("RptSeq") == null) {
                    getLogger().warn("Market Reset received " + readMessage);
                    if (mdEntry.getValue("ExchangeTradingSessionID") == null)
                        sequenceValidator.onMarketReset();
                    messageHandler.onMarketReset();
                    continue;
                }
                int seqNum = mdEntry.getInt("RptSeq");
                boolean lastEntryInTransaction = isLastEntryInTransaction(mdEntry);

                checkTradeId(mdEntry);

                T exchangeSecurityId = getExchangeSecurityId(mdEntry);
                PerformanceData performanceData = new PerformanceData()
                        .setExchangeSendingTime(sendingTime)
                        .setGatewayDequeTime(dequeTimestamp)
                        .setGatewayInTime(inTimestamp);

                if (!messageHandler.isAllowedUpdate(exchangeSecurityId))
                    continue;

                if (sequenceValidator.isRecovering(exchangeSecurityId, seqNum, false)) {
                    if (sequenceValidator.onIncrementalSeq(exchangeSecurityId, seqNum)) {
                        messageHandler.onIncremental(mdEntry, performanceData);

                        // finished recovering
                        StoredMdEntry[] storedMdEntriesToProcess = sequenceValidator.stopRecovering(exchangeSecurityId);
                        if (storedMdEntriesToProcess != null)
                            for (StoredMdEntry storedMdEntryToProcess : storedMdEntriesToProcess) {
                                sequenceValidator.onIncrementalSeq(exchangeSecurityId,
                                        storedMdEntryToProcess.getSequenceNumber());

                                messageHandler.onIncremental(storedMdEntryToProcess.getMdEntry(),
                                        storedMdEntryToProcess.getPerformanceData());
                            }
                    } else {
                        sequenceValidator.storeIncremental(exchangeSecurityId, seqNum, mdEntry, performanceData, lastFragment, lastEntryInTransaction);
                    }
                } else {
                    if (sequenceValidator.onIncrementalSeq(exchangeSecurityId, seqNum)) {
                        messageHandler.onIncremental(mdEntry, performanceData);
                    } else {
                        sequenceValidator.storeIncremental(exchangeSecurityId, seqNum, mdEntry, performanceData, lastFragment, lastEntryInTransaction);
                        sequenceValidator.startRecovering(exchangeSecurityId);
                    }
                }
                if (lastEntryInTransaction)
                    messageHandler.flushIncrementals();
            }
            if (lastFragment)
                messageHandler.flushIncrementals();
        }
    }

    protected void checkTradeId(GroupValue mdEntry) { }

    protected abstract T getExchangeSecurityId(GroupValue mdEntry);

    protected abstract SequenceValue getMdEntries(Message readMessage);

    protected abstract boolean isLastEntryInTransaction(GroupValue mdEntry);

    protected abstract boolean isLastFragment(Message readMessage);
}
