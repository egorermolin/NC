// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.connection.multicast.channel;

import ru.ncapital.gateways.micexfast.connection.multicast.IMulticastEventListener;

import java.io.IOException;
import java.nio.channels.DatagramChannel;

/**
 * Created by egore on 5/4/16.
 */
public class SynchChannelPacketReader extends AChannelPacketReader {

    private Thread readingThread = null;

    public SynchChannelPacketReader(IMulticastEventListener eventReceiver, DatagramChannel channel) {
        super(eventReceiver, channel);
    }

    @Override
    public void start() {
        running = true;
        readingThread = Thread.currentThread();
    }

    @Override
    public void stop() {
        running = false;
        readingThread.interrupt();
    }

    @Override
    public ChannelPacket nextPacket() {
        if (running) {
            try {
                return receivePacketFromChannel();
            } catch (IOException e) {
                eventReceiver.onException(e);
            }
        }
        return null;
    }
}
