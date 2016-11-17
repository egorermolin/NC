package ru.ncapital.gateways.moexfast.messagehandlers;

import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/21/16.
 */
public interface IMessageHandler<T> {
    boolean isAllowedUpdate(T securityId);

    void onSnapshot(Message readMessage);

    void onIncremental(GroupValue mdEntry, PerformanceData perfData);

    void flushIncrementals();

    MessageHandlerType getType();

    void onMarketReset();
}
