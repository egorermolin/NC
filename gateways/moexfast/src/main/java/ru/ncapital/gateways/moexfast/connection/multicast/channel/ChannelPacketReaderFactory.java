package ru.ncapital.gateways.moexfast.connection.multicast.channel;

import ru.ncapital.gateways.moexfast.connection.ConnectionId;
import ru.ncapital.gateways.moexfast.connection.multicast.IMulticastEventListener;

import java.nio.channels.DatagramChannel;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by egore on 5/4/16.
 */
public class ChannelPacketReaderFactory {
    public static int ASYNCH_QUEUE_CAPACITY = 10000;

    public IChannelPacketReader create(IMulticastEventListener eventReceiver, DatagramChannel channel, boolean asynch, ConnectionId connectionId) {
        if (asynch)
            return new AsynchChannelPacketReader(eventReceiver, channel, new ArrayBlockingQueue(ASYNCH_QUEUE_CAPACITY), connectionId);
        else
            return new SynchChannelPacketReader(eventReceiver, channel);
    }
}
