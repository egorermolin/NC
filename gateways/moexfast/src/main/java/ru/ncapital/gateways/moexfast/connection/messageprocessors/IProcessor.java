package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.MessageHandler;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;

public interface IProcessor extends MessageHandler {
    void setIsPrimary(boolean isPrimary);

    ThreadLocal<Long> getInTimestampHolder();
}
