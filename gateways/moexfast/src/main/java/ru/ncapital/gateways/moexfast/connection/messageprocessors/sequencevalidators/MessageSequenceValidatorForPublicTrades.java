package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public class MessageSequenceValidatorForPublicTrades<T> extends MessageSequenceValidator<T> {
    public MessageSequenceValidatorForPublicTrades() {
        super(MessageHandlerType.PUBLIC_TRADES);
    }

    @Override
    public boolean onIncrementalSeq(T exchangeSecurityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("INC -> " + exchangeSecurityId + " " + seqNum);

        return true;
    }

    @Override
    public boolean isRecovering(T exchangeSecurityId, boolean isSnapshot) {
        return false;
    }
}
