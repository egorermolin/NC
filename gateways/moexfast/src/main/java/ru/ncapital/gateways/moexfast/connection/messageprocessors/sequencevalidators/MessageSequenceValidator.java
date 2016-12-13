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
            return LoggerFactory.getLogger(Thread.currentThread().getName() + "-" + type + "-SequenceValidator");
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

    @Override
    public void onMarketReset() {
        for (SequenceNumber<T> sequenceNumber : sequenceNumbers.values()) {
            synchronized (sequenceNumber) {
                sequenceNumber.lastSeqNum = 0;
            }
        }
    }

    @Override
    public String convertExchangeSecurityIdToSecurityId(T exchangeSecurityId) {
        return marketDataManager.convertExchangeSecurityIdToSecurityId(exchangeSecurityId);
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
            logger.get().trace("SNAP -> " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public boolean onIncrementalSeq(T exchangeSecurityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("INC -> " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            if (sequenceNumber.lastSeqNum + 1 != seqNum) {
                if (isRecovering(exchangeSecurityId, seqNum, false))
                    return false;

                if (logger.get().isDebugEnabled())
                    logger.get().debug("OutOfSequence [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][Expected: " + (sequenceNumber.lastSeqNum + 1) + "][Received: " + seqNum + "]");

                sequenceNumber.numberOfMissingSequences = seqNum - sequenceNumber.lastSeqNum - 1;
                return false;
            } else {
                if (isRecovering(exchangeSecurityId, seqNum, false)) {
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("InSequence [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][Received: " + seqNum + "]");

                    sequenceNumber.numberOfMissingSequences = 0;
                }
            }

            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public void storeIncremental(T exchangeSecurityId, int seqNum, GroupValue mdEntry, PerformanceData perfData, boolean lastFragment, boolean lastEntryInTransaction) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("STORE -> " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            if (!storedMdEntriesByExchangeSecurityId.containsKey(exchangeSecurityId)) {
                storedMdEntriesByExchangeSecurityId.put(exchangeSecurityId, new TreeMap<Integer, StoredMdEntry<T>>());
            }

            Map<Integer, StoredMdEntry<T>> storedMdEntries = storedMdEntriesByExchangeSecurityId.get(exchangeSecurityId);
            storedMdEntries.put(seqNum, new StoredMdEntry<>(exchangeSecurityId, seqNum, mdEntry, perfData, lastFragment, lastEntryInTransaction));
        }
    }

    @Override
    public void startRecovering(T exchangeSecurityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            if (sequenceNumber.numberOfMissingSequences > 0) {
                logger.get().info("Start Recovering [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][Gap: " + sequenceNumber.numberOfMissingSequences + "]");
            } else if (sequenceNumber.numberOfMissingSequences == 0) {
                logger.get().info("Start Recovering [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][FIRST]");
            } else {
                logger.get().info("Start Recovering [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][RESET]");
            }
        }

        exchangeSecurityIdsToRecover.add(exchangeSecurityId);
        marketDataManager.setRecovery(exchangeSecurityId, true, getType().convert());
    }

    @Override
    public StoredMdEntry<T>[] stopRecovering(T exchangeSecurityId) {
        SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
        synchronized (sequenceNumber) {
            //check incremental messages are in sequence
            Map<Integer, StoredMdEntry<T>> storedMdEntries = storedMdEntriesByExchangeSecurityId.get(exchangeSecurityId);
            if (storedMdEntries == null || storedMdEntries.size() == 0) {
                sequenceNumber.numberOfMissingSequences = 0;

                logger.get().info("Stop Recovering [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "]");

                exchangeSecurityIdsToRecover.remove(exchangeSecurityId);
                marketDataManager.setRecovery(exchangeSecurityId, false, getType().convert());

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
                            if (logger.get().isDebugEnabled())
                                logger.get().debug("OutOfSequence on Stop Recovering [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][Expected: " + (currentSeqNum + 1) + "][Received: " + mdEntrySeqNum + "]");

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
        marketDataManager.setRecovery(exchangeSecurityId, false, getType().convert());

        StoredMdEntry<T>[] mdEntriesToProcess = storedMdEntriesToProcess.remove(exchangeSecurityId);
        logger.get().info("Stop Recovering [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][Queue: " + mdEntriesToProcess.length + "]");

        return mdEntriesToProcess;
    }

    @Override
    public boolean isRecovering(T exchangeSecurityId, int seqNum, boolean isSnapshot) {
        if (exchangeSecurityIdsToRecover.contains(exchangeSecurityId))
            return true;

        if (isSnapshot) {
            SequenceNumber sequenceNumber = getSequenceNumber(exchangeSecurityId);
            synchronized (sequenceNumber) {
                if (sequenceNumber.lastSeqNum < seqNum) {
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("OutOfSequence [SecurityId: " + convertExchangeSecurityIdToSecurityId(exchangeSecurityId) + "][Expected: " + sequenceNumber.lastSeqNum + "][Received: " + seqNum + "]");

                    sequenceNumber.numberOfMissingSequences = seqNum - sequenceNumber.lastSeqNum;
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
    public List<String> getRecovering() {
        List<String> securityIdsToRecover = new ArrayList<>();
        for (T exchangeSecurityId : exchangeSecurityIdsToRecover)
            securityIdsToRecover.add(convertExchangeSecurityIdToSecurityId(exchangeSecurityId));
        return securityIdsToRecover;
    }

    public void setMarketDataManager(MarketDataManager<T> marketDataManager) {
        this.marketDataManager = marketDataManager;
    }
}
