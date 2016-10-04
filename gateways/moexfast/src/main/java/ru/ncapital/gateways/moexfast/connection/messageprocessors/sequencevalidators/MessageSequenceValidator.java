package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import com.google.inject.Inject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

import java.util.*;

public class MessageSequenceValidator implements IMessageSequenceValidator {

    private MessageHandlerType type;

    protected ThreadLocal<Logger> logger = new ThreadLocal<Logger>() {
        @Override
        protected Logger initialValue() {
            return LoggerFactory.getLogger(Thread.currentThread().getName() + "-SequenceValidator");
        }
    };

    private Map<String, Map<Integer, StoredMdEntry>> storedMdEntriesBySecurityId = new HashMap<>();

    private Map<String, StoredMdEntry[]> storedMdEntriesToProcess = new HashMap<>();

    private Set<String> securityIdsToRecover = new HashSet<>();

    protected Map<String, SequenceNumber> sequenceNumbers = new HashMap<>();

    @Inject
    private MarketDataManager marketDataManager;

    protected MessageSequenceValidator(MessageHandlerType type) {
        this.type = type;
    }

    @Override
    public MessageHandlerType getType() {
        return type;
    }

    private SequenceNumber getSequenceNumber(String securityId) {
        SequenceNumber sequenceNumber = sequenceNumbers.get(securityId);
        if (sequenceNumber == null) {
            sequenceNumber = new SequenceNumber();
            sequenceNumber.securityId = securityId;

            sequenceNumbers.put(securityId, sequenceNumber);
        }
        return sequenceNumber;
    }

    @Override
    public boolean onSnapshotSeq(String securityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("SNAP -> " + securityId + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(securityId);

        synchronized (sequenceNumber) {
            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public boolean onIncrementalSeq(String securityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("INC -> " + securityId + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(securityId);

        synchronized (sequenceNumber) {
            if (sequenceNumber.lastSeqNum + 1 != seqNum) {
                if (sequenceNumber.lastSeqNum > 0 && !isRecovering(securityId, false)) {
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("OutOfSequence [Symbol: " + securityId + "][Expected: " + (sequenceNumber.lastSeqNum + 1) + "][Received: " + seqNum + "]");

                    sequenceNumber.numberOfMissingSequences = seqNum - sequenceNumber.lastSeqNum - 1;
                }

                return false;
            } else {
                if (isRecovering(securityId, false)) {
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("InSequence [Symbol: " + securityId + "][Received: " + seqNum + "]");

                    sequenceNumber.numberOfMissingSequences = 0;
                }
            }

            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public void storeIncremental(String securityId, int seqNum, GroupValue mdEntry, PerformanceData perfData) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("STORE -> " + securityId + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(securityId);

        synchronized (sequenceNumber) {
            if (!storedMdEntriesBySecurityId.containsKey(securityId)) {
                storedMdEntriesBySecurityId.put(securityId, new TreeMap<Integer, StoredMdEntry>());
            }

            Map<Integer, StoredMdEntry> storedMdEntries = storedMdEntriesBySecurityId.get(securityId);

            storedMdEntries.put(seqNum, new StoredMdEntry(securityId, seqNum, mdEntry, perfData));
        }
    }

    @Override
    public void startRecovering(String securityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(securityId);
        synchronized (sequenceNumber) {
            logger.get().info("Start Recovering " + securityId
                    + ((sequenceNumber.numberOfMissingSequences > 0) ? (" " + sequenceNumber.numberOfMissingSequences) : ""));
        }

        securityIdsToRecover.add(securityId);
        marketDataManager.setRecovery(securityId, true, type.equals("OrderList"));
    }

    @Override
    public StoredMdEntry[] stopRecovering(String securityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(securityId);
        synchronized (sequenceNumber) {
            //check incrementals are in sequence
            Map<Integer, StoredMdEntry> storedMdEntries = storedMdEntriesBySecurityId.get(securityId);
            if (storedMdEntries == null || storedMdEntries.size() == 0) {
                sequenceNumber.numberOfMissingSequences = 0;

                logger.get().info("Stop Recovering " + securityId);

                securityIdsToRecover.remove(securityId);
                marketDataManager.setRecovery(securityId, false, type.equals("OrderList"));

                return null;
            }

            List<StoredMdEntry> mdEntriesToProcess = new ArrayList<>();
            int currentSeqNum = sequenceNumber.lastSeqNum;

            {
                for (int mdEntrySeqNum : storedMdEntries.keySet()) {
                    StoredMdEntry mdEntry = storedMdEntries.get(mdEntrySeqNum);
                    if (mdEntrySeqNum <= currentSeqNum)
                        continue;
                    else {
                        if (mdEntrySeqNum == currentSeqNum + 1) {
                            currentSeqNum = mdEntrySeqNum;
                            mdEntriesToProcess.add(mdEntry);
                        } else {
                            // out of sequence
                            return null;
                        }
                    }
                }

                storedMdEntriesToProcess.put(securityId, mdEntriesToProcess.toArray(new StoredMdEntry[mdEntriesToProcess.size()]));
                storedMdEntries.clear();
                mdEntriesToProcess.clear();
            }

            sequenceNumber.numberOfMissingSequences = 0;
        }

        securityIdsToRecover.remove(securityId);
        marketDataManager.setRecovery(securityId, false, type.equals("OrderList"));

        StoredMdEntry[] mdEntriesToProcess = storedMdEntriesToProcess.remove(securityId);

        logger.get().info("Stop Recovering "
                + securityId + ((mdEntriesToProcess.length > 0) ? (" " + mdEntriesToProcess.length) : ""));

        return mdEntriesToProcess;
    }

    @Override
    public boolean isRecovering(String securityId, boolean isSnapshot) {
        if (securityIdsToRecover.contains(securityId))
            return true;

        if (isSnapshot) {
            SequenceNumber sequenceNumber = getSequenceNumber(securityId);
            synchronized (sequenceNumber) {
                if (sequenceNumber.lastSeqNum == -1) {
                    startRecovering(securityId);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isRecovering() {
        return !securityIdsToRecover.isEmpty();
    }

    @Override
    public String[] getRecovering() { return securityIdsToRecover.toArray(new String[securityIdsToRecover.size()]);}
}