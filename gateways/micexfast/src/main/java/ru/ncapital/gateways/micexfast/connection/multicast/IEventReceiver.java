package ru.ncapital.gateways.micexfast.connection.multicast;

/**
 * Created by egore on 5/2/16.
 */
public interface IEventReceiver {

    void onDisconnect();

    void onPacketRead();
}
