package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.MicexInstrument;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import java.util.*;

/**
 * Created by egore on 1/11/16.
 */
public class SnapshotProcessor extends Processor implements ISnapshotProcessor {

    private Map<String, Map<Integer, Message>> fragmentedSnapshots = new HashMap<String, Map<Integer, Message>>();

    private long sendingTimeOfSnapshotStart = 0;

    private boolean wasRecovering;

    public SnapshotProcessor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        super(messageHandler, sequenceValidator);
    }

    private void processSnapshotsAndIncrementals(String securityId, int rptSeqNum, Collection<Message> messages) {
        synchronized (sequenceValidator) {
            if (sequenceValidator.onSnapshotSeq(securityId, rptSeqNum)) {
                for (Message message : messages)
                    messageHandler.onSnapshot(message);

                // finished recovering
                StoredMdEntry[] storedMdEntriesToProcess = sequenceValidator.stopRecovering(securityId);
                if (storedMdEntriesToProcess != null) {
                    for (StoredMdEntry storedMdEntry : storedMdEntriesToProcess) {
                        sequenceValidator.onIncrementalSeq(securityId, storedMdEntry.getSequenceNumber());

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
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");
        String securityId = symbol + MicexInstrument.BOARD_SEPARATOR + tradingSessionId;

        int rptSeqNum = readMessage.getInt("RptSeq");
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (firstFragment)
            fragmentedSnapshots.put(securityId, Collections.synchronizedMap(new TreeMap<Integer, Message>()));

        Map<Integer, Message> messages = fragmentedSnapshots.get(securityId);
        if (messages == null)
            return;

        messages.put(seqNum, readMessage);

        if (lastFragment) {
            if (checkMessages(messages)) {
                processSnapshotsAndIncrementals(securityId, rptSeqNum, messages.values());
            } else {
                fragmentedSnapshots.remove(securityId);
            }
        }
    }

    @Override
    protected boolean checkSequence(Message readMessage) {
        String symbol = readMessage.getString("Symbol");
        String tradingSessionId = readMessage.getString("TradingSessionID");
        String securityId = symbol + MicexInstrument.BOARD_SEPARATOR + tradingSessionId;
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

        if (!messageHandler.isAllowedUpdate(symbol, tradingSessionId))
            return false;

        if (!sequenceValidator.isRecovering(securityId, true))
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

    @Override
    public void reset() {
        sequenceArray.clear();
        fragmentedSnapshots.clear();
        if (sequenceValidator.isRecovering())
            printRecoveringSecurityIds();
    }
}
