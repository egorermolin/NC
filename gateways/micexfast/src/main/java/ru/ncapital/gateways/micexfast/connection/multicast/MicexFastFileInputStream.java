package ru.ncapital.gateways.micexfast.connection.multicast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by egore on 12/10/15.
 */
public class MicexFastFileInputStream extends InputStream {
    private static final int BUFFER_LENGTH = 1500;

    private BufferedReader reader;

    private ByteBuffer bytebuffer;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private Logger logger = LoggerFactory.getLogger("MicexFastFileInputStream");

    public static char[] byteToHex(byte b) {
        char[] hex = new char[2];
        int v = b & 0xFF;
        hex[0] = hexArray[v >>> 4];
        hex[1] = hexArray[v & 0x0F];
        return hex;
    }

    public static byte hexToByte(char hex0, char hex1) {
        return (byte) ((Character.digit(hex0, 16) << 4)
                + Character.digit(hex1, 16));
    }

    public static byte[] hexToByte(String hexLine2) {
        String hexLine = hexLine2.toUpperCase();
        byte[] byteLine = new byte[hexLine.length() / 3 + 1];
        int count = 0;
        while (count < hexLine.length()) {
            byteLine[count / 3] = hexToByte(hexLine.charAt(count), hexLine.charAt(count + 1));
            count += 3;
        }
        return byteLine;
    }

    public MicexFastFileInputStream(String file) throws IOException {
        reader =  Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8);

        this.bytebuffer = ByteBuffer.allocate(BUFFER_LENGTH);
        this.bytebuffer.flip();
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
            String lineIn = reader.readLine();
            if (lineIn == null)
                return -1;

            bytebuffer.clear();
            bytebuffer.put(hexToByte(lineIn));
            bytebuffer.flip();

            if (logger.isTraceEnabled())
                logger.trace(bufferToString(bytebuffer.duplicate()));
        }

        return (bytebuffer.get() & 0xFF);
    }

    public static void main(String[] args) throws IOException {
        InputStream in = new MicexFastFileInputStream(args[0]);

        int b;
        while ((b = in.read()) >= 0)
            ;
    }
}
