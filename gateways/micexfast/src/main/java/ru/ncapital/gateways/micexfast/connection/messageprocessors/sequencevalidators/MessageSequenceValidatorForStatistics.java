package ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators;

import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public class MessageSequenceValidatorForStatistics extends MessageSequenceValidator {
    public MessageSequenceValidatorForStatistics() {
        super(MessageHandlerType.STATISTICS);
    }
}
