package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 1/28/16.
 */
public class MessageSequenceValidatorForPublicTrades extends MessageSequenceValidator {
    public MessageSequenceValidatorForPublicTrades() {
        super(MessageHandlerType.PUBLIC_TRADES);
    }

    @Override
    public boolean onIncrementalSeq(String securityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("INC -> " + securityId + "-" + seqNum);

        return true;
    }

    @Override
    public boolean isRecovering(String securityId, boolean isSnapshot) {
        return false;
    }
}
