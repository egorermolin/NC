package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Message;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import java.util.*;

/**
 * Created by egore on 1/11/16.
 */
public abstract class SnapshotProcessor<T> extends Processor<T> implements ISnapshotProcessor {

    private Map<T, Map<Integer, Message>> fragmentedSnapshots = new HashMap<>();

    private long sendingTimeOfSnapshotStart = 0;

    private boolean wasRecovering;

    public SnapshotProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    private void processSnapshotsAndIncrementals(T exchangeSecurityId, int rptSeqNum, Collection<Message> messages) {
        synchronized (sequenceValidator) {
            if (sequenceValidator.onSnapshotSeq(exchangeSecurityId, rptSeqNum)) {
                for (Message message : messages)
                    messageHandler.onSnapshot(message);

                // finished recovering
                StoredMdEntry[] storedMdEntriesToProcess = sequenceValidator.stopRecovering(exchangeSecurityId);
                if (storedMdEntriesToProcess != null) {
                    for (StoredMdEntry storedMdEntry : storedMdEntriesToProcess) {
                        sequenceValidator.onIncrementalSeq(exchangeSecurityId, storedMdEntry.getSequenceNumber());

                        messageHandler.onIncremental(storedMdEntry.getMdEntry(), new PerformanceData());
                    }
                    messageHandler.flushIncrementals();
                }
            }
        }
    }

    private boolean checkMessages(Map<Integer, Message> messages) {
        // check seqNum integrity
        int lastSeqNum = 0;
        for (int seqNum : messages.keySet()) {
            if (lastSeqNum == 0) {
                lastSeqNum = seqNum;
                if (messages.get(seqNum).getInt("RouteFirst") != 1)
                    // missing first fragment
                    return false;
            } else if (lastSeqNum + 1 < seqNum) {
                // missing message, giving up
                return false;
            } else {
                lastSeqNum = seqNum;
            }
        }
        if (messages.get(lastSeqNum).getInt("LastFragment") != 1)
            // missing last fragment
            return false;

        return true;
    }

    @Override
    public void processMessage(Message readMessage) {
        int seqNum = readMessage.getInt("MsgSeqNum");
        T exchangeSecurityId = getExchangeSecurityId(readMessage);

        int rptSeqNum = readMessage.getInt("RptSeq");
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (firstFragment)
            fragmentedSnapshots.put(exchangeSecurityId, Collections.synchronizedMap(new TreeMap<Integer, Message>()));

        Map<Integer, Message> messages = fragmentedSnapshots.get(exchangeSecurityId);
        if (messages == null)
            return;

        messages.put(seqNum, readMessage);

        if (lastFragment) {
            if (checkMessages(messages)) {
                processSnapshotsAndIncrementals(exchangeSecurityId, rptSeqNum, messages.values());
            } else {
                fragmentedSnapshots.remove(exchangeSecurityId);
            }
        }
    }

    @Override
    protected boolean checkSequence(Message readMessage) {
        T exchangeSecurityId = getExchangeSecurityId(readMessage);
        int seqNum = readMessage.getInt("MsgSeqNum");
        long sendingTime = readMessage.getLong("SendingTime");

        if (seqNum == 1) {
            synchronized (this) {
                if (sendingTimeOfSnapshotStart < sendingTime) {
                    // new snapshot cycle
                    sendingTimeOfSnapshotStart = sendingTime;
                    reset();
                } else
                    return false;
            }
        } else {
            if (sequenceArray.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                return false;
        }

        if (!messageHandler.isAllowedUpdate(exchangeSecurityId))
            return false;

        if (!sequenceValidator.isRecovering(exchangeSecurityId, true))
            return false;

        return true;
    }

    private void printRecoveringSecurityIds() {
        T[] recoveringSecurityIds = sequenceValidator.getRecovering();
        StringBuilder sb = new StringBuilder("Recovering ");
        if (recoveringSecurityIds != null && recoveringSecurityIds.length > 0) {
            wasRecovering = true;
            boolean first = true;
            for (T s : recoveringSecurityIds) {
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

    @Override
    public void reset() {
        sequenceArray.clear();
        fragmentedSnapshots.clear();
        if (sequenceValidator.isRecovering())
            printRecoveringSecurityIds();
    }

    protected abstract T getExchangeSecurityId(Message readMessage);
}
