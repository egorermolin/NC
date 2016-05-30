package ru.ncapital.gateways.micexfast.messagehandlers;

import org.openfast.GroupValue;
import org.openfast.Message;
import ru.ncapital.gateways.micexfast.domain.PerformanceData;

/**
 * Created by egore on 1/21/16.
 */
public interface IMessageHandler {
    boolean isAllowedUpdate(String symbol, String trandingSessionId);

    void onSnapshot(Message readMessage, PerformanceData perfData);

    void beforeIncremental(GroupValue mdEntry, PerformanceData perfData);

    void onIncremental(GroupValue mdEntry, PerformanceData perfData);

    void flushIncrementals(PerformanceData perfData);

    MessageHandlerType getType();
}
