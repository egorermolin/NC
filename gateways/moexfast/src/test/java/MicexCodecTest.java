import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openfast.Context;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.micexfast.MicexConfigurationManager;
import ru.ncapital.gateways.micexfast.MicexInstrumentManager;
import ru.ncapital.gateways.micexfast.MicexMarketDataManager;
import ru.ncapital.gateways.moexfast.connection.Connection;
import ru.ncapital.gateways.moexfast.connection.ConnectionId;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IIncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.multicast.MessageReader;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by egore on 10/3/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class MicexCodecTest {

    private MessageReader messageReader;

    @Mock
    private MicexConfigurationManager configurationManager;

    @Mock
    private MicexInstrumentManager instrumentManager;

    @Mock
    private MicexMarketDataManager marketDataManager;

    @Mock
    private Connection connection;

    @Mock
    private DatagramChannel channel;

    @Mock
    private MembershipKey membershipKey;

    @Mock
    private IIncrementalProcessor processor;

    @Before
    public void setup() throws IOException {
        when(configurationManager.getConnection(any(ConnectionId.class))).thenReturn(connection);
        when(configurationManager.getPrimaryNetworkInterface()).thenReturn("localhost");
        when(configurationManager.getSecondaryNetworkInterface()).thenReturn("localhost");
        when(configurationManager.getFastTemplatesFile()).thenReturn("src/main/resources/micex/fast_templates.xml");
        when(channel.join(any(InetAddress.class), any(NetworkInterface.class), any(InetAddress.class))).thenReturn(membershipKey);

        when(configurationManager.isAsynchChannelReader()).thenReturn(false);
        messageReader = createMessageReader();

        when(marketDataManager.getIncrementalProcessorInTimestamp(MessageHandlerType.ORDER_LIST)).thenReturn(new ThreadLocal<Long>() {
            @Override
            public Long initialValue() {
                return 0L;
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

    private final byte[] payload = new byte[] {
            (byte) 0xA7, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0xE0, (byte) 0x1B, (byte) 0xB6, (byte) 0x03,
            (byte) 0x7D, (byte) 0xA7, (byte) 0x23, (byte) 0x65, (byte) 0x6D, (byte) 0x37, (byte) 0x0F, (byte) 0x5B,
            (byte) 0x01, (byte) 0xF6, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0xB0, (byte) 0x35, (byte) 0x30,
            (byte) 0x30, (byte) 0x38, (byte) 0xB1, (byte) 0x55, (byte) 0x53, (byte) 0x44, (byte) 0x52, (byte) 0x55,
            (byte) 0x42, (byte) 0x5F, (byte) 0x53, (byte) 0x50, (byte) 0xD4, (byte) 0x18, (byte) 0xDF, (byte) 0x80,
            (byte) 0x24, (byte) 0x0F, (byte) 0x25, (byte) 0xA1, (byte) 0x03, (byte) 0x6D, (byte) 0xF9, (byte) 0xFD,
            (byte) 0x03, (byte) 0x7A, (byte) 0xB7, (byte) 0x81, (byte) 0x01, (byte) 0xB9, (byte) 0xCF, (byte) 0x43,
            (byte) 0x45, (byte) 0x54, (byte) 0xD3, (byte) 0x80};

    private class ChannelReceiveAnswer implements Answer {
        private boolean sent = false;

        private int count = 10;

        ChannelReceiveAnswer(int count) {
            this.count = count;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (!sent) {
                ((ByteBuffer) invocation.getArguments()[0]).put(payload);
                sent = --count == 0;
            } else {
                if (messageReader.isRunning())
                    Executors.newSingleThreadExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            messageReader.stop();
                        }
                    });
            }
            return null;
        }
    }

    @Ignore
    @Test
    public void testOpenfast100k() {
        long begin = System.currentTimeMillis();
        try {
            Mockito.doAnswer(new ChannelReceiveAnswer(100000)).when(channel).receive(any(ByteBuffer.class));
            messageReader.init("info");
            messageReader.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();

        System.out.println("elapsed " + (end-begin));
        System.out.println("avg " + (end-begin) / 100000.0);
    }

    @Test
    public void testOpenfast() {
        try {
            Mockito.doAnswer(new ChannelReceiveAnswer(1)).when(channel).receive(any(ByteBuffer.class));
            messageReader.init("info");
            messageReader.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(processor).handleMessage(messageCaptor.capture(), any(Context.class), any(Coder.class));

        assertEquals(65191, messageCaptor.getValue().getInt("MsgSeqNum"));
        assertEquals(65191, messageCaptor.getValue().getInt(5));
        assertEquals(20151211075748086L, messageCaptor.getValue().getLong("SendingTime"));
        assertEquals(20151211075748086L, messageCaptor.getValue().getLong(6));
        SequenceValue mdEntries = messageCaptor.getValue().getSequence("GroupMDEntries");
        SequenceValue mdEntriesOther = messageCaptor.getValue().getSequence(7);
        assertEquals(mdEntries, mdEntriesOther);
        assertEquals(1, mdEntries.getLength());
        GroupValue mdEntry = mdEntries.get(0);
        assertEquals('0', mdEntry.getString(0).charAt(0));
        assertEquals('0', mdEntry.getString("MDUpdateAction").charAt(0));
        assertEquals('0', mdEntry.getString(1).charAt(0));
        assertEquals('0', mdEntry.getString("MDEntryType").charAt(0));
        assertEquals("50081", mdEntry.getString(2));
        assertEquals("50081", mdEntry.getString("MDEntryID"));
        assertEquals("USDRUB_SPT", mdEntry.getString(3));
        assertEquals("USDRUB_SPT", mdEntry.getString("Symbol"));
        assertEquals("3166", mdEntry.getString(4));
        assertEquals("3166", mdEntry.getString("RptSeq"));
        assertEquals("75748000", mdEntry.getString(6));
        assertEquals("75748000", mdEntry.getString("MDEntryTime"));
        assertEquals("63224", mdEntry.getString(7));
        assertEquals("63224", mdEntry.getString("OrigTime"));
        assertEquals("64.823", mdEntry.getString(8));
        assertEquals("64.823", mdEntry.getString("MDEntryPx"));
        assertEquals("185", mdEntry.getString(9));
        assertEquals("185", mdEntry.getString("MDEntrySize"));
        assertEquals("CETS", mdEntry.getString(12));
        assertEquals("CETS", mdEntry.getString("TradingSessionID"));
    }
}
