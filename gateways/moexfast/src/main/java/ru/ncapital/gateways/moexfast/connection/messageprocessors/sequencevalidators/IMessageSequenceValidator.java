package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public interface IMessageSequenceValidator<T> {
    boolean onSnapshotSeq(T exchangeSecurityId, int seqNum);

    boolean onIncrementalSeq(T exchangeSecurityId, int seqNum);

    void startRecovering(T exchangeSecurityId);

    void storeIncremental(T exchangeSecurityId, int seqNum, GroupValue mdEntry, PerformanceData perfData);

    StoredMdEntry[] stopRecovering(T exchangeSecurityId);

    boolean isRecovering(T exchangeSecurityId, boolean isSnapshot);

    boolean isRecovering();

    T[] getRecovering();

    MessageHandlerType getType();
}
