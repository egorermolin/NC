package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;

import java.util.*;

/**
 * Created by egore on 1/11/16.
 */
public class SnapshotProcessor extends Processor {
    private IMessageHandler messageHandler;

    private IMessageSequenceValidator sequenceValidator;

    private Map<String, Map<Integer, Message>> fragmentedSnapshots = new HashMap<String, Map<Integer, Message>>();

    private long sendingTimeOfSnapshotStart = 0;

    private boolean wasRecovering;

    public SnapshotProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        this.messageHandler = messageHandler;
        this.sequenceValidator = sequenceValidator;
    }

    private void processSnapshotsAndIncrementals(Iterable<Message> messages, String securityId, int rptSeqNum) {
        synchronized (sequenceValidator) {
            if (sequenceValidator.onSnapshotSeq(securityId, rptSeqNum)) {
                for (Message message : messages)
                    messageHandler.onSnapshot(message, getInTimestamp());

                // finished recovering
                GroupValue[] mdEntriesToProcess = sequenceValidator.stopRecovering(securityId);
                if (mdEntriesToProcess != null)
                    for (GroupValue mdEntry : mdEntriesToProcess) {
                        sequenceValidator.onIncrementalSeq(securityId, mdEntry.getInt("RptSeq"));

                        messageHandler.onIncremental(mdEntry, getInTimestamp());
                    }
            }
        }
    }

    private boolean checkSeqNums(Iterable<Integer> seqNums) {
        // check seqNum integrity
        int lastSeqNum = 0;
        for (int seqNum : seqNums) {
            if (lastSeqNum == 0) {
                lastSeqNum = seqNum;
            } else if (lastSeqNum + 1 < seqNum) {
                // missing message, giving up
                return false;
            } else {
                lastSeqNum = seqNum;
            }
        }
        return true;
    }

    @Override
    public void processMessage(final Message readMessage) {
        int seqNum = readMessage.getInt("MsgSeqNum");
        String securityId = readMessage.getString("Symbol") + ":" + readMessage.getString("TradingSessionID");
        int rptSeqNum = readMessage.getInt("RptSeq");
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (firstFragment)
            fragmentedSnapshots.put(securityId, Collections.synchronizedMap(new TreeMap<Integer, Message>()));

        Map<Integer, Message> messages = fragmentedSnapshots.get(securityId);
        if (messages == null)
            return;

        messages.put(seqNum, readMessage);

        if (lastFragment && checkSeqNums(messages.keySet()))
            processSnapshotsAndIncrementals(messages.values(), securityId, rptSeqNum);
    }

    @Override
    protected boolean checkSequence(Message readMessage) {
        int seqNum = readMessage.getInt("MsgSeqNum");
        long sendingTime = readMessage.getLong("SendingTime");

        if (seqNum == 1) {
            synchronized (this) {
                if (sendingTimeOfSnapshotStart < sendingTime) {
                    // new snapshot cycle
                    sendingTimeOfSnapshotStart = sendingTime;
                    reset();
                    printRecoveringSecurityIds();
                } else
                    return false;
            }
        } else {
            if (sequenceArray.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                return false;
        }

        String securityId = readMessage.getString("Symbol") + ":" + readMessage.getString("TradingSessionID");
        if (!sequenceValidator.isRecovering(securityId))
            return false;

        return true;
    }

    private void printRecoveringSecurityIds() {
        String [] recoveringSecurityIds = sequenceValidator.getRecovering();
        StringBuilder sb = new StringBuilder("Recovering ");
        if (recoveringSecurityIds != null && recoveringSecurityIds.length > 0) {
            wasRecovering = true;
            boolean first = true;
            for (String s : recoveringSecurityIds) {
                if (first)
                    first = false;
                else
                    sb.append(", ");
                sb.append(s);
            }

            getLogger().info(sb.toString());
        } else {
            if (wasRecovering) {
                getLogger().info("Finished Recovering");
                wasRecovering = false;
            }
        }
    }

    private void reset() {
        sequenceArray.clear();
        fragmentedSnapshots.clear();
    }
}
