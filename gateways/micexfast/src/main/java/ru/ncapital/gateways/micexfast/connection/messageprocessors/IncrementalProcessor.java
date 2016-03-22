package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.*;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;

/**
 * Created by egore on 1/11/16.
 */
public class IncrementalProcessor extends Processor {
    private IMessageHandler messageHandler;

    private IMessageSequenceValidator sequenceValidator;

    public IncrementalProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        this.messageHandler = messageHandler;
        this.sequenceValidator = sequenceValidator;
    }

    @Override
    protected void processMessage(Message readMessage) {
        long inTime = getInTimestamp();
        synchronized (sequenceValidator) {
            SequenceValue mdEntries = readMessage.getSequence("GroupMDEntries");
            for (int i = 0; i < mdEntries.getLength(); ++i) {
                GroupValue mdEntry = mdEntries.get(i);
                String securityId = mdEntry.getString("Symbol") + ":" + mdEntry.getString("TradingSessionID");
                int rptSeqNum = mdEntry.getInt("RptSeq");

                if (sequenceValidator.isRecovering(securityId)) {
                    if (sequenceValidator.onIncrementalSeq(securityId, rptSeqNum)) {
                        messageHandler.onIncremental(mdEntry, inTime);

                        // finished recovering
                        GroupValue[] mdEntriesToProcess = sequenceValidator.stopRecovering(securityId);
                        if (mdEntriesToProcess != null)
                            for (GroupValue mdEntryToProcess : mdEntriesToProcess) {
                                sequenceValidator.onIncrementalSeq(securityId, mdEntryToProcess.getInt("RptSeq"));

                                messageHandler.onIncremental(mdEntryToProcess, inTime);
                            }
                    } else {
                        sequenceValidator.storeIncremental(mdEntry, securityId, rptSeqNum);
                    }
                    continue;
                }

                if (sequenceValidator.onIncrementalSeq(securityId, rptSeqNum)) {
                    messageHandler.onIncremental(mdEntry, inTime);
                } else {
                    sequenceValidator.storeIncremental(mdEntry, securityId, rptSeqNum);
                    sequenceValidator.startRecovering(securityId);
                }
            }
            messageHandler.flushIncrementals(inTime);
        }
    }
}
