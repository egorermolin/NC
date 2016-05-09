// Copyright 2016 Orc Software AB All rights reserved.
// Reproduction in whole or in part in any form or medium without express
// written permission of Orc Software AB is strictly prohibited.

import org.codehaus.groovy.control.messages.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openfast.Context;
import org.openfast.MessageHandler;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.micexfast.ConfigurationManager;
import ru.ncapital.gateways.micexfast.InstrumentManager;
import ru.ncapital.gateways.micexfast.MarketDataManager;
import ru.ncapital.gateways.micexfast.connection.Connection;
import ru.ncapital.gateways.micexfast.connection.ConnectionId;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IProcessor;
import ru.ncapital.gateways.micexfast.connection.multicast.MessageReader;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;

/**
 * Created by egore on 5/4/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageReaderTest {
    private MessageReader messageReaderAsynch;

    private MessageReader messageReaderSynch;

    @Mock
    private ConfigurationManager configurationManager;

    @Mock
    private InstrumentManager instrumentManager;

    @Mock
    private MarketDataManager marketDataManager;

    @Mock
    private Connection connection;

    @Mock
    private DatagramChannel channel;

    @Mock
    private MembershipKey membershipKey;

    @Mock
    private IProcessor processor;

    private MessageReader testableReader;

    @Before
    public void setup() throws IOException {
        when(configurationManager.getConnection(any(ConnectionId.class))).thenReturn(connection);
        when(configurationManager.getPrimaryNetworkInterface()).thenReturn("localhost");
        when(configurationManager.getSecondaryNetworkInterface()).thenReturn("localhost");
        when(configurationManager.getFastTemplatesFile()).thenReturn("src/main/resources/fast_templates.xml");
        when(channel.join(any(InetAddress.class), any(NetworkInterface.class), any(InetAddress.class))).thenReturn(membershipKey);

        when(configurationManager.isAsynchChannelReader()).thenReturn(true);
        messageReaderAsynch = createMessageReader();

        when(configurationManager.isAsynchChannelReader()).thenReturn(false);
        messageReaderSynch = createMessageReader();

        when(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST)).thenReturn(new ThreadLocal<Long>() {
            @Override
            public Long initialValue() {
                return new Long(0);
            }
        });
        when(marketDataManager.getIncrementalProcessor(MessageHandlerType.ORDER_LIST)).thenReturn(processor);
    }

    private MessageReader createMessageReader() {
        return new MessageReader(ConnectionId.CURR_ORDER_LIST_INCR_A, configurationManager, marketDataManager, instrumentManager) {
            @Override
            public DatagramChannel openChannel() throws IOException {
                return channel;
            }

            @Override
            public void destroy() throws IOException {

            }

            @Override
            public NetworkInterface getNetworkInterface(String name) throws SocketException {
                return NetworkInterface.getNetworkInterfaces().nextElement();
            }
        };
    }

    @Test
    public void testStartAndStopSynch() throws IOException, InterruptedException {
        testStartAndStop(messageReaderSynch);
    }

    @Test
    public void testStartAndStopASynch() throws IOException, InterruptedException {
        testStartAndStop(messageReaderAsynch);
    }

    public void testStartAndStop(MessageReader reader) throws IOException, InterruptedException {
        testableReader = reader;
        try {
            Mockito.doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    if (testableReader.isRunning())
                        Executors.newSingleThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                testableReader.stop();
                            }
                        });

                    return null;
                }
            }).when(channel).receive(any(ByteBuffer.class));

            testableReader.init("debug");
            testableReader.start();
        } catch (Exception e) {
            fail();
        }

        verify(channel, atLeastOnce()).join(any(InetAddress.class), any(NetworkInterface.class), any(InetAddress.class));
        verify(channel, atLeastOnce()).receive(any(ByteBuffer.class));
    }

    @Test
    public void testPayloadSynch() throws IOException {
        testPayload(messageReaderSynch);
    }

    @Test
    public void testPayloadASynch() throws IOException {
        testPayload(messageReaderAsynch);
    }

    public void testPayload(MessageReader reader) throws IOException {
        final byte[] payload = new byte[] {
                (byte) 0xA7, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0xE0, (byte) 0x1B, (byte) 0xB6, (byte) 0x03,
                (byte) 0x7D, (byte) 0xA7, (byte) 0x23, (byte) 0x65, (byte) 0x6D, (byte) 0x37, (byte) 0x0F, (byte) 0x5B,
                (byte) 0x01, (byte) 0xF6, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0xB0, (byte) 0x35, (byte) 0x30,
                (byte) 0x30, (byte) 0x38, (byte) 0xB1, (byte) 0x55, (byte) 0x53, (byte) 0x44, (byte) 0x52, (byte) 0x55,
                (byte) 0x42, (byte) 0x5F, (byte) 0x53, (byte) 0x50, (byte) 0xD4, (byte) 0x18, (byte) 0xDF, (byte) 0x80,
                (byte) 0x24, (byte) 0x0F, (byte) 0x25, (byte) 0xA1, (byte) 0x03, (byte) 0x6D, (byte) 0xF9, (byte) 0xFD,
                (byte) 0x03, (byte) 0x7A, (byte) 0xB7, (byte) 0x81, (byte) 0x01, (byte) 0xB9, (byte) 0xCF, (byte) 0x43,
                (byte) 0x45, (byte) 0x54, (byte) 0xD3, (byte) 0x80};

        testableReader = reader;
        try {
            Mockito.doAnswer(new Answer() {
                private boolean sent = false;

                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    if (!sent) {
                        ((ByteBuffer)invocation.getArguments()[0]).put(payload);
                        sent = true;
                    } else {
                        if (testableReader.isRunning())
                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    testableReader.stop();
                                }
                            });
                    }
                    return null;
                }
            }).when(channel).receive(any(ByteBuffer.class));

            testableReader.init("debug");
            testableReader.start();
        } catch (Exception e) {
            fail();
        }

        ArgumentCaptor<org.openfast.Message> messageCaptor = ArgumentCaptor.forClass(org.openfast.Message.class);
        verify(processor).handleMessage(messageCaptor.capture(), any(Context.class), any(Coder.class));

        assertEquals(20151211075748086L, messageCaptor.getValue().getLong("SendingTime"));
    }

    @Test
    public void testPayloadASynch100000() throws IOException {
        final byte[] payload = new byte[] {
                (byte) 0xA7, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0xE0, (byte) 0x1B, (byte) 0xB6, (byte) 0x03,
                (byte) 0x7D, (byte) 0xA7, (byte) 0x23, (byte) 0x65, (byte) 0x6D, (byte) 0x37, (byte) 0x0F, (byte) 0x5B,
                (byte) 0x01, (byte) 0xF6, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0xB0, (byte) 0x35, (byte) 0x30,
                (byte) 0x30, (byte) 0x38, (byte) 0xB1, (byte) 0x55, (byte) 0x53, (byte) 0x44, (byte) 0x52, (byte) 0x55,
                (byte) 0x42, (byte) 0x5F, (byte) 0x53, (byte) 0x50, (byte) 0xD4, (byte) 0x18, (byte) 0xDF, (byte) 0x80,
                (byte) 0x24, (byte) 0x0F, (byte) 0x25, (byte) 0xA1, (byte) 0x03, (byte) 0x6D, (byte) 0xF9, (byte) 0xFD,
                (byte) 0x03, (byte) 0x7A, (byte) 0xB7, (byte) 0x81, (byte) 0x01, (byte) 0xB9, (byte) 0xCF, (byte) 0x43,
                (byte) 0x45, (byte) 0x54, (byte) 0xD3, (byte) 0x80};

        testableReader = messageReaderAsynch;
        try {
            Mockito.doAnswer(new Answer() {
                private int sent = 100000;

                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    if (sent > 0) {
                        ((ByteBuffer)invocation.getArguments()[0]).put(payload);
                        sent--;
                    } else {
                        if (testableReader.isRunning())
                            Executors.newSingleThreadExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    testableReader.stop();
                                }
                            });
                    }
                    return null;
                }
            }).when(channel).receive(any(ByteBuffer.class));

            testableReader.init("info");
            testableReader.start();
        } catch (Exception e) {
            fail();
        }

        ArgumentCaptor<org.openfast.Message> messageCaptor = ArgumentCaptor.forClass(org.openfast.Message.class);
        verify(processor, atLeastOnce()).handleMessage(messageCaptor.capture(), any(Context.class), any(Coder.class));

        assertEquals(20151211075748086L, messageCaptor.getValue().getLong("SendingTime"));
    }
}
