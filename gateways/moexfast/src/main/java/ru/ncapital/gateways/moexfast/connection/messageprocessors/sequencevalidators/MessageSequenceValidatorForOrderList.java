package ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators;

import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

/**
 * Created by egore on 2/3/16.
 */
public class MessageSequenceValidatorForOrderList<T> extends MessageSequenceValidator<T> {
    public MessageSequenceValidatorForOrderList() {
        super(MessageHandlerType.ORDER_LIST);
    }
}
