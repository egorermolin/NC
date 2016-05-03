package ru.ncapital.gateways.micexfast.connection.multicast;

import java.nio.ByteBuffer;

/**
 * Created by egore on 12/9/15.
 */
public class ChannelPacket {
    private static final int BUFFER_LENGTH = 1500;

    private ByteBuffer byteBuffer;

    private long inTimestamp;

    public ChannelPacket(ByteBuffer buffer, long inTimestamp) {
        this.byteBuffer = ByteBuffer.allocate(BUFFER_LENGTH);
        this.byteBuffer.clear();
        this.byteBuffer.flip();
        this.byteBuffer.put(buffer);
        this.inTimestamp = inTimestamp;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public long getInTimestamp() { return inTimestamp; }
}
