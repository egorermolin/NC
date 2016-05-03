package ru.ncapital.gateways.micexfast.connection.multicast;

import ru.ncapital.gateways.micexfast.Utils;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;

/**
 * Created by egore on 12/9/15.
 */
public class AsynchChannelReader implements Runnable {

    private IEventReceiver eventReceiver;

    private BlockingQueue<ChannelPacket> packetQueue;

    private static final int BUFFER_LENGTH = 1500;

    private DatagramChannel channel;

    private ByteBuffer bytebuffer;

    private volatile boolean running = true;

    public AsynchChannelReader(DatagramChannel channel, BlockingQueue queue, IEventReceiver eventReceiver) {
        this.channel = channel;
        this.packetQueue = queue;
        this.eventReceiver = eventReceiver;
    }

    @Override
    public void run() {
        while (running) {
            try {
                channel.read(bytebuffer);
                long inTimestamp = Utils.currentTimeInTicks();

                packetQueue.offer(new ChannelPacket(bytebuffer, inTimestamp));
            } catch (Exception e) {
                eventReceiver.onDisconnect();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
