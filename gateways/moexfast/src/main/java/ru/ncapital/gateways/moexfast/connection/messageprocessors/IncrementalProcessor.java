package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/11/16.
 */
public abstract class IncrementalProcessor<T> extends Processor<T> implements IIncrementalProcessor {

    public IncrementalProcessor(IMessageHandler<T> messageHandler, IMessageSequenceValidator<T> sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected void processMessage(Message readMessage) {
        long inTimestamp = getInTimestamp();
        long dequeTimestamp = Utils.currentTimeInTicks();
        long sendingTime = Utils.convertTodayToTicks((readMessage.getLong("SendingTime") % 1_00_00_00_000L) * 1_000L);

        synchronized (sequenceValidator) {
            SequenceValue mdEntries = getMdEntries(readMessage);
            for (int i = 0; i < mdEntries.getLength(); ++i) {
                GroupValue mdEntry = mdEntries.get(i);
                if (mdEntry.getValue("RptSeq") == null) {
                    getLogger().warn("Market Reset received " + readMessage);
                    continue;
                }
                int rptSeqNum = mdEntry.getInt("RptSeq");

                checkTradeId(mdEntry);

                T exchangeSecurityId = getExchangeSecurityId(mdEntry);
                PerformanceData performanceData = new PerformanceData()
                        .setExchangeSendingTime(sendingTime)
                        .setGatewayDequeTime(dequeTimestamp)
                        .setGatewayInTime(inTimestamp);

                if (!messageHandler.isAllowedUpdate(exchangeSecurityId))
                    continue;

                if (sequenceValidator.isRecovering(exchangeSecurityId, false)) {
                    if (sequenceValidator.onIncrementalSeq(exchangeSecurityId, rptSeqNum)) {
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
                        sequenceValidator.storeIncremental(exchangeSecurityId, rptSeqNum, mdEntry, performanceData);
                    }
                } else {
                    if (sequenceValidator.onIncrementalSeq(exchangeSecurityId, rptSeqNum)) {
                        messageHandler.onIncremental(mdEntry, performanceData);
                    } else {
                        sequenceValidator.storeIncremental(exchangeSecurityId, rptSeqNum, mdEntry, performanceData);
                        sequenceValidator.startRecovering(exchangeSecurityId);
                    }
                }
            }
            messageHandler.flushIncrementals();
        }
    }

    protected void checkTradeId(GroupValue mdEntry) { }

    protected abstract T getExchangeSecurityId(GroupValue mdEntry);

    protected abstract SequenceValue getMdEntries(Message readMessage);
}
