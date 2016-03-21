package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import org.openfast.Context;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.MessageHandler;
import org.openfast.codec.Coder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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

    private void processSnapshotsAndIncrementals(Iterable<Message> messages, String symbol, int rptSeqNum) {
        synchronized (sequenceValidator) {
            if (sequenceValidator.onSnapshotSeq(symbol, rptSeqNum)) {
                for (Message message : messages)
                    messageHandler.onSnapshot(message, getInTimestamp());

                // finished recovering
                GroupValue[] mdEntriesToProcess = sequenceValidator.stopRecovering(symbol);
                if (mdEntriesToProcess != null)
                    for (GroupValue mdEntry : mdEntriesToProcess) {
                        sequenceValidator.onIncrementalSeq(symbol, mdEntry.getInt("RptSeq"));

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
        String symbol = readMessage.getString("Symbol") + ":" + readMessage.getString("TradingSessionID");
        int rptSeqNum = readMessage.getInt("RptSeq");
        boolean firstFragment = readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getInt("LastFragment") == 1;

        if (firstFragment)
            fragmentedSnapshots.put(symbol, Collections.synchronizedMap(new TreeMap<Integer, Message>()));

        Map<Integer, Message> messages = fragmentedSnapshots.get(symbol);
        if (messages == null)
            return;

        messages.put(seqNum, readMessage);

        if (lastFragment && checkSeqNums(messages.keySet()))
            processSnapshotsAndIncrementals(messages.values(), symbol, rptSeqNum);
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
                    printRecoveringSymbols();
                } else
                    return false;
            }
        } else {
            if (sequenceArray.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                return false;
        }

        String symbol = readMessage.getString("Symbol") + ":" + readMessage.getString("TradingSessionID");
        if (!sequenceValidator.isRecovering(symbol))
            return false;

        return true;
    }

    private void printRecoveringSymbols() {
        String [] recoveringSymbols = sequenceValidator.getRecovering();
        StringBuilder sb = new StringBuilder("Recovering ");
        if (recoveringSymbols != null && recoveringSymbols.length > 0) {
            wasRecovering = true;
            boolean first = true;
            for (String s : recoveringSymbols) {
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
