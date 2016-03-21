package ru.ncapital.gateways.micexfast.connection.multicast.packet;

import org.openfast.Context;
import org.openfast.Message;
import org.openfast.MessageHandler;
import org.openfast.codec.Coder;
import org.openfast.template.MessageTemplate;

/**
 * Created by egore on 12/9/15.
 */
public class PacketHandler implements MessageHandler {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private MessageTemplate[] templates;

    public static char[] byteToHex(byte b) {
        char[] hex = new char[2];
        int v = b & 0xFF;
        hex[0] = hexArray[v >>> 4];
        hex[1] = hexArray[v & 0x0F];
        return hex;
    }

    public static byte hexToByte(char[] hex, int offset) {
        return (byte) ((Character.digit(hex[offset + 0], 16) << 4)
                + Character.digit(hex[offset + 1], 16));
    }
/*
    public PacketHandler() {
        InputStream templateSource = null;
        try {
            templateSource = new FileInputStream("/home/egore/Downloads/fast_templates.xml");
            MessageTemplateLoader templateLoader = new XMLMessageTemplateLoader();
            templates = templateLoader.load(templateSource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        MessageInputStream messageIn = new MessageInputStream(new InputStream() {

            private final char[] message = new String("" +
                    "C0 10 BC 40 97 23 65 6D 33 29 34 08 93").toCharArray();

            private final char[] message2 = new String("E0 1B B8 01 00 EF 23 65 6D 33 2C 29 6B 9B 82 82 B1 53 " +
                    "30 31 30 34 30 32 31 30 B0 43 4E 59 52 55 42 5F 53 50 " +
                    "D4 03 AC FC 06 2C D5 82 90 1D 49 1B E9 38 4C FD 43 45 " +
                    "54 D3 80 82 B0 42 30 30 30 37 30 37 37 35 B0 45 55 52 " +
                    "55 53 44 30 30 30 54 4F CD 03 D1 FB 04 28 F7 82 87 1D " +
                    "49 1B E9 38 58 EA 43 45 54 D3 80").toCharArray();

            private final char[] message3 = new String("E0 1B B8 05 2E EA 23 65 6D 33 39 57 10 E7 81 83 B0 42 30 "+
                    "30 30 36 34 31 32 38 B0 55 53 44 52 55 42 5F 53 50 D4 24 B2 FD 03 75 80 80 2A 76 40 F1 3C 60 93 43 45 54 D3 80").toCharArray();

            private int offset;

            @Override
            public int read() throws IOException {
                if (offset < message3.length) {
                    int b = hexToByte(message3, offset);

                    offset += 3;

                    return b & 0xFF;
                } else {
                    throw new EOFException();
                }
            }
        });

         for (MessageTemplate template : templates) {
            messageIn.registerTemplate(Integer.valueOf(template.getId()), template);
        }

        messageIn.addMessageHandler(this);

        messageIn.readMessage();
    }

   public void onPacket(Packet packet) {
        ByteBuffer buf = packet.getCurrentByteBuffer();
        StringBuilder sb = new StringBuilder("Received " + buf.remaining() + ":");
        int count = 0;

        while (count < buf.remaining()) {
            sb.append(byteToHex(buf.get(count))).append(' ');
            count++;
        }

        System.out.println(sb.toString());
    }*/

    @Override
    public void handleMessage(Message readMessage, Context context, Coder coder) {
        System.out.println(readMessage);
    }
}
