package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

/**
 * Created by egore on 1/28/16.
 */
public class MessageSequenceValidatorForPublicTrades extends MessageSequenceValidator {
    public MessageSequenceValidatorForPublicTrades() {
        super("PublicTrade");
    }

    @Override
    public boolean onIncrementalSeq(String securityId, int seqNum) {
        if (logger.get().isTraceEnabled())
            logger.get().trace("INC -> " + securityId + "-" + seqNum);

        return true;
    }
}
