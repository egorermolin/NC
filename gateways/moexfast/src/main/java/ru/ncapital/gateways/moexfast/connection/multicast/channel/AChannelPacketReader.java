package ru.ncapital.gateways.moexfast.connection.multicast.channel;

import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.multicast.IMulticastEventListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by egore on 5/4/16.
 */
public abstract class AChannelPacketReader implements IChannelPacketReader {

    protected static final int BUFFER_LENGTH = 1500;

    protected IMulticastEventListener eventReceiver;

    protected DatagramChannel channel;

    protected ByteBuffer bytebuffer;

    protected volatile boolean running = true;

    AChannelPacketReader(IMulticastEventListener eventReceiver, DatagramChannel channel) {
        this.eventReceiver = eventReceiver;
        this.channel = channel;
        this.bytebuffer = ByteBuffer.allocate(BUFFER_LENGTH);
        this.bytebuffer.clear();
        this.bytebuffer.flip();
    }


    protected ChannelPacket receivePacketFromChannel() throws IOException {
        bytebuffer.clear();
        channel.receive(bytebuffer);
        bytebuffer.flip();

        return new ChannelPacket(bytebuffer, Utils.currentTimeInTicks());
    }
}
