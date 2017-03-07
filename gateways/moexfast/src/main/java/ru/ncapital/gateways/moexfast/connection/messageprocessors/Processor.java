package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Context;
import org.openfast.Message;
import org.openfast.codec.Coder;

/**
 * Created by egore on 24.02.2016.
 */
abstract class Processor extends BaseProcessorWithMessageBackup implements IProcessor {

    SequenceArray sequenceArray = new SequenceArray();

    @Override
    public void handleMessage(Message readMessage, Context context, Coder coder) {
        if (checkSequence(readMessage)) {
            if (getLogger().isTraceEnabled())
                getLogger().trace(readMessage.toString());

            processMessage(readMessage);
        }
    }

    private boolean isMessageFromPrimary() {
        if (isPrimaryAlive())
            return isPrimary();

        // primary channel is dead, make backup act like primary
        return true;
    }

    protected boolean checkSequence(Message readMessage) {
        if (readMessage == null)
            return false;

        int seqNum = readMessage.getInt("MsgSeqNum");
        if (isMessageFromPrimary()) {
            switch (sequenceArray.checkSequence(seqNum)) {
                case IN_SEQUENCE:
                    break;
                case OUT_OF_SEQUENCE:
                    handleMessage(getBackupMessage(seqNum - 1), null, null); // get missing messages from secondary channel
                    break;
                case DUPLICATE:
                    return false;
            }
        } else {
            switch (sequenceArray.checkDuplicate(seqNum)) {
                case DUPLICATE:
                    return false;
                case IN_SEQUENCE:
                case OUT_OF_SEQUENCE:
                    addBackupMessage(seqNum, readMessage); // add backup message
                    return false;
            }
        }

        return true;
    }

    protected abstract void processMessage(Message readMessage);
}
