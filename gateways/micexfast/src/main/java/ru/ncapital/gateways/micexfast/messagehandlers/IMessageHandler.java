package ru.ncapital.gateways.micexfast.messagehandlers;

import org.openfast.GroupValue;
import org.openfast.Message;

/**
 * Created by egore on 1/21/16.
 */
public interface IMessageHandler {
    boolean isAllowedUpdate(String symbol, String trandingSessionId);

    void onSnapshot(Message readMessage, long inTime);

    void onIncremental(GroupValue mdEntry, long inTime);

    void beforeIncremental(GroupValue mdEntry, long inTime);

    void flushIncrementals(long inTime);

    String getType();
}
