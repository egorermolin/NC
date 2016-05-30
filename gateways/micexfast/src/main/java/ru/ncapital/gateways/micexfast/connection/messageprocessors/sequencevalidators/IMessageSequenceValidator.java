package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.GroupValue;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.micexfast.domain.PerformanceData;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public interface IMessageSequenceValidator {
    boolean onSnapshotSeq(String securityId, int seqNum);

    boolean onIncrementalSeq(String securityId, int seqNum);

    void storeIncremental(GroupValue mdEntry, String securityId, int seqNum, PerformanceData perfData);

    void startRecovering(String securityId);

    StoredMdEntry[] stopRecovering(String securityId);

    boolean isRecovering(String securityId, boolean isSnapshot);

    boolean isRecovering();

    String[] getRecovering();

    MessageHandlerType getType();
}
