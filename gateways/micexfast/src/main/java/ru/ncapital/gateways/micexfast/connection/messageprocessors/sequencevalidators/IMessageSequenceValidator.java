package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

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

    boolean isRecovering();

    String[] getRecovering();

    MessageHandlerType getType();
}
