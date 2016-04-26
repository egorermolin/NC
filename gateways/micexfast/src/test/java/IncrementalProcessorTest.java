import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.Context;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.micexfast.GatewayModule;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.IncrementalProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorForOrderList;
import ru.ncapital.gateways.micexfast.messagehandlers.IMessageHandler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by egore on 1/13/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class IncrementalProcessorTest {
    @Mock
    private Context context;

    @Mock
    private Coder coder;

    @Mock
    private IMessageHandler marketDataHandler;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private IMessageSequenceValidator sequenceValidator;

    private IncrementalProcessor incrementalProcessor;

    @Before
    public void setup() {
        sequenceValidator = Guice.createInjector(new GatewayModule()).getInstance(MessageSequenceValidatorFactory.class).createMessageSequenceValidatorForOrderList();
        incrementalProcessor = new IncrementalProcessor(marketDataHandler, sequenceValidator);
        incrementalProcessor.setIsPrimary(true);
        Mockito.when(marketDataHandler.isAllowedUpdate("SYMB", "CETS")).thenReturn(true);
        Mockito.when(marketDataHandler.isAllowedUpdate("SYMB2", "CETS")).thenReturn(true);
    }

    private Message getIncermentalMock(int seqNum, int numMdEntries) {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(seqNum);
        Mockito.when(message.getString("MessageType")).thenReturn("X");
        Mockito.when(message.getLong("SendingTime")).thenReturn(123L);

        SequenceValue mdEntries = Mockito.mock(SequenceValue.class);
        Mockito.when(message.getSequence("GroupMDEntries")).thenReturn(mdEntries);
        Mockito.when(mdEntries.getLength()).thenReturn(numMdEntries);

        for (int i = 0; i < numMdEntries; ++i) {
            GroupValue mdEntry = Mockito.mock(GroupValue.class);
            Mockito.when(mdEntries.get(i)).thenReturn(mdEntry);
        }

        return message;
    }

    @Test
    public void testIncremental() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals(Mockito.eq(false), Mockito.anyLong());

        message = getIncermentalMock(1, 1);
        GroupValue entry2 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals(Mockito.eq(false), Mockito.anyLong());

        assert !sequenceValidator.isRecovering("SYMB;CETS", false);
    }

    @Test
    public void testIncremental2Symbols() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        sequenceValidator.startRecovering("SYMB2;CETS");
        sequenceValidator.onSnapshotSeq("SYMB2;CETS", 199);
        assert sequenceValidator.stopRecovering("SYMB2;CETS") == null;

        Message message = getIncermentalMock(1, 2);
        GroupValue entry1 = message.getSequence("GroupMDEntries").get(0);
        GroupValue entry2 = message.getSequence("GroupMDEntries").get(1);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(1).getString("Symbol")).thenReturn("SYMB2");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(1).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("GroupMDEntries").get(1).getInt("RptSeq")).thenReturn(200);

        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry2), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals(Mockito.eq(false), Mockito.anyLong());

        assert !sequenceValidator.isRecovering("SYMB;CETS", false);
        assert !sequenceValidator.isRecovering("SYMB2;CETS", false);
    }

    @Test
    public void testIncrementalOutOfSequence() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        // ===

        Message message1 = getIncermentalMock(2, 1);
        GroupValue entry11 = message1.getSequence("GroupMDEntries").get(0);

        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals(Mockito.eq(false), Mockito.anyLong());
        assert sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(2)).flushIncrementals(Mockito.eq(false), Mockito.anyLong());

        assert !sequenceValidator.isRecovering("SYMB;CETS", false);
    }

    @Test
    public void testIncrementalOutOfSequence2() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        // ===

        Message message1 = getIncermentalMock(2, 2);
        GroupValue entry11 = message1.getSequence("GroupMDEntries").get(0);
        GroupValue entry12 = message1.getSequence("GroupMDEntries").get(0);

        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(102);
        Mockito.when(message1.getSequence("GroupMDEntries").get(1).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message1.getSequence("GroupMDEntries").get(1).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message1.getSequence("GroupMDEntries").get(1).getInt("RptSeq")).thenReturn(103);

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry12), Mockito.anyLong());
        assert sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message2 = getIncermentalMock(1, 2);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);
        GroupValue entry22 = message2.getSequence("GroupMDEntries").get(1);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("GroupMDEntries").get(1).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(1).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(1).getInt("RptSeq")).thenReturn(101);

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry12), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry22), Mockito.anyLong());

        assert !sequenceValidator.isRecovering("SYMB;CETS", false);
    }

    @Test
    public void testIncrementalOutOfSequenceFailed() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        // ===

        Message message1 = getIncermentalMock(3, 1);
        GroupValue entry11 = message1.getSequence("GroupMDEntries").get(0);

        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(102);

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        assert sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());

        assert sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        sequenceValidator.onSnapshotSeq("SYMB;CETS", 102);
        assert sequenceValidator.stopRecovering("SYMB;CETS").length == 0;
    }

    @Test
    public void testIncrementalWith2Channels() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        // ===

        Message message1 = getIncermentalMock(1, 1);
        GroupValue entry11 = message1.getSequence("GroupMDEntries").get(0);

        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        assert !sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
        assert !sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message3 = getIncermentalMock(2, 1);
        GroupValue entry31 = message3.getSequence("GroupMDEntries").get(0);

        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message3, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry31), Mockito.anyLong());

        assert !sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message4 = getIncermentalMock(2, 1);
        GroupValue entry41 = message4.getSequence("GroupMDEntries").get(0);

        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(entry41), Mockito.anyLong());
        assert !sequenceValidator.isRecovering("SYMB;CETS", false);
    }

    @Test
    public void testIncrementalWith2ChannelsWithFailover() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        // ===

        for (int i = 1; i < 150; ++i) {
            Message message1 = getIncermentalMock(i, 1);
            GroupValue entry11 = message1.getSequence("GroupMDEntries").get(0);

            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);

            incrementalProcessor.setIsPrimary(i % 2 == 1);
            incrementalProcessor.handleMessage(message1, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(i % 2)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
            assert !sequenceValidator.isRecovering("SYMB;CETS", false);

            // ===

            Message message2 = getIncermentalMock(i, 1);
            GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);

            incrementalProcessor.setIsPrimary(i % 2 == 0);
            incrementalProcessor.handleMessage(message2, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(1 - i % 2)).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
            assert !sequenceValidator.isRecovering("SYMB;CETS", false);
        }

        // ===

        Message message3 = getIncermentalMock(1, 1);
        GroupValue entry31 = message3.getSequence("GroupMDEntries").get(0);

        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(1);

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message3, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry31), Mockito.anyLong());
        assert sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message4 = getIncermentalMock(1, 1);
        GroupValue entry41 = message4.getSequence("GroupMDEntries").get(0);

        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(1);

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry41), Mockito.anyLong());
        assert sequenceValidator.isRecovering("SYMB;CETS", false);

        sequenceValidator.onSnapshotSeq("SYMB;CETS", 1);
        assert sequenceValidator.stopRecovering("SYMB;CETS").length == 0;
    }

    @Test
    public void testIncrementalManyMessagesWith2Channels() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        for (int i = 1; i < 1000; ++i) {
            Message message1 = getIncermentalMock(i, 1);
            GroupValue entry11 = message1.getSequence("GroupMDEntries").get(0);

            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);

            incrementalProcessor.setIsPrimary(i % 2 == 1);
            incrementalProcessor.handleMessage(message1, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(i % 2)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
            assert !sequenceValidator.isRecovering("SYMB;CETS", false);

            // ===

            Message message2 = getIncermentalMock(i, 1);
            GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);

            incrementalProcessor.setIsPrimary(i % 2 == 0);
            incrementalProcessor.handleMessage(message2, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(1 - i % 2)).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
            assert !sequenceValidator.isRecovering("SYMB;CETS", false);
        }
    }

    @Test
    public void testIncrementalOutOfSequenceWith2Channels() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        // ===

        Message message1 = getIncermentalMock(1, 1);
        GroupValue entry11 = message1.getSequence("GroupMDEntries").get(0);

        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        assert !sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message2 = getIncermentalMock(2, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
        assert !sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message3 = getIncermentalMock(3, 1);
        GroupValue entry31 = message3.getSequence("GroupMDEntries").get(0);

        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(102);

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message3, context, coder);

        InOrder marketDataHandlerInOrder = Mockito.inOrder(marketDataHandler);
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry11), Mockito.anyLong());
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry21), Mockito.anyLong());
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry31), Mockito.anyLong());

        assert !sequenceValidator.isRecovering("SYMB;CETS", false);

        // ===

        Message message4 = getIncermentalMock(3, 1);
        GroupValue entry41 = message4.getSequence("GroupMDEntries").get(0);

        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(102);

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry41), Mockito.anyLong());
        assert !sequenceValidator.isRecovering("SYMB;CETS", false);
    }
}
