package ru.ncapital.gateways.micexfast.connection.multicast.channel;

import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.connection.multicast.IMulticastEventListener;

import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by egore on 12/9/15.
 */
public class AsynchChannelPacketReader extends AChannelPacketReader {

    private BlockingQueue<ChannelPacket> packetQueue;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public AsynchChannelPacketReader(IMulticastEventListener eventListener, DatagramChannel channel, BlockingQueue queue) {
        super(eventListener, channel);

        this.packetQueue = queue;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start() {
        running = true;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        ChannelPacket channelPacket = receivePacketFromChannel();
                        if (channelPacket != null)
                            packetQueue.offer(channelPacket);

                    } catch (Exception e) {
                        eventReceiver.onException(e);
                    }
                }
            }
        });
    }

    @Override
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

    @Override
    public ChannelPacket nextPacket() {
        if (running) {
            try {
                return packetQueue.take();
            } catch (Exception e) {
                Utils.printStackTrace(e, LoggerFactory.getLogger("AsynchChannelPacketReader"));
            }
        }
        return null;
    }
}
