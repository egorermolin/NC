package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Message;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import java.util.*;

/**
 * Created by egore on 1/11/16.
 */
public abstract class SnapshotProcessor<T> extends Processor implements ISnapshotProcessor<T> {

    private Map<T, Map<Integer, Message>> fragmentedSnapshots = new HashMap<>();

    private Set<Integer> receivedSnapshots = new TreeSet<>();

    private int allSnapshotsReceived = -1;

    private long timeOfLastSequenceReset = 0;

    private IMessageHandler<T> messageHandler;

    private final IMessageSequenceValidator<T> sequenceValidator;

    public SnapshotProcessor(IMessageHandler<T> messageHandler, IMessageSequenceValidator<T> sequenceValidator) {
        this.messageHandler = messageHandler;
        this.sequenceValidator = sequenceValidator;
    }

    private void processSnapshotsAndIncrementals(T exchangeSecurityId, int rptSeqNum, Collection<Message> messages) {
        synchronized (sequenceValidator) {
            if (sequenceValidator.onSnapshotSeq(exchangeSecurityId, rptSeqNum)) {
                for (Message message : messages)
                    messageHandler.onSnapshot(message);

                // finished recovering
                StoredMdEntry[] storedMdEntriesToProcess = sequenceValidator.stopRecovering(exchangeSecurityId);
                if (storedMdEntriesToProcess != null) {
                    boolean lastFragment = true;

                    for (StoredMdEntry storedMdEntry : storedMdEntriesToProcess) {
                        lastFragment = storedMdEntry.isLastFragment();

                        sequenceValidator.onIncrementalSeq(exchangeSecurityId, storedMdEntry.getSequenceNumber());

                        messageHandler.onIncremental(storedMdEntry.getMdEntry(), new PerformanceData());

                        if (storedMdEntry.isLastEntryInTransaction())
                            messageHandler.flushIncrementals();
                    }

                    if (lastFragment)
                        messageHandler.flushIncrementals();
                }
            }
        }
    }

    private boolean checkMessages(T exchangeSecurityId, Map<Integer, Message> messages) {
        // check seqNum integrity
        int lastSeqNum = 0;
        for (int seqNum : messages.keySet()) {
            if (lastSeqNum == 0) {
                lastSeqNum = seqNum;
                if (messages.get(seqNum).getValue("RouteFirst") != null
                        && messages.get(seqNum).getInt("RouteFirst") != 1) {

                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Missing snapshot [SecurityId: " + sequenceValidator.convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][FIRST]");

                    return false;
                }
            } else if (lastSeqNum + 1 < seqNum) {
                if (getLogger().isDebugEnabled())
                    getLogger().debug("Missing snapshot [SecurityId: " + sequenceValidator.convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][Expected: " + (lastSeqNum + 1) + "][Received: " + seqNum + "]");

                return false;
            } else {
                lastSeqNum = seqNum;
            }
        }

        if (messages.get(lastSeqNum).getValue("LastFragment") != null
                && messages.get(lastSeqNum).getInt("LastFragment") != 1) {

            if (getLogger().isDebugEnabled())
                getLogger().debug("Missing snapshot [SecurityId: " + sequenceValidator.convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][LAST]");

            return false;
        }

        return true;
    }

    @Override
    public void processMessage(Message readMessage) {
        int seqNum = readMessage.getInt("MsgSeqNum");
        T exchangeSecurityId = getExchangeSecurityId(readMessage);

        int rptSeqNum = readMessage.getInt("RptSeq");
        boolean firstFragment = readMessage.getValue("RouteFirst") == null || readMessage.getInt("RouteFirst") == 1;
        boolean lastFragment = readMessage.getValue("LastFragment") == null || readMessage.getInt("LastFragment") == 1;

        if (getLogger().isDebugEnabled())
            getLogger().debug("Received snapshot [SecurityId: " + sequenceValidator.convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][MsgSeqNum: " + seqNum + "][RptSeqNum: " + rptSeqNum + "]" + (firstFragment ? "[FIRST]" : "") + (lastFragment ? "[LAST]" : ""));

        if (firstFragment)
            fragmentedSnapshots.put(exchangeSecurityId, Collections.synchronizedMap(new TreeMap<Integer, Message>()));

        Map<Integer, Message> messages = fragmentedSnapshots.get(exchangeSecurityId);
        if (messages == null)
            return;

        messages.put(seqNum, readMessage);

        if (lastFragment) {
            if (checkMessages(exchangeSecurityId, messages)) {
                processSnapshotsAndIncrementals(exchangeSecurityId, rptSeqNum, messages.values());
            } else {
                fragmentedSnapshots.remove(exchangeSecurityId);
            }
        }
    }

