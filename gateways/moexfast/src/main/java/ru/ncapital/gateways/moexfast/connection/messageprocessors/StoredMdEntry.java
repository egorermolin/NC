package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 5/30/16.
 */
public class StoredMdEntry {
    private String securityId;

    private int sequenceNumber;

    private GroupValue mdEntry;

    private PerformanceData performanceData;

    public StoredMdEntry(String securityId, int sequenceNumber, GroupValue mdEntry, PerformanceData performanceData) {
        this.securityId = securityId;
        this.sequenceNumber = sequenceNumber;
        this.mdEntry = mdEntry;
        this.performanceData = performanceData;
    }

    public String getSecurityId() {
        return securityId;
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
