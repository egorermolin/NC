import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.Context;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.SnapshotProcessor;

/**
 * Created by egore on 1/11/16.
 */

@RunWith(MockitoJUnitRunner.class)
public class SnapshotProcessorTest {
    @Mock
    private Context context;

    @Mock
    private Coder coder;
    
    @Mock
    private IMessageHandler marketDataHandler;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;
    
    @Mock
    private MessageSequenceValidator sequenceValidator;

    private SnapshotProcessor snapshotProcessor;

    @Before
    public void setup() {
        Mockito.when(marketDataHandler.getType()).thenReturn("Test");

        snapshotProcessor = new SnapshotProcessor(marketDataHandler, sequenceValidator);

        Mockito.when(sequenceValidator.isRecovering("SYMB:CETS")).thenReturn(true);
        Mockito.when(sequenceValidator.isRecovering("SYMB2:CETS")).thenReturn(true);
        Mockito.when(sequenceValidator.onSnapshotSeq(Mockito.eq("SYMB:CETS"), Mockito.anyInt())).thenReturn(true);
        Mockito.when(sequenceValidator.onSnapshotSeq(Mockito.eq("SYMB2:CETS"), Mockito.anyInt())).thenReturn(true);
        Mockito.when(sequenceValidator.getRecovering()).thenReturn(new String[]{"SYMB:CETS", "SYMB2:CETS"});

        Mockito.when(marketDataHandler.isAllowedUpdate("SYMB", "CETS")).thenReturn(true);
        Mockito.when(marketDataHandler.isAllowedUpdate("SYMB2", "CETS")).thenReturn(true);
    }

    @Test
    public void testSnapshot() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(sequenceValidator).isRecovering("SYMB:CETS");
        Mockito.verify(sequenceValidator).stopRecovering("SYMB:CETS");
    }

    @Test
    public void testSnapshot2Symbols() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB2");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message2.getInt("RptSeq")).thenReturn(100);

        snapshotProcessor.handleMessage(message, context, coder);
        snapshotProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(2)).onSnapshot(messageCaptor.capture(), Mockito.anyLong());
        Mockito.verify(sequenceValidator).isRecovering("SYMB:CETS");
        Mockito.verify(sequenceValidator).isRecovering("SYMB2:CETS");
        Mockito.verify(sequenceValidator).stopRecovering("SYMB:CETS");
        Mockito.verify(sequenceValidator).stopRecovering("SYMB2:CETS");
    }

    @Test
    public void testSnapshotNotRecovering() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB3");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.never()).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(sequenceValidator, Mockito.never()).onSnapshotSeq(Mockito.eq("SYMB:CETS"), Mockito.anyInt());
    }

    @Test
    public void testSnapshotDuplicateStart() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);
        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
    }

    @Test
    public void testSnapshotDuplicateStart2() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.when(message.getLong("SendingTime")).thenReturn(2L);
        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(2)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
    }

    @Test
    public void testSnapshotDuplicate() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
    }

    @Test
    public void testSnapshotWithIncrementals() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        GroupValue incMdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue incMdEntry2 = Mockito.mock(GroupValue.class);
        Mockito.when(incMdEntry1.getInt("RptSeq")).thenReturn(101);
        Mockito.when(incMdEntry2.getInt("RptSeq")).thenReturn(102);
        GroupValue[] incMdEntries = new GroupValue[] {incMdEntry1, incMdEntry2};
        Mockito.when(sequenceValidator.stopRecovering("SYMB:CETS")).thenReturn(incMdEntries);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(sequenceValidator).onIncrementalSeq("SYMB:CETS", 101);
        Mockito.verify(sequenceValidator).onIncrementalSeq("SYMB:CETS", 102);
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry1), Mockito.anyLong());
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry2), Mockito.anyLong());
    }

    @Test
    public void testSnapshot2FragmentsWithIncrementals() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message2.getInt("RptSeq")).thenReturn(100);

        GroupValue incMdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue incMdEntry2 = Mockito.mock(GroupValue.class);
        Mockito.when(incMdEntry1.getInt("RptSeq")).thenReturn(101);
        Mockito.when(incMdEntry2.getInt("RptSeq")).thenReturn(102);
        GroupValue[] incMdEntries = new GroupValue[] {incMdEntry1, incMdEntry2};
        Mockito.when(sequenceValidator.stopRecovering("SYMB:CETS")).thenReturn(incMdEntries);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(2)).onSnapshot(messageCaptor.capture(), Mockito.anyLong());
        Mockito.verify(sequenceValidator).onIncrementalSeq("SYMB:CETS", 101);
        Mockito.verify(sequenceValidator).onIncrementalSeq("SYMB:CETS", 102);
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry1), Mockito.anyLong());
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry2), Mockito.anyLong());

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
    }

    @Test
    public void testSnapshot3Fragments() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = Mockito.mock(Message.class);
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(3)).onSnapshot(messageCaptor.capture(), Mockito.anyLong());

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
        assert messageCaptor.getAllValues().get(2).getInt("MsgSeqNum") == 3;
    }

    @Test
    public void testSnapshot3FragmentsWithRecoveringInMiddle() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = Mockito.mock(Message.class);
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(1);

        Mockito.when(sequenceValidator.isRecovering("SYMB:CETS")).thenReturn(false);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        Mockito.when(sequenceValidator.isRecovering("SYMB:CETS")).thenReturn(true);

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.any(Message.class), Mockito.anyLong());
    }


    @Test
    public void testSnapshot3FragmentsOutOfOrder() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);

        Message message3 = Mockito.mock(Message.class);
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message3.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(0);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());
    }

    @Test
    public void testSnapshot3FragmentsOneMissing() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
    }

    @Test
    public void testSnapshot3FragmentsTwoTimes() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = Mockito.mock(Message.class);
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        Mockito.when(message.getLong("SendingTime")).thenReturn(2L);
        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(3)).onSnapshot(messageCaptor.capture(), Mockito.anyLong());

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
        assert messageCaptor.getAllValues().get(2).getInt("MsgSeqNum") == 3;
    }

    @Test
    public void testSnapshot4FragmentsOutOfOrder() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = Mockito.mock(Message.class);
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = Mockito.mock(Message.class);
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(0);

        Message message4 = Mockito.mock(Message.class);
        Mockito.when(message4.getInt("MsgSeqNum")).thenReturn(4);
        Mockito.when(message4.getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message4.getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message4.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message4.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message4), Mockito.anyLong());

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message4), Mockito.anyLong());

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message3), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onSnapshot(Mockito.eq(message4), Mockito.anyLong());

        snapshotProcessor.handleMessage(message4, context, coder);
        Mockito.verify(marketDataHandler, Mockito.times(4)).onSnapshot(messageCaptor.capture(), Mockito.anyLong());

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
        assert messageCaptor.getAllValues().get(2).getInt("MsgSeqNum") == 3;
        assert messageCaptor.getAllValues().get(3).getInt("MsgSeqNum") == 4;
    }
}
