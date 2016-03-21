package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.GroupValue;

/**
 * Created by egore on 2/3/16.
 */
public interface IMessageSequenceValidator {
    boolean onSnapshotSeq(String symbol, int seqNum);

    boolean onIncrementalSeq(String symbol, int seqNum);

    void storeIncremental(GroupValue mdEntry, String symbol, int seqNum);

    void startRecovering(String symbol);

    GroupValue[] stopRecovering(String symbol);

    boolean isRecovering(String symbol);

    String[] getRecovering();
}
