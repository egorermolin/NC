package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.Context;
import org.openfast.Message;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;

/**
 * Created by egore on 24.02.2016.
 */
public abstract class Processor extends BaseProcessorWithMessageBackup implements IProcessor {

    protected SequenceArray sequenceArray = new SequenceArray();

    protected IMessageSequenceValidator sequenceValidator;

    protected IMessageHandler messageHandler;

    protected Processor(IMessageHandler messageHandler, IMessageSequenceValidator sequenceValidator) {
        this.messageHandler = messageHandler;
        this.sequenceValidator = sequenceValidator;
    }

    protected Processor() {
    }

    @Override
    public IMessageSequenceValidator getSequenceValidator() {
        return sequenceValidator;
    }

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
