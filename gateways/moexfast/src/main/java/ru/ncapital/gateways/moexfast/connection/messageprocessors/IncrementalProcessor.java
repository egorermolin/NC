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
public abstract class IncrementalProcessor extends Processor implements IIncrementalProcessor {

    protected String lastDealNumber;

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

                String securityId = getSecurityId(mdEntry);
                PerformanceData performanceData = new PerformanceData()
                        .setExchangeSendingTime(sendingTime)
                        .setGatewayDequeTime(dequeTimestamp)
                        .setGatewayInTime(inTimestamp);

                int rptSeqNum = mdEntry.getInt("RptSeq");
                String dealNumber = mdEntry.getString("DealNumber");
                if (dealNumber != null) {
                    if (!dealNumber.equals(lastDealNumber)) {
                        lastDealNumber = dealNumber;
                    } else {
                        mdEntry.setString("DealNumber", null);
                    }
                }

                if (!messageHandler.isAllowedUpdate(securityId))
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

    protected abstract String getSecurityId(GroupValue mdEntry);
}
