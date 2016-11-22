package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 5/30/16.
 */
public class StoredMdEntry<T> {
    private T exchangeSecurityId;

    private int sequenceNumber;

    private GroupValue mdEntry;

    private PerformanceData performanceData;

    private boolean lastFragment;

    private boolean lastEntryInTransaction;

    public StoredMdEntry(T exchangeSecurityId, int sequenceNumber, GroupValue mdEntry, PerformanceData performanceData, boolean lastFragment, boolean lastEntryInTransaction) {
        this.exchangeSecurityId = exchangeSecurityId;
        this.sequenceNumber = sequenceNumber;
        this.mdEntry = mdEntry;
        this.performanceData = performanceData;
        this.lastFragment = lastFragment;
    }

    public T getExchangeSecurityId() {
        return exchangeSecurityId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public GroupValue getMdEntry() {
        return mdEntry;
    }

    public PerformanceData getPerformanceData() {
        return performanceData;
    }

    public boolean isLastFragment() {
        return lastFragment;
    }

    public boolean isLastEntryInTransaction() {
        return lastEntryInTransaction;
    }
}
