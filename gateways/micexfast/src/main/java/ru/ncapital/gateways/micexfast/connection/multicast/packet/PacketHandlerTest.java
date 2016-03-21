package ru.ncapital.gateways.micexfast.connection.multicast.packet;

import org.apache.log4j.Level;
import org.openfast.*;
import org.openfast.error.FastException;
import org.openfast.logging.FastMessageLogger;
import org.openfast.template.MessageTemplate;
import org.openfast.template.loader.MessageTemplateLoader;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ncapital.gateways.micexfast.GatewayManager;
import ru.ncapital.gateways.micexfast.connection.multicast.MicexBlockReader;
import ru.ncapital.gateways.micexfast.connection.multicast.MicexFastFileInputStream;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by egore on 12/9/15.
 */
public class PacketHandlerTest {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private MessageTemplate[] templates;

    private Logger logger = LoggerFactory.getLogger("PacketHandlerTest");

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

    public PacketHandlerTest() throws IOException {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.TRACE);
        InputStream templateSource = null;
        try {
            templateSource = new FileInputStream("src/main/resources/fast_templates.xml");
            MessageTemplateLoader templateLoader = new XMLMessageTemplateLoader();
            templates = templateLoader.load(templateSource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        /*
        MessageInputStream messageIn = new MessageInputStream(new InputStream() {
            private final char[] message = new String("" +
                    "C0 10 BC 40 97 23 65 6D 33 29 34 08 93").toCharArray();

            private final char[] message2 = new String("E0 1B B8 01 00 EF 23 65 6D 33 2C 29 6B 9B 82 82 B1 53 " +
                    "30 31 30 34 30 32 31 30 B0 43 4E 59 52 55 42 5F 53 50 " +
                    "D4 03 AC FC 06 2C D5 82 90 1D 49 1B E9 38 4C FD 43 45 " +
                    "54 D3 80 82 B0 42 30 30 30 37 30 37 37 35 B0 45 55 52 " +
                    "55 53 44 30 30 30 54 4F CD 03 D1 FB 04 28 F7 82 87 1D " +
                    "49 1B E9 38 58 EA 43 45 54 D3 80").toCharArray();

            private final char[] message3 = new String("E0 1B B8 03 30 B4 23 65 6D 37 0E 5F 24 9C 81 82 B0 42 30 " +
                    "30 30 36 36 31 38 30 B0 55 53 44 30 30 30 30 30 30 54 " +
                    "4F C4 13 DA FE 33 DA 81 15 84 23 13 41 C1 34 13 FF 43 45 54 D3 80").toCharArray();

            private int offset;

            @Override
            public int read() throws IOException {
                if (offset < message3.length) {
                    int b = hexToByte(message3, offset);

                    offset += 3;

                    return b & 0xFF;
                } else {
                    return -1;
                }
            }
        });
        */

        MessageInputStream messageIn = new MessageInputStream(new MicexFastFileInputStream("src/test/resources/production"));

        for (MessageTemplate template : templates) {
            messageIn.registerTemplate(Integer.valueOf(template.getId()), template);
        }
/*
        messageIn.addMessageHandler(new MarketDataManager() {
            {
                setMarketDataHandler(new DefaultMarketDataHandler());
                subscribe(new Subscription("CNYRUB_SPT"));
                subscribe(new Subscription("EURUSD000TOM"));
            }
        });*/


        logger.info("READY");

        messageIn.setBlockReader(new MicexBlockReader());

        messageIn.getContext().setLogger(new FastMessageLogger() {
            @Override
            public void log(Message message, byte[] bytes, Direction direction) {
                if (logger.isTraceEnabled())
                    logger.trace("(IN) " + message.toString());
            }
        });
        // 77
        //messageIn.getContext().setTraceEnabled(true);

        try {
            while (true)
                messageIn.readMessage();
        } catch (FastException e) {
            // do nothing
            logger.error(e.getMessage(), e);
        }
    }


    public static void main(String[] args) throws IOException {

        GatewayManager.addConsoleAppender("%d{hh:mm:sss} %m %n", Level.TRACE);
        new PacketHandlerTest();
    }
}
