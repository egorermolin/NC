package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.Logger;
import ru.ncapital.gateways.micexfast.connection.ConnectionId;
import ru.ncapital.gateways.micexfast.connection.multicast.channel.ChannelPacket;
import ru.ncapital.gateways.micexfast.connection.multicast.channel.ChannelPacketReaderFactory;
import ru.ncapital.gateways.micexfast.connection.multicast.channel.IChannelPacketReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by egore on 12/10/15.
 */
public class MicexFastMulticastInputStream extends InputStream {
    private static final int BUFFER_LENGTH = 1500;

    private IChannelPacketReader packetReader;

    private ByteBuffer bytebuffer;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private Logger logger;

    private ThreadLocal<Long> inTimestamp;

    private ChannelPacketReaderFactory channelPacketReaderFactory = new ChannelPacketReaderFactory();

    private volatile boolean running = false;

    public static char[] byteToHex(byte b) {
        char[] hex = new char[2];
        int v = b & 0xFF;
        hex[0] = hexArray[v >>> 4];
        hex[1] = hexArray[v & 0x0F];
        return hex;
    }

    public MicexFastMulticastInputStream(IMulticastEventListener eventListener, DatagramChannel channel, Logger logger, boolean asynch, ConnectionId connectionId) {
        this.logger = logger;
        this.bytebuffer = ByteBuffer.allocate(BUFFER_LENGTH);
        this.bytebuffer.clear();
        this.bytebuffer.flip();
        packetReader = channelPacketReaderFactory.create(eventListener, channel, asynch, connectionId);
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

    private static String bufferToString(ByteBuffer buf) {
        StringBuilder sb = new StringBuilder("Received ").append(buf.remaining());
        if (buf.hasRemaining()) {
            sb.append(" - ").append(getSeqNum(buf)).append(" = ");

            int count = 0;
            while (count < buf.remaining()) {
                sb.append(byteToHex(buf.get(count))).append(' ');
                count++;
            }
        }
        return sb.toString();
    }

    @Override
    public int available() throws IOException {
        return bytebuffer.remaining();
    }

    @Override
    public int read() throws IOException {
        while (!bytebuffer.hasRemaining()) {
            bytebuffer.clear();
            ChannelPacket packet = packetReader.nextPacket();
            if (packet == null)
                return -1;

            bytebuffer.put(packet.getByteBuffer());
            inTimestamp.set(packet.getInTimestamp());
            bytebuffer.flip();

            if (logger.isTraceEnabled())
                logger.trace(bufferToString(bytebuffer.duplicate()));
        }

        return (bytebuffer.get() & 0xFF);
    }

    public void start() {
        running = true;
        packetReader.start();
    }

    public void stop() {
        packetReader.stop();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
