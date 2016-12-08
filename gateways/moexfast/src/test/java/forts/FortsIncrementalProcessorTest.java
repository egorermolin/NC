package forts;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.*;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.fortsfast.FortsMarketDataManager;
import ru.ncapital.gateways.fortsfast.connection.messageprocessors.FortsIncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.IncrementalProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorForOrderList;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import static org.mockito.Mockito.mock;

/**
 * Created by egore on 1/13/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class FortsIncrementalProcessorTest {
    @Mock
    private Context context;

    @Mock
    private Coder coder;

    @Mock
    private IMessageHandler<Long> marketDataHandler;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    @Mock
    private FortsMarketDataManager marketDataManager;

    private MessageSequenceValidator<Long> sequenceValidator = new MessageSequenceValidatorForOrderList<>();

    private IncrementalProcessor<Long> incrementalProcessor;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        incrementalProcessor = new FortsIncrementalProcessor(marketDataHandler, sequenceValidator);
        incrementalProcessor.setIsPrimary(true);

        sequenceValidator.setMarketDataManager(marketDataManager);

        Mockito.when(marketDataHandler.isAllowedUpdate(380922L)).thenReturn(true);
        Mockito.when(marketDataHandler.isAllowedUpdate(380925L)).thenReturn(true);
    }

    private Message getIncermentalMock(int seqNum, int numMdEntries) {
        Message message = mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(seqNum);
        Mockito.when(message.getString("MessageType")).thenReturn("X");
        Mockito.when(message.getLong("SendingTime")).thenReturn(123L);

        SequenceValue mdEntries = mock(SequenceValue.class);
        Mockito.when(message.getSequence("MDEntries")).thenReturn(mdEntries);
        Mockito.when(mdEntries.getLength()).thenReturn(numMdEntries);

        for (int i = 0; i < numMdEntries; ++i) {
            GroupValue mdEntry = mock(GroupValue.class);
            Mockito.when(mdEntries.get(i)).thenReturn(mdEntry);
        }

        return message;
    }

    @Test
    public void testIncremental() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.getRecovering();
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("MDEntries").get(0);

        Mockito.when(message.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        message = getIncermentalMock(1, 1);
        GroupValue entry2 = message.getSequence("MDEntries").get(0);

        Mockito.when(message.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert !sequenceValidator.isRecovering(380922L, 101, false);
    }


    @Test
    public void testIncremental2() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        Message message = getIncermentalMock(1, 1);
        GroupValue entry1 = message.getSequence("MDEntries").get(0);

        Mockito.when(message.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(380922L);
        Mockito.when(message.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        message = getIncermentalMock(1, 1);
        GroupValue entry2 = message.getSequence("MDEntries").get(0);

        Mockito.when(message.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(380922L);
        Mockito.when(message.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert !sequenceValidator.isRecovering(380922L, 101, false);
    }

    @Test
    public void testIncremental2Symbols() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        sequenceValidator.startRecovering(380925L);
        sequenceValidator.onSnapshotSeq(380925L, 199);
        assert sequenceValidator.stopRecovering(380925L) == null;

        Message message = getIncermentalMock(1, 2);
        GroupValue entry1 = message.getSequence("MDEntries").get(0);
        GroupValue entry2 = message.getSequence("MDEntries").get(1);

        Mockito.when(message.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getSequence("MDEntries").get(1).getLong("SecurityID")).thenReturn(97516870L);
        Mockito.when(message.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getSequence("MDEntries").get(1).getInt("RptSeq")).thenReturn(200);
        Mockito.when(message.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getSequence("MDEntries").get(1).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();

        assert !sequenceValidator.isRecovering(380922L, 101, false);
        assert !sequenceValidator.isRecovering(380925L, 201, false);
    }

    @Test
    public void testIncrementalOutOfSequence() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        // ===

        Message message1 = getIncermentalMock(2, 1);
        GroupValue entry11 = message1.getSequence("MDEntries").get(0);

        Mockito.when(message1.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message1.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message1.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).flushIncrementals();
        assert sequenceValidator.isRecovering(380922L, 102, false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("MDEntries").get(0);

        Mockito.when(message2.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(2)).flushIncrementals();

        assert !sequenceValidator.isRecovering(380922L, 102, false);
    }

    @Test
    public void testIncrementalOutOfSequence2() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        // ===

        Message message1 = getIncermentalMock(2, 2);
        GroupValue entry11 = message1.getSequence("MDEntries").get(0);
        GroupValue entry12 = message1.getSequence("MDEntries").get(0);

        Mockito.when(message1.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message1.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(102);
        Mockito.when(message1.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message1.getSequence("MDEntries").get(1).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message1.getSequence("MDEntries").get(1).getInt("RptSeq")).thenReturn(103);
        Mockito.when(message1.getSequence("MDEntries").get(1).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry12), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering(380922L, 104, false);

        // ===

        Message message2 = getIncermentalMock(1, 2);
        GroupValue entry21 = message2.getSequence("MDEntries").get(0);
        GroupValue entry22 = message2.getSequence("MDEntries").get(1);

        Mockito.when(message2.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getSequence("MDEntries").get(1).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getSequence("MDEntries").get(1).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message2.getSequence("MDEntries").get(1).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry12), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry22), Mockito.any(PerformanceData.class));

        assert !sequenceValidator.isRecovering(380922L, 104, false);
    }

    @Test
    public void testIncrementalOutOfSequenceFailed() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        // ===

        Message message1 = getIncermentalMock(3, 1);
        GroupValue entry11 = message1.getSequence("MDEntries").get(0);

        Mockito.when(message1.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message1.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(102);
        Mockito.when(message1.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering(380922L, 103, false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("MDEntries").get(0);

        Mockito.when(message2.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(1)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));

        assert sequenceValidator.isRecovering(380922L, 103, false);

        // ===

        sequenceValidator.onSnapshotSeq(380922L, 102);
        assert sequenceValidator.stopRecovering(380922L).length == 0;
    }

    @Test
    public void testIncrementalWith2Channels() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        // ===

        Message message1 = getIncermentalMock(1, 1);
        GroupValue entry11 = message1.getSequence("MDEntries").get(0);

        Mockito.when(message1.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message1.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message1.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering(380922L, 102, false);

        // ===

        Message message2 = getIncermentalMock(1, 1);
        GroupValue entry21 = message2.getSequence("MDEntries").get(0);

        Mockito.when(message2.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message2.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering(380922L, 102, false);

        // ===

        Message message3 = getIncermentalMock(2, 1);
        GroupValue entry31 = message3.getSequence("MDEntries").get(0);

        Mockito.when(message3.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message3.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message3, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry31), Mockito.any(PerformanceData.class));

        assert !sequenceValidator.isRecovering(380922L, 102, false);

        // ===

        Message message4 = getIncermentalMock(2, 1);
        GroupValue entry41 = message4.getSequence("MDEntries").get(0);

        Mockito.when(message4.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message4.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message4.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(entry41), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering(380922L, 102, false);
    }

    @Ignore
    @Test
    public void testIncrementalWith2ChannelsWithFailover() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        // ===

        for (int i = 1; i < 150; ++i) {
            Message message1 = getIncermentalMock(i, 1);
            GroupValue entry11 = message1.getSequence("MDEntries").get(0);

            Mockito.when(message1.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
            Mockito.when(message1.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);
            Mockito.when(message1.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 1);
            incrementalProcessor.handleMessage(message1, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(i % 2)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering(380922L, 99 + i + 1, false);

            // ===

            Message message2 = getIncermentalMock(i, 1);
            GroupValue entry21 = message2.getSequence("MDEntries").get(0);

            Mockito.when(message2.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
            Mockito.when(message2.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);
            Mockito.when(message2.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 0);
            incrementalProcessor.handleMessage(message2, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(1 - i % 2)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering(380922L, 99 + i + 1, false);
        }

        // ===

        Message message3 = getIncermentalMock(1, 1);
        GroupValue entry31 = message3.getSequence("MDEntries").get(0);

        Mockito.when(message3.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(1);
        Mockito.when(message3.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message3, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry31), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering(380922L, 2, false);

        // ===

        Message message4 = getIncermentalMock(1, 1);
        GroupValue entry41 = message4.getSequence("MDEntries").get(0);

        Mockito.when(message4.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message4.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(1);
        Mockito.when(message4.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry41), Mockito.any(PerformanceData.class));
        assert sequenceValidator.isRecovering(380922L, 2, false);

        sequenceValidator.onSnapshotSeq(380922L, 1);
        assert sequenceValidator.stopRecovering(380922L).length == 0;
    }

    @Ignore
    @Test
    public void testIncrementalManyMessagesWith2Channels() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        for (int i = 1; i < 1000; ++i) {
            Message message1 = getIncermentalMock(i, 1);
            GroupValue entry11 = message1.getSequence("MDEntries").get(0);

            Mockito.when(message1.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
            Mockito.when(message1.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);
            Mockito.when(message1.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 1);
            incrementalProcessor.handleMessage(message1, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(i % 2)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering(380922L, 99 + i + 1, false);

            // ===

            Message message2 = getIncermentalMock(i, 1);
            GroupValue entry21 = message2.getSequence("MDEntries").get(0);

            Mockito.when(message2.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
            Mockito.when(message2.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(99 + i);
            Mockito.when(message2.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

            incrementalProcessor.setIsPrimary(i % 2 == 0);
            incrementalProcessor.handleMessage(message2, context, coder);

            Mockito.verify(marketDataHandler, Mockito.times(1 - i % 2)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
            assert !sequenceValidator.isRecovering(380922L, 99 + i + 1, false);
        }
    }

    @Test
    public void testIncrementalOutOfSequenceWith2Channels() {
        sequenceValidator.startRecovering(380922L);
        sequenceValidator.onSnapshotSeq(380922L, 99);
        assert sequenceValidator.stopRecovering(380922L) == null;

        // ===

        Message message1 = getIncermentalMock(1, 1);
        GroupValue entry11 = message1.getSequence("MDEntries").get(0);

        Mockito.when(message1.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message1.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(100);
        Mockito.when(message1.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message1, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering(380922L, 102, false);

        // ===

        Message message2 = getIncermentalMock(2, 1);
        GroupValue entry21 = message2.getSequence("MDEntries").get(0);

        Mockito.when(message2.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(101);
        Mockito.when(message2.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering(380922L, 102, false);

        // ===

        Message message3 = getIncermentalMock(3, 1);
        GroupValue entry31 = message3.getSequence("MDEntries").get(0);

        Mockito.when(message3.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(102);
        Mockito.when(message3.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(true);
        incrementalProcessor.handleMessage(message3, context, coder);

        InOrder marketDataHandlerInOrder = Mockito.inOrder(marketDataHandler);
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry11), Mockito.any(PerformanceData.class));
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry21), Mockito.any(PerformanceData.class));
        marketDataHandlerInOrder.verify(marketDataHandler).onIncremental(Mockito.eq(entry31), Mockito.any(PerformanceData.class));

        assert !sequenceValidator.isRecovering(380922L, 103, false);

        // ===

        Message message4 = getIncermentalMock(3, 1);
        GroupValue entry41 = message4.getSequence("MDEntries").get(0);

        Mockito.when(message4.getSequence("MDEntries").get(0).getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message4.getSequence("MDEntries").get(0).getInt("RptSeq")).thenReturn(102);
        Mockito.when(message4.getSequence("MDEntries").get(0).getValue("RptSeq")).thenReturn(mock(FieldValue.class));

        incrementalProcessor.setIsPrimary(false);
        incrementalProcessor.handleMessage(message4, context, coder);

        Mockito.verify(marketDataHandler, Mockito.times(0)).onIncremental(Mockito.eq(entry41), Mockito.any(PerformanceData.class));
        assert !sequenceValidator.isRecovering(380922L, 103, false);
    }
}