    private synchronized boolean resetSequence(long sendingTime) {
        if (timeOfLastSequenceReset < sendingTime) {
            if (getLogger().isDebugEnabled())
                getLogger().debug("Reset Snapshot");

            // new snapshot cycle
            timeOfLastSequenceReset = sendingTime;
            reset();
        } else
            return false;

        return true;
    }

    @Override
    protected boolean checkSequence(Message readMessage) {
        if (getLogger().isTraceEnabled())
            getLogger().trace(readMessage.toString());

        int seqNum = readMessage.getInt("MsgSeqNum");
        long sendingTime = readMessage.getLong("SendingTime");
        char messageType = readMessage.getString("MessageType").charAt(0);

        if (seqNum == 1) { // SequenceReset
            if (!resetSequence(sendingTime))
                return false;
        } else {
            if (sequenceArray.checkSequence(seqNum) == SequenceArray.Result.DUPLICATE)
                return false;
        }

        receivedSnapshots.add(seqNum);

        if (messageType == 'W') {
            T exchangeSecurityId = getExchangeSecurityId(readMessage);
            int rptSeqNum = readMessage.getInt("RptSeq");
            return messageHandler.isAllowedUpdate(exchangeSecurityId) && sequenceValidator.isRecovering(exchangeSecurityId, rptSeqNum, true);
        }

        if (messageType == '4') {
            int lastSeqNum = 0;
            for (int receivedSeqNum : receivedSnapshots) {
                if (receivedSeqNum == lastSeqNum + 1)
                    lastSeqNum = receivedSeqNum;
                else {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("OutOfSequence on SequenceReset [Expected: " + (lastSeqNum + 1) + "][Received: " + receivedSeqNum + "]");

                    break;
                }
            }

            if (lastSeqNum == seqNum) {
                if (allSnapshotsReceived == 0)
                    allSnapshotsReceived = 1;
            }
        }

        return false;
    }

    private void printRecoveringSecurityIds(int stopRecovering) {
        List<T> recoveringExchangeSecurityIds = sequenceValidator.getRecovering();
        boolean hasRecoveringExchangeSecurityIds = recoveringExchangeSecurityIds.size() > 0;
        if (hasRecoveringExchangeSecurityIds) {
            StringBuilder sb = new StringBuilder();
            if (stopRecovering > 0) {
                sb.append("Stopped Recovering for instruments which did not receive snapshot");
                for (T recoveringExchangeSecurityId : recoveringExchangeSecurityIds) {
                    sequenceValidator.stopRecovering(recoveringExchangeSecurityId);
                }
            } else {
                sb.append("Continue Recovering for instruments which did not receive snapshot");
                for (T recoveringExchangeSecurityId : recoveringExchangeSecurityIds) {
                    sb.append(" ").append(sequenceValidator.convertExchangeSecurityIdToSecurityId(recoveringExchangeSecurityId));
                }
            }
            getLogger().info(sb.toString());
        }

        if (hasRecoveringExchangeSecurityIds && sequenceValidator.getRecovering().isEmpty())
            getLogger().info("Finished Recovering");
    }

    @Override
    public void start() {
        allSnapshotsReceived = 0;
    }

    @Override
    public void reset() {
        sequenceArray.clear();
        fragmentedSnapshots.clear();

        if (sequenceValidator.isRecovering())
            printRecoveringSecurityIds(allSnapshotsReceived);

        if (allSnapshotsReceived == 1)
            allSnapshotsReceived = 0;
    }

    @Override
    public IMessageSequenceValidator<T> getSequenceValidator() {
        return sequenceValidator;
    }

    protected abstract T getExchangeSecurityId(Message readMessage);
}
