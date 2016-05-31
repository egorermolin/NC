package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.domain.Instrument;
import ru.ncapital.gateways.micexfast.performance.PerformanceData;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;

/**
 * Created by egore on 1/11/16.
 */
public class IncrementalProcessor extends Processor implements IIncrementalProcessor {

    public IncrementalProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    @Override
    protected void processMessage(Message readMessage) {
        long inTimestamp = getInTimestamp();
        long dequeTimestamp = Utils.currentTimeInTicks();
        long sendingTime = Utils.convertTodayToTicks((readMessage.getLong("SendingTime") % 1_00_00_00_000L) * 1_000L);

        synchronized (sequenceValidator) {
            SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
            for (int i = 0; i < mdEntries.getLength(); ++i) {
                GroupValue mdEntry = mdEntries.get(i);
                if (mdEntry.getValue("RptSeq") == null) {
                    getLogger().warn("Market Reset received " + readMessage);
                    continue;
                }

                String symbol = mdEntry.getString("Symbol");
                String tradingSessionId = mdEntry.getString("TradingSessionID");
                String securityId = symbol + Instrument.BOARD_SEPARATOR + tradingSessionId;
                PerformanceData performanceData = new PerformanceData()
                        .setExchangeSendingTime(sendingTime)
                        .setGatewayDequeTime(dequeTimestamp)
                        .setGatewayInTime(inTimestamp);

                int rptSeqNum = mdEntry.getInt("RptSeq");

                if (!messageHandler.isAllowedUpdate(symbol, tradingSessionId))
                    continue;

                if (sequenceValidator.isRecovering(securityId, false)) {
                    if (sequenceValidator.onIncrementalSeq(securityId, rptSeqNum)) {
                        messageHandler.onIncremental(mdEntry, performanceData);

                        // finished recovering
                        StoredMdEntry[] storedMdEntriesToProcess = sequenceValidator.stopRecovering(securityId);
                        if (storedMdEntriesToProcess != null)
                            for (StoredMdEntry storedMdEntryToProcess : storedMdEntriesToProcess) {
                                sequenceValidator.onIncrementalSeq(securityId,
                                        storedMdEntryToProcess.getSequenceNumber());

                                messageHandler.onIncremental(storedMdEntryToProcess.getMdEntry(),
                                        storedMdEntryToProcess.getPerformanceData());
                            }
                    } else {
                        sequenceValidator.storeIncremental(securityId, rptSeqNum, mdEntry, performanceData);
                    }
                    continue;
                }

                if (sequenceValidator.onIncrementalSeq(securityId, rptSeqNum)) {
                    messageHandler.onIncremental(mdEntry, performanceData);
                } else {
                    sequenceValidator.storeIncremental(securityId, rptSeqNum, mdEntry, performanceData);
                    sequenceValidator.startRecovering(securityId);
                }
            }
            messageHandler.flushIncrementals();
        }
    }
}
