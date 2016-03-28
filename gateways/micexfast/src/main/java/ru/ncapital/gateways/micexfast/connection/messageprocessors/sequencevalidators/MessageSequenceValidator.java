package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import com.google.inject.Inject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.domain.BBO;

import java.util.*;

/**
 * Created by egore on 12/28/15.
 */

class SequenceNumber {
    String securityId;

    int lastSeqNum;
}

public class MessageSequenceValidator implements IMessageSequenceValidator {
    protected String type;

    protected ThreadLocal<Logger> logger = new ThreadLocal<Logger>() {
        @Override
        protected Logger initialValue() {
            return LoggerFactory.getLogger(Thread.currentThread().getName() + "-SequenceValidator");
        }
    };

    private Map<String, Map<Integer, GroupValue>> storedMdEntriesBySecurityId = new HashMap<String, Map<Integer, GroupValue>>();

    private Map<String, GroupValue[]> storedMdEntriesToProcess = new HashMap<String, GroupValue[]>();

    private Set<String> securityIdsToRecover = new HashSet<String>();

    protected Map<String, SequenceNumber> sequenceNumbers = new HashMap<String, SequenceNumber>();

    @Inject
    private MarketDataManager marketDataManager;

    protected MessageSequenceValidator(String type) {
        this.type = type;
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
                if (sequenceNumber.lastSeqNum > 0 && !isRecovering(securityId))
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("OutOfSequence [Symbol: " + securityId + "][Expected: " + (sequenceNumber.lastSeqNum + 1) + "][Received: " + seqNum + "]");

                return false;
            } else {
                if (isRecovering(securityId))
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("InSequence [Symbol: " + securityId + "][Received: " + seqNum + "]");
            }

            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public void storeIncremental(GroupValue mdEntry, String securityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("STORE -> " + securityId + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(securityId);

        synchronized (sequenceNumber) {
            if (!storedMdEntriesBySecurityId.containsKey(securityId)) {
                storedMdEntriesBySecurityId.put(securityId, new TreeMap<Integer, GroupValue>());
            }

            Map<Integer, GroupValue> storedMdEntries = storedMdEntriesBySecurityId.get(securityId);

            storedMdEntries.put(seqNum, mdEntry);
        }
    }

    @Override
    public void startRecovering(String securityId) {
        logger.get().info("Start Recovering " + securityId);

        securityIdsToRecover.add(securityId);
        marketDataManager.onBBO(new BBO(securityId.contains(":") ? securityId.substring(0, securityId.indexOf(':')) : securityId) {
            {
                setInRecovery(true, type.equals("OrderList") ? 0 : 1);
            }
        }, 0);
    }

    @Override
    public GroupValue[] stopRecovering(String securityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(securityId);
        synchronized (sequenceNumber) {
            //check incrementals are in sequence
            Map<Integer, GroupValue> storedMdEntries = storedMdEntriesBySecurityId.get(securityId);
            if (storedMdEntries == null || storedMdEntries.size() == 0) {
                logger.get().info("Stop Recovering " + securityId);

                securityIdsToRecover.remove(securityId);
                marketDataManager.onBBO(new BBO(securityId.contains(":") ? securityId.substring(0, securityId.indexOf(':')) : securityId) {
                    {
                        setInRecovery(false, type.equals("OrderList") ? 0 : 1);
                    }
                }, 0);
                return null;
            }

            List<GroupValue> mdEntriesToProcess = new ArrayList<GroupValue>();
            int currentSeqNum = sequenceNumber.lastSeqNum;

            {
                for (int mdEntrySeqNum : storedMdEntries.keySet()) {
                    GroupValue mdEntry = storedMdEntries.get(mdEntrySeqNum);
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

                storedMdEntriesToProcess.put(securityId, mdEntriesToProcess.toArray(new GroupValue[mdEntriesToProcess.size()]));
                storedMdEntries.clear();
                mdEntriesToProcess.clear();
            }
        }

        logger.get().info("Stop Recovering " + securityId);

        securityIdsToRecover.remove(securityId);
        marketDataManager.onBBO(new BBO(securityId.contains(":") ? securityId.substring(0, securityId.indexOf(':')) : securityId) {
            {
                setInRecovery(false, type.equals("OrderList") ? 0 : 1);
            }
        }, 0);

        return storedMdEntriesToProcess.remove(securityId);
    }

    @Override
    public boolean isRecovering(String securityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(securityId);

        synchronized (sequenceNumber) {
            if (sequenceNumber.lastSeqNum == 0)
                return true;
        }

        return securityIdsToRecover.contains(securityId);
    }

    @Override
    public String[] getRecovering() { return securityIdsToRecover.toArray(new String[securityIdsToRecover.size()]);}
}
