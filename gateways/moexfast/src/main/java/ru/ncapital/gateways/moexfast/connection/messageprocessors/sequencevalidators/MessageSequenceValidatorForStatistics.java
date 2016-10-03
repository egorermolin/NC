package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public class MessageSequenceValidatorForStatistics extends MessageSequenceValidator {
    public MessageSequenceValidatorForStatistics() {
        super(MessageHandlerType.STATISTICS);
    }
}
