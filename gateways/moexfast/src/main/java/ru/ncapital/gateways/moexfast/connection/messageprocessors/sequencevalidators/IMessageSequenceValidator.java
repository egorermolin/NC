package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public interface IMessageSequenceValidator {
    boolean onSnapshotSeq(String securityId, int seqNum);

    boolean onIncrementalSeq(String securityId, int seqNum);

    void startRecovering(String securityId);

    void storeIncremental(String securityId, int seqNum, GroupValue mdEntry, PerformanceData perfData);

    StoredMdEntry[] stopRecovering(String securityId);

    boolean isRecovering(String securityId, boolean isSnapshot);

    boolean isRecovering();

    String[] getRecovering();

    MessageHandlerType getType();
}
