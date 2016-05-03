package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.Utils;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by egore on 12/9/15.
 */
public class AsynchChannelPacketReader implements Runnable {

    private IEventListener eventReceiver;

    private BlockingQueue<ChannelPacket> packetQueue;

    private static final int BUFFER_LENGTH = 1500;

    private DatagramChannel channel;

    private ByteBuffer bytebuffer;

    private volatile boolean running = true;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public AsynchChannelPacketReader(DatagramChannel channel, BlockingQueue queue, IEventListener eventReceiver) {
        this.channel = channel;
        this.packetQueue = queue;
        this.eventReceiver = eventReceiver;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void run() {
        while (running) {
            try {
                channel.read(bytebuffer);
                long inTimestamp = Utils.currentTimeInTicks();

                packetQueue.offer(new ChannelPacket(bytebuffer, inTimestamp));
            } catch (Exception e) {
                eventReceiver.onException(e);
            }
        }
    }

    public void start() {
        executor.execute(this);
    }

    public void stop() {
        running = false;
        executor.shutdown();
        try {
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            Utils.printStackTrace(e, LoggerFactory.getLogger("AsynchChannelPacketReader"));
        }
    }
}
