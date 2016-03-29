package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.GroupValue;

/**
 * Created by egore on 2/3/16.
 */
public interface IMessageSequenceValidator {
    boolean onSnapshotSeq(String securityId, int seqNum);

    boolean onIncrementalSeq(String securityId, int seqNum);

    void storeIncremental(GroupValue mdEntry, String securityId, int seqNum);

    void startRecovering(String securityId);

    GroupValue[] stopRecovering(String securityId);

    boolean isRecovering(String securityId, boolean isSnapshot);

    String[] getRecovering();
}
