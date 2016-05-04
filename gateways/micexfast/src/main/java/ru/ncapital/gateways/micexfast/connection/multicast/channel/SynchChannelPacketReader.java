// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.
package ru.ncapital.gateways.micexfast.connection.multicast.channel;

import ru.ncapital.gateways.micexfast.connection.multicast.IMulticastEventListener;

import java.nio.channels.DatagramChannel;

/**
 * Created by egore on 5/4/16.
 */
public class SynchChannelPacketReader extends AChannelPacketReader {

    public SynchChannelPacketReader(IMulticastEventListener eventReceiver, DatagramChannel channel) {
        super(eventReceiver, channel);
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public ChannelPacket nextPacket() {
        if (running) {
            try {
                return receivePacketFromChannel();
            } catch (Exception e) {
                eventReceiver.onException(e);
            }
        }
        return null;
    }
}
