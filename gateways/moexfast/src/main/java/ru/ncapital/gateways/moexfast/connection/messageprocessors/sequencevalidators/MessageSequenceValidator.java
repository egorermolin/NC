package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import com.google.inject.Inject;
import org.openfast.GroupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import java.util.*;

public class MessageSequenceValidator<T> implements IMessageSequenceValidator<T> {

    private MessageHandlerType type;

    protected ThreadLocal<Logger> logger = new ThreadLocal<Logger>() {
        @Override
        protected Logger initialValue() {
            return LoggerFactory.getLogger(Thread.currentThread().getName() + "-SequenceValidator");
        }
    };

    private Map<T, Map<Integer, StoredMdEntry<T>>> storedMdEntriesByExchangeSecurityId = new HashMap<>();

    private Map<T, StoredMdEntry<T>[]> storedMdEntriesToProcess = new HashMap<>();

    private Set<T> exchangeSecurityIdsToRecover = new HashSet<>();

    private Map<T, SequenceNumber<T>> sequenceNumbers = new HashMap<>();

    @Inject
    private MarketDataManager<T> marketDataManager;

    public MessageSequenceValidator(MessageHandlerType type) {
        this.type = type;
    }

    @Override
    public MessageHandlerType getType() {
        return type;
    }

    protected SequenceNumber getSequenceNumber(T exchangeSecurityId) {
        SequenceNumber<T> sequenceNumber = sequenceNumbers.get(exchangeSecurityId);
        if (sequenceNumber == null) {
            sequenceNumber = new SequenceNumber<>();
            sequenceNumber.exchangeSecurityId = exchangeSecurityId;

            sequenceNumbers.put(exchangeSecurityId, sequenceNumber);
        }
        return sequenceNumber;
    }

    @Override
    public boolean onSnapshotSeq(T exchangeSecurityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("SNAP -> " + exchangeSecurityId + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public boolean onIncrementalSeq(T exchangeSecurityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("INC -> " + exchangeSecurityId + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            if (sequenceNumber.lastSeqNum + 1 != seqNum) {
                if (sequenceNumber.lastSeqNum > 0 && !isRecovering(exchangeSecurityId, false)) {
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("OutOfSequence [Symbol: " + exchangeSecurityId + "][Expected: " + (sequenceNumber.lastSeqNum + 1) + "][Received: " + seqNum + "]");

                    sequenceNumber.numberOfMissingSequences = seqNum - sequenceNumber.lastSeqNum - 1;
                }

                return false;
            } else {
                if (isRecovering(exchangeSecurityId, false)) {
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("InSequence [Symbol: " + exchangeSecurityId + "][Received: " + seqNum + "]");

                    sequenceNumber.numberOfMissingSequences = 0;
                }
            }

            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public void storeIncremental(T exchangeSecurityId, int seqNum, GroupValue mdEntry, PerformanceData perfData) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("STORE -> " + exchangeSecurityId + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            if (!storedMdEntriesByExchangeSecurityId.containsKey(exchangeSecurityId)) {
                storedMdEntriesByExchangeSecurityId.put(exchangeSecurityId, new TreeMap<Integer, StoredMdEntry<T>>());
            }

            Map<Integer, StoredMdEntry<T>> storedMdEntries = storedMdEntriesByExchangeSecurityId.get(exchangeSecurityId);
            storedMdEntries.put(seqNum, new StoredMdEntry<>(exchangeSecurityId, seqNum, mdEntry, perfData));
        }
    }

    @Override
    public void startRecovering(T exchangeSecurityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            logger.get().info("Start Recovering " + exchangeSecurityId
                    + ((sequenceNumber.numberOfMissingSequences > 0) ? (" " + sequenceNumber.numberOfMissingSequences) : ""));
        }

        exchangeSecurityIdsToRecover.add(exchangeSecurityId);
        marketDataManager.setRecovery(exchangeSecurityId, true, type.equals("OrderList"));
    }

    @Override
    public StoredMdEntry<T>[] stopRecovering(T exchangeSecurityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            //check incrementals are in sequence
            Map<Integer, StoredMdEntry<T>> storedMdEntries = storedMdEntriesByExchangeSecurityId.get(exchangeSecurityId);
            if (storedMdEntries == null || storedMdEntries.size() == 0) {
                sequenceNumber.numberOfMissingSequences = 0;

                logger.get().info("Stop Recovering " + exchangeSecurityId);

                exchangeSecurityIdsToRecover.remove(exchangeSecurityId);
                marketDataManager.setRecovery(exchangeSecurityId, false, type.equals("OrderList"));

                return null;
            }

            List<StoredMdEntry<T>> mdEntriesToProcess = new ArrayList<>();
            int currentSeqNum = sequenceNumber.lastSeqNum;

            {
                for (int mdEntrySeqNum : storedMdEntries.keySet()) {
                    StoredMdEntry<T> mdEntry = storedMdEntries.get(mdEntrySeqNum);
                    if (mdEntrySeqNum > currentSeqNum) {
                        if (mdEntrySeqNum == currentSeqNum + 1) {
                            currentSeqNum = mdEntrySeqNum;
                            mdEntriesToProcess.add(mdEntry);
                        } else {
                            // out of sequence
                            return null;
                        }
                    }
                }

                storedMdEntriesToProcess.put(exchangeSecurityId, mdEntriesToProcess.toArray(new StoredMdEntry[mdEntriesToProcess.size()]));
                storedMdEntries.clear();
                mdEntriesToProcess.clear();
            }

            sequenceNumber.numberOfMissingSequences = 0;
        }

        exchangeSecurityIdsToRecover.remove(exchangeSecurityId);
        marketDataManager.setRecovery(exchangeSecurityId, false, type.equals("OrderList"));

        StoredMdEntry<T>[] mdEntriesToProcess = storedMdEntriesToProcess.remove(exchangeSecurityId);

        logger.get().info("Stop Recovering "
                + exchangeSecurityId + ((mdEntriesToProcess.length > 0) ? (" " + mdEntriesToProcess.length) : ""));

        return mdEntriesToProcess;
    }

    @Override
    public boolean isRecovering(T exchangeSecurityId, boolean isSnapshot) {
        if (exchangeSecurityIdsToRecover.contains(exchangeSecurityId))
            return true;

        if (isSnapshot) {
            SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
            synchronized (sequenceNumber) {
                if (sequenceNumber.lastSeqNum == -1) {
                    startRecovering(exchangeSecurityId);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isRecovering() {
        return !exchangeSecurityIdsToRecover.isEmpty();
    }

    @Override
    public T[] getRecovering() {
        return (T[]) exchangeSecurityIdsToRecover.toArray();
    }

    public void setMarketDataManager(MarketDataManager<T> marketDataManager) {
        this.marketDataManager = marketDataManager;
    }
}
