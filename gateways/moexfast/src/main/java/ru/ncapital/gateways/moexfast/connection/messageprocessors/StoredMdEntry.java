package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

public class StoredMdEntry<T> {
    private T exchangeSecurityId;

    private int sequenceNumber;

    private GroupValue mdEntry;

    private PerformanceData performanceData;

    public StoredMdEntry(T exchangeSecurityId, int sequenceNumber, GroupValue mdEntry, PerformanceData performanceData) {
        this.exchangeSecurityId = exchangeSecurityId;
        this.sequenceNumber = sequenceNumber;
        this.mdEntry = mdEntry;
        this.performanceData = performanceData;
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
}
