package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public interface IMessageSequenceValidator<T> {
    boolean onSnapshotSeq(T securityId, int seqNum);

    boolean onIncrementalSeq(T securityId, int seqNum);

    void startRecovering(T securityId);

    void storeIncremental(T securityId, int seqNum, GroupValue mdEntry, PerformanceData perfData);

    StoredMdEntry[] stopRecovering(T securityId);

    boolean isRecovering(T securityId, boolean isSnapshot);

    boolean isRecovering();

    T[] getRecovering();

    MessageHandlerType getType();
}
