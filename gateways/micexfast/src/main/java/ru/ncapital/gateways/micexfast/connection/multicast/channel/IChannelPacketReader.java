package ru.ncapital.gateways.micexfast.connection.multicast.channel;

/**
 * Created by egore on 04.05.2016.
 */
public interface IChannelPacketReader {

    void start();

    void stop();

    ChannelPacket nextPacket();
}
