// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.connection.multicast.channel;

import ru.ncapital.gateways.micexfast.connection.multicast.IMulticastEventListener;

import java.nio.channels.DatagramChannel;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by egore on 5/4/16.
 */
public class ChannelPacketReaderFactory {
    public static int ASYNCH_QUEUE_CAPACITY = 10000;

    public IChannelPacketReader create(IMulticastEventListener eventReceiver, DatagramChannel channel, boolean asynch) {
        if (asynch)
            return new AsynchChannelPacketReader(eventReceiver, channel, new ArrayBlockingQueue(ASYNCH_QUEUE_CAPACITY));
        else
            return new SynchChannelPacketReader(eventReceiver, channel);
    }
}
