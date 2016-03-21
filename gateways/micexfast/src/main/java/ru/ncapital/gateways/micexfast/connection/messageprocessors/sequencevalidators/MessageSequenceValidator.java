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
    String symbol;

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

    private Map<String, Map<Integer, GroupValue>> storedMdEntriesBySymbol = new HashMap<String, Map<Integer, GroupValue>>();

    private Map<String, GroupValue[]> storedMdEntriesToProcess = new HashMap<String, GroupValue[]>();

    private Set<String> symbolsToRecover = new HashSet<String>();

    protected Map<String, SequenceNumber> sequenceNumbers = new HashMap<String, SequenceNumber>();

    @Inject
    private MarketDataManager marketDataManager;

    protected MessageSequenceValidator(String type) {
        this.type = type;
    }

    private SequenceNumber getSequenceNumber(String symbol) {
        SequenceNumber sequenceNumber = sequenceNumbers.get(symbol);
        if (sequenceNumber == null) {
            sequenceNumber = new SequenceNumber();
            sequenceNumber.symbol = symbol;

            sequenceNumbers.put(symbol, sequenceNumber);
        }
        return sequenceNumber;
    }

    @Override
    public boolean onSnapshotSeq(String symbol, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("SNAP -> " + symbol + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(symbol);

        synchronized (sequenceNumber) {
            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public boolean onIncrementalSeq(String symbol, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("INC -> " + symbol + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(symbol);

        synchronized (sequenceNumber) {
            if (sequenceNumber.lastSeqNum + 1 != seqNum) {
                if (sequenceNumber.lastSeqNum > 0 && !isRecovering(symbol))
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("OutOfSequence [Symbol: " + symbol + "][Expected: " + (sequenceNumber.lastSeqNum + 1) + "][Received: " + seqNum + "]");

                return false;
            } else {
                if (isRecovering(symbol))
                    if (logger.get().isDebugEnabled())
                        logger.get().debug("InSequence [Symbol: " + symbol + "][Received: " + seqNum + "]");
            }

            sequenceNumber.lastSeqNum = seqNum;
        }

        return true;
    }

    @Override
    public void storeIncremental(GroupValue mdEntry, String symbol, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("STORE -> " + symbol + " " + seqNum);

        SequenceNumber sequenceNumber = getSequenceNumber(symbol);

        synchronized (sequenceNumber) {
            if (!storedMdEntriesBySymbol.containsKey(symbol)) {
                storedMdEntriesBySymbol.put(symbol, new TreeMap<Integer, GroupValue>());
            }

            Map<Integer, GroupValue> storedMdEntries = storedMdEntriesBySymbol.get(symbol);

            storedMdEntries.put(seqNum, mdEntry);
        }
    }

    @Override
    public void startRecovering(String symbol) {
        logger.get().info("Start Recovering " + symbol);

        symbolsToRecover.add(symbol);
        marketDataManager.onBBO(new BBO(symbol.contains(":") ? symbol.substring(0, symbol.indexOf(':')) : symbol) {
            {
                setInRecovery(true, type.equals("OrderList") ? 0 : 1);
            }
        }, 0);
    }

    @Override
    public GroupValue[] stopRecovering(String symbol) {
        SequenceNumber sequenceNumber = getSequenceNumber(symbol);
        synchronized (sequenceNumber) {
            //check incrementals are in sequence
            Map<Integer, GroupValue> storedMdEntries = storedMdEntriesBySymbol.get(symbol);
            if (storedMdEntries == null || storedMdEntries.size() == 0) {
                logger.get().info("Stop Recovering " + symbol);

                symbolsToRecover.remove(symbol);
                marketDataManager.onBBO(new BBO(symbol.contains(":") ? symbol.substring(0, symbol.indexOf(':')) : symbol) {
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

                storedMdEntriesToProcess.put(symbol, mdEntriesToProcess.toArray(new GroupValue[mdEntriesToProcess.size()]));
                storedMdEntries.clear();
                mdEntriesToProcess.clear();
            }
        }

        logger.get().info("Stop Recovering " + symbol);

        symbolsToRecover.remove(symbol);
        marketDataManager.onBBO(new BBO(symbol.contains(":") ? symbol.substring(0, symbol.indexOf(':')) : symbol) {
            {
                setInRecovery(false, type.equals("OrderList") ? 0 : 1);
            }
        }, 0);

        return storedMdEntriesToProcess.remove(symbol);
    }

    @Override
    public boolean isRecovering(String symbol) {
        return symbolsToRecover.contains(symbol);
    }

    @Override
    public String[] getRecovering() { return symbolsToRecover.toArray(new String[symbolsToRecover.size()]);}
}
