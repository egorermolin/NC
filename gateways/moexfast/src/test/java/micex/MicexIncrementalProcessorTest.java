package micex;

import com.google.inject.Guice;
import com.google.inject.Key;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.*;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.micexfast.MicexGatewayModule;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexIncrementalProcessor;
import ru.ncapital.gateways.moexfast.MarketDataManager;
import ru.ncapital.gateways.moexfast.NullGatewayConfiguration;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import static org.mockito.Mockito.mock;

/**
 * Created by egore on 1/13/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class MicexIncrementalProcessorTest {
    @Mock
    private Context context;

    @Mock
    private Coder coder;

    @Mock
    private IMessageHandler<String> marketDataHandler;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private IMessageSequenceValidator<String> sequenceValidator =
             Guice.createInjector(new MicexGatewayModule())
            .getInstance(new Key<MessageSequenceValidatorFactory<String>>(){})
            .createMessageSequenceValidatorForOrderList();

    private IncrementalProcessor<String> incrementalProcessor;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        ((MessageSequenceValidator) sequenceValidator).setMarketDataManager(mock(MarketDataManager.class));

        incrementalProcessor = new MicexIncrementalProcessor(marketDataHandler, sequenceValidator, new NullGatewayConfiguration());
        incrementalProcessor.setIsPrimary(true);

        Mockito.when(marketDataHandler.isAllowedUpdate("SYMB;CETS")).thenReturn(true);
        Mockito.when(marketDataHandler.isAllowedUpdate("SYMB2;CETS")).thenReturn(true);
    }

    private Message getIncermentalMock(int seqNum, int numMdEntries) {
        Message message = mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(seqNum);
        Mockito.when(message.getString("MessageType")).thenReturn("X");
        Mockito.when(message.getLong("SendingTime")).thenReturn(123L);

        SequenceValue mdEntries = mock(SequenceValue.class);
        Mockito.when(message.getSequence("GroupMDEntries")).thenReturn(mdEntries);
        Mockito.when(mdEntries.getLength()).thenReturn(numMdEntries);

        for (int i = 0; i < numMdEntries; ++i) {
            GroupValue mdEntry = mock(GroupValue.class);
            Mockito.when(mdEntries.get(i)).thenReturn(mdEntry);
        }

        return message;
    }

    @Test
    public void testIncrementalNotFirst() {
        assert !sequenceValidator.isRecovering("SYMB;CETS", 101, false);

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert sequenceValidator.isRecovering("SYMB;CETS", 101, true);
        assert sequenceValidator.isRecovering("SYMB;CETS", 102, false);
    }

    @Test
    public void testIncrementalFirst() {
        assert !sequenceValidator.isRecovering("SYMB;CETS", 1, false);

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(1);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert !sequenceValidator.isRecovering("SYMB;CETS", 2, false);
    }

    @Test
    public void testIncremental() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.getRecovering();
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        message = getIncermentalMock(1, 1);
        GroupValue entry2 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert !sequenceValidator.isRecovering("SYMB;CETS", 100, false);
        assert !sequenceValidator.isRecovering("SYMB;CETS", 101, false);
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
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getSequence("GroupMDEntries").get(1).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert !sequenceValidator.isRecovering("SYMB;CETS", 100, true);
        assert !sequenceValidator.isRecovering("SYMB2;CETS", 200, true);
        assert !sequenceValidator.isRecovering("SYMB;CETS", 101, false);
        assert !sequenceValidator.isRecovering("SYMB2;CETS", 201, false);
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
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert sequenceValidator.isRecovering("SYMB;CETS", 102, false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(2)).flushIncrementals();

        assert !sequenceValidator.isRecovering("SYMB;CETS", 102, false);
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
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message1.getSequence("GroupMDEntries").get(1).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message1.getSequence("GroupMDEntries").get(1).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message1.getSequence("GroupMDEntries").get(1).getInt("RptSeq")).thenReturn(103);
        Mockito.when(message1.getSequence("GroupMDEntries").get(1).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry12), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering("SYMB;CETS", 103, false);

        // ===

        Message message2 = getIncermentalMock(1, 2);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);
        GroupValue entry22 = message2.getSequence("GroupMDEntries").get(1);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getSequence("GroupMDEntries").get(1).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(1).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(1).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message2.getSequence("GroupMDEntries").get(1).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry12), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry22), Mockito.any(PerformanceData.class));

        assert !sequenceValidator.isRecovering("SYMB;CETS", 102, false);
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
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering("SYMB;CETS", 103, false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));

        assert sequenceValidator.isRecovering("SYMB;CETS", 101, false);

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
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering("SYMB;CETS", 101, false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering("SYMB;CETS", 101, false);

        // ===

        Message message3 = getIncermentalMock(2, 1);
        GroupValue entry31 = message3.getSequence("GroupMDEntries").get(0);

        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message3, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry31), Mockito.any(PerformanceData.class));

        assert !sequenceValidator.isRecovering("SYMB;CETS", 102, false);

        // ===

        Message message4 = getIncermentalMock(2, 1);
        GroupValue entry41 = message4.getSequence("GroupMDEntries").get(0);

        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(entry41), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering("SYMB;CETS", 102, false);
    }

    @Ignore
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
            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 1);
            incrementalProcessor.handleMessage(message1, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(i % 2)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering("SYMB;CETS", 99 + i + 1, false);

            // ===

            Message message2 = getIncermentalMock(i, 1);
            GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 0);
            incrementalProcessor.handleMessage(message2, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(1 - i % 2)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering("SYMB;CETS", 99 + i + 1, false);
        }

        // ===

        Message message3 = getIncermentalMock(1, 1);
        GroupValue entry31 = message3.getSequence("GroupMDEntries").get(0);

        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(1);
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message3, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry31), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering("SYMB;CETS", 2, false);

        // ===

        Message message4 = getIncermentalMock(1, 1);
        GroupValue entry41 = message4.getSequence("GroupMDEntries").get(0);

        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(1);
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry41), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering("SYMB;CETS", 2, false);

        sequenceValidator.onSnapshotSeq("SYMB;CETS", 1);
        assert sequenceValidator.stopRecovering("SYMB;CETS").length == 0;
    }

    @Ignore
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
            Mockito.when(message1.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 1);
            incrementalProcessor.handleMessage(message1, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(i % 2)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering("SYMB;CETS", 99 + i + 1, false);

            // ===

            Message message2 = getIncermentalMock(i, 1);
            GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);
            Mockito.when(message2.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 0);
            incrementalProcessor.handleMessage(message2, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(1 - i % 2)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering("SYMB;CETS", 99 + i + 1, false);
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
        Mockito.when(message1.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering("SYMB;CETS", 101, false);

        // ===

        Message message2 = getIncermentalMock(2, 1);
        GroupValue entry21 = message2.getSequence("GroupMDEntries").get(0);

        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message2.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering("SYMB;CETS", 102, false);

        // ===

        Message message3 = getIncermentalMock(3, 1);
        GroupValue entry31 = message3.getSequence("GroupMDEntries").get(0);

        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(102);
        Mockito.when(message3.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message3, context, coder);

        InOrder marketDataHandlerInOrder = Mockito.inOrder(marketDataHandler);
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry31), Mockito.any(PerformanceData.class));

        assert !sequenceValidator.isRecovering("SYMB;CETS", 103, false);

        // ===

        Message message4 = getIncermentalMock(3, 1);
        GroupValue entry41 = message4.getSequence("GroupMDEntries").get(0);

        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(102);
        Mockito.when(message4.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry41), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering("SYMB;CETS", 103, false);
    }

    @Test
    public void testTradeId() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("DealNumber")).thenReturn("1000");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("DealNumber")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();
        Mockito.verify(message.getSequence("GroupMDEntries").get(0), Mockito.times(0)).setString("DealNumber", null);

        message = getIncermentalMock(2, 1);
        GroupValue entry2 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("DealNumber")).thenReturn("1000");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("DealNumber")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(2)).flushIncrementals();
        Mockito.verify(entry2, Mockito.times(1)).setString("DealNumber", null);

        assert !sequenceValidator.isRecovering("SYMB;CETS", 102, false);
    }

    @Test
    public void testTradeId2() {
        sequenceValidator.startRecovering("SYMB;CETS");
        sequenceValidator.onSnapshotSeq("SYMB;CETS", 99);
        assert sequenceValidator.stopRecovering("SYMB;CETS") == null;

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("DealNumber")).thenReturn("1000");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("DealNumber")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();
        Mockito.verify(message.getSequence("GroupMDEntries").get(0), Mockito.times(0)).setString("DealNumber", null);

        message = getIncermentalMock(2, 1);
        GroupValue entry2 = message.getSequence("GroupMDEntries").get(0);

        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("Symbol")).thenReturn("SYMB");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("TradingSessionID")).thenReturn("CETS");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getString("DealNumber")).thenReturn("1001");
        Mockito.when(message.getSequence("GroupMDEntries").get(0).getValue("DealNumber")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(2)).flushIncrementals();
        Mockito.verify(entry2, Mockito.times(0)).setString("DealNumber", null);

        assert !sequenceValidator.isRecovering("SYMB;CETS", 102, false);
    }
}
