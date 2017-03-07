package ru.ncapital.gateways.moexfast.connection.messageprocessors;

import org.openfast.MessageHandler;

/**
 * Created by egore on 5/6/16.
 */
public interface IProcessor extends MessageHandler {
    void setIsPrimary(boolean isPrimary);

    void setPrimaryAlive(boolean alive);

    ThreadLocal<Long> getInTimestampHolder();
}
