package ru.ncapital.gateways.micexfast.connection.multicast.packet;

import java.nio.ByteBuffer;

/**
 * Created by egore on 12/9/15.
 */
public class Packet {
    private static final int BUFFER_LENGTH = 1590;

    private ByteBuffer bytebuffer;

    public Packet() {
        this.bytebuffer = ByteBuffer.allocate(BUFFER_LENGTH);
    }

    public ByteBuffer getNextByteBuffer() {
        bytebuffer.clear();

        return bytebuffer;
    }

    public ByteBuffer getCurrentByteBuffer() {
        bytebuffer.flip();

        return bytebuffer;
    }
}
