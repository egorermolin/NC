package ru.ncapital.gateways.micexfast.connection.multicast;

import cli.System.IO.EndOfStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by egore on 12/10/15.
 */
public class MicexFastMulticastInputStream extends InputStream {
    private static final int BUFFER_LENGTH = 1500;

    private DatagramChannel channel;

    private AsynchChannelPacketReader packetReader;

    private ByteBuffer bytebuffer;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private Logger logger;

    private ThreadLocal<Long> inTimestamp;

    private BlockingQueue<ChannelPacket> packetQueue;

    private final boolean asynch;

    public static char[] byteToHex(byte b) {
        char[] hex = new char[2];
        int v = b & 0xFF;
        hex[0] = hexArray[v >>> 4];
        hex[1] = hexArray[v & 0x0F];
        return hex;
    }

    public MicexFastMulticastInputStream(DatagramChannel channel, Logger logger, IEventListener eventListener, boolean asynch) {
        this.logger = logger;
        this.channel = channel;
        this.bytebuffer = ByteBuffer.allocate(BUFFER_LENGTH);
        this.bytebuffer.clear();
        this.bytebuffer.flip();
        this.asynch = asynch;
        if (this.asynch) {
            packetQueue = new ArrayBlockingQueue<ChannelPacket>(10000);
            packetReader = new AsynchChannelPacketReader(channel, packetQueue, eventListener);
        }
    }

    public void setInTimestamp(ThreadLocal<Long> inTimestamp) {
        this.inTimestamp = inTimestamp;
    }

    private static int getSeqNum(ByteBuffer bb) {
        return (int) ((bb.get(0) & 0xFF)
                          + (bb.get(1) & 0xFF) * 256
                          + (bb.get(2) & 0xFF) * 256 * 256
                          + (bb.get(3) & 0xFF) * 256.* 256 * 256);
    }

    private String bufferToString(ByteBuffer buf) {
        StringBuilder sb = new StringBuilder("Received " + buf.remaining() + " - " + getSeqNum(buf) + " = ");
        int count = 0;

        while (count < buf.remaining()) {
            sb.append(byteToHex(buf.get(count))).append(' ');
            count++;
        }

        return sb.toString();
    }

    @Override
    public int available() throws IOException {
        return bytebuffer.remaining();
    }

    @Override
    public int read() throws IOException {
        if (!bytebuffer.hasRemaining()) {
            bytebuffer.clear();
            if (asynch) {
                try {
                    ChannelPacket packet = packetQueue.take();
                    bytebuffer.put(packet.getByteBuffer());
                    inTimestamp.set(packet.getInTimestamp());
                } catch (InterruptedException e) {
                    throw new IOException("Reading from queue interrupted");
                }
            } else {
                channel.receive(bytebuffer);
                inTimestamp.set(Utils.currentTimeInTicks());
            }
            bytebuffer.flip();

            if (logger.isTraceEnabled())
                logger.trace(bufferToString(bytebuffer.duplicate()));
        }

        return (bytebuffer.get() & 0xFF);
    }

    public void start() {
        if (asynch) {
            packetReader.run();
        }
    }

    public void stop() {
        if (asynch) {
            packetReader.stop();
        }
    }
}
