package ru.ncapital.gateways.micexfast.connection.multicast;

import ru.ncapital.gateways.micexfast.connection.multicast.channel.ChannelPacket;

import java.nio.channels.Channel;

/**
 * Created by egore on 5/2/16.
 */
public interface IMulticastEventListener {

    void onException(Exception e);
}
