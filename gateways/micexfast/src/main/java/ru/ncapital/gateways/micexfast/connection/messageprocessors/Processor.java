package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import com.google.inject.Inject;
import org.openfast.Context;
import org.openfast.Message;
import org.openfast.MessageHandler;
import org.openfast.codec.Coder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.connection.ConnectionManager;

import java.util.*;

/**
 * Created by egore on 24.02.2016.
 */
public abstract class Processor extends BaseProcessorWithMessageBackup implements MessageHandler {

    protected SequenceArray sequenceArray = new SequenceArray();

    @Inject
    protected ConnectionManager connectionManager;

    @Override
    public void handleMessage(Message readMessage, Context context, Coder coder) {
        handleMessage(readMessage);
    }

    private void handleMessage(Message readMessage) {
        if (checkSequence(readMessage)) {
            if (getLogger().isTraceEnabled())
                getLogger().trace(readMessage.toString());

            processMessage(readMessage);
        }
    }

    protected boolean checkSequence(Message readMessage) {
        if (readMessage == null)
            return false;

        int seqNum = readMessage.getInt("MsgSeqNum");
        if (isPrimary()) {
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
