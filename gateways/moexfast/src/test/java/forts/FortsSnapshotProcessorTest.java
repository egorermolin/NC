package forts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.Context;
import org.openfast.FieldValue;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.codec.Coder;
import ru.ncapital.gateways.fortsfast.connection.messageprocessors.FortsSnapshotProcessor;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.MicexSnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SnapshotProcessor;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.moexfast.messagehandlers.IMessageHandler;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Created by egore on 1/11/16.
 */

@RunWith(MockitoJUnitRunner.class)
public class FortsSnapshotProcessorTest {
    @Mock
    private Context context;

    @Mock
    private Coder coder;
    
    @Mock
    private IMessageHandler<Long> marketDataHandler;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;
    
    @Mock
    private MessageSequenceValidator<Long> sequenceValidator;

    private SnapshotProcessor<Long> snapshotProcessor;

    @Before
    public void setup() {
        Mockito.when(marketDataHandler.getType()).thenReturn(MessageHandlerType.ORDER_LIST);

        snapshotProcessor = new FortsSnapshotProcessor(marketDataHandler, sequenceValidator);

        Mockito.when(sequenceValidator.isRecovering(380922L, true)).thenReturn(true);
        Mockito.when(sequenceValidator.isRecovering(380925L, true)).thenReturn(true);
        Mockito.when(sequenceValidator.onSnapshotSeq(Mockito.eq(380922L), Mockito.anyInt())).thenReturn(true);
        Mockito.when(sequenceValidator.onSnapshotSeq(Mockito.eq(380925L), Mockito.anyInt())).thenReturn(true);
        Mockito.when(sequenceValidator.getRecovering()).thenReturn(new Long[]{380922L, 380925L});

        Mockito.when(marketDataHandler.isAllowedUpdate(380922L)).thenReturn(true);
        Mockito.when(marketDataHandler.isAllowedUpdate(380925L)).thenReturn(true);
    }

    @Test
    public void testSnapshot() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler).onSnapshot(Mockito.eq(message));
        Mockito.verify(sequenceValidator).isRecovering(380922L, true);
        Mockito.verify(sequenceValidator).stopRecovering(380922L);
    }

    @Test
    public void testSnapshot2Symbols() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516870L);
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message2.getInt("RptSeq")).thenReturn(100);

        snapshotProcessor.handleMessage(message, context, coder);
        snapshotProcessor.handleMessage(message2, context, coder);

        Mockito.verify(marketDataHandler, times(2)).onSnapshot(messageCaptor.capture());
        Mockito.verify(sequenceValidator).isRecovering(380922L, true);
        Mockito.verify(sequenceValidator).isRecovering(380925L, true);
        Mockito.verify(sequenceValidator).stopRecovering(380922L);
        Mockito.verify(sequenceValidator).stopRecovering(380925L);
    }

    @Test
    public void testSnapshotNotRecovering() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97517570L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, Mockito.never()).onSnapshot(Mockito.eq(message));
        Mockito.verify(sequenceValidator, Mockito.never()).onSnapshotSeq(Mockito.eq(380927L), Mockito.anyInt());
    }

    @Test
    public void testSnapshotDuplicateStart() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);
        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, times(1)).onSnapshot(Mockito.eq(message));
    }

    @Test
    public void testSnapshotDuplicateStart2() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.when(message.getLong("SendingTime")).thenReturn(2L);
        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, times(2)).onSnapshot(Mockito.eq(message));
    }

    @Test
    public void testSnapshotDuplicate() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(marketDataHandler, times(1)).onSnapshot(Mockito.eq(message));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSnapshotWithIncrementals() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        StoredMdEntry<String> incStoredMdEntry1 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        StoredMdEntry<String> incStoredMdEntry2 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        Mockito.when(incStoredMdEntry1.getSequenceNumber()).thenReturn(101);
        Mockito.when(incStoredMdEntry2.getSequenceNumber()).thenReturn(102);
        Mockito.when(incStoredMdEntry1.isLastFragment()).thenReturn(true);
        Mockito.when(incStoredMdEntry2.isLastFragment()).thenReturn(true);
        Mockito.when(incStoredMdEntry1.isLastEntryInTransaction()).thenReturn(true);
        Mockito.when(incStoredMdEntry2.isLastEntryInTransaction()).thenReturn(true);
        GroupValue incMdEntry1 = mock(GroupValue.class);
        GroupValue incMdEntry2 = mock(GroupValue.class);
        Mockito.when(incStoredMdEntry1.getMdEntry()).thenReturn(incMdEntry1);
        Mockito.when(incStoredMdEntry2.getMdEntry()).thenReturn(incMdEntry2);
        StoredMdEntry<Long>[] incStoredMdEntries = (StoredMdEntry<Long>[]) new StoredMdEntry[] {incStoredMdEntry1, incStoredMdEntry2};
        Mockito.when(sequenceValidator.stopRecovering(380922L)).thenReturn(incStoredMdEntries);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 101);
        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 102);
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, times(3)).flushIncrementals();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSnapshotWithIncrementals2() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        StoredMdEntry<String> incStoredMdEntry1 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        StoredMdEntry<String> incStoredMdEntry2 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        Mockito.when(incStoredMdEntry1.getSequenceNumber()).thenReturn(101);
        Mockito.when(incStoredMdEntry2.getSequenceNumber()).thenReturn(102);
        Mockito.when(incStoredMdEntry1.isLastFragment()).thenReturn(true);
        Mockito.when(incStoredMdEntry2.isLastFragment()).thenReturn(false);
        Mockito.when(incStoredMdEntry1.isLastEntryInTransaction()).thenReturn(true);
        Mockito.when(incStoredMdEntry2.isLastEntryInTransaction()).thenReturn(false);
        GroupValue incMdEntry1 = mock(GroupValue.class);
        GroupValue incMdEntry2 = mock(GroupValue.class);
        Mockito.when(incStoredMdEntry1.getMdEntry()).thenReturn(incMdEntry1);
        Mockito.when(incStoredMdEntry2.getMdEntry()).thenReturn(incMdEntry2);
        StoredMdEntry<Long>[] incStoredMdEntries = (StoredMdEntry<Long>[]) new StoredMdEntry[] {incStoredMdEntry1, incStoredMdEntry2};
        Mockito.when(sequenceValidator.stopRecovering(380922L)).thenReturn(incStoredMdEntries);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 101);
        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 102);
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, times(1)).flushIncrementals();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSnapshotWithIncrementals3() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        StoredMdEntry<String> incStoredMdEntry1 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        StoredMdEntry<String> incStoredMdEntry2 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        Mockito.when(incStoredMdEntry1.getSequenceNumber()).thenReturn(101);
        Mockito.when(incStoredMdEntry2.getSequenceNumber()).thenReturn(102);
        Mockito.when(incStoredMdEntry1.isLastFragment()).thenReturn(false);
        Mockito.when(incStoredMdEntry2.isLastFragment()).thenReturn(false);
        Mockito.when(incStoredMdEntry1.isLastEntryInTransaction()).thenReturn(false);
        Mockito.when(incStoredMdEntry2.isLastEntryInTransaction()).thenReturn(false);
        GroupValue incMdEntry1 = mock(GroupValue.class);
        GroupValue incMdEntry2 = mock(GroupValue.class);
        Mockito.when(incStoredMdEntry1.getMdEntry()).thenReturn(incMdEntry1);
        Mockito.when(incStoredMdEntry2.getMdEntry()).thenReturn(incMdEntry2);
        StoredMdEntry<Long>[] incStoredMdEntries = (StoredMdEntry<Long>[]) new StoredMdEntry[] {incStoredMdEntry1, incStoredMdEntry2};
        Mockito.when(sequenceValidator.stopRecovering(380922L)).thenReturn(incStoredMdEntries);

        snapshotProcessor.handleMessage(message, context, coder);

        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 101);
        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 102);
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, times(0)).flushIncrementals();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSnapshot2FragmentsWithIncrementals() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getInt("RptSeq")).thenReturn(100);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);
        Mockito.when(message2.getInt("RptSeq")).thenReturn(100);

        StoredMdEntry<String> incStoredMdEntry1 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        StoredMdEntry<String> incStoredMdEntry2 = (StoredMdEntry<String>) mock(StoredMdEntry.class);
        Mockito.when(incStoredMdEntry1.getSequenceNumber()).thenReturn(101);
        Mockito.when(incStoredMdEntry2.getSequenceNumber()).thenReturn(102);
        Mockito.when(incStoredMdEntry1.isLastFragment()).thenReturn(true);
        Mockito.when(incStoredMdEntry2.isLastFragment()).thenReturn(true);
        Mockito.when(incStoredMdEntry1.isLastEntryInTransaction()).thenReturn(true);
        Mockito.when(incStoredMdEntry2.isLastEntryInTransaction()).thenReturn(true);
        GroupValue incMdEntry1 = mock(GroupValue.class);
        GroupValue incMdEntry2 = mock(GroupValue.class);
        Mockito.when(incStoredMdEntry1.getMdEntry()).thenReturn(incMdEntry1);
        Mockito.when(incStoredMdEntry2.getMdEntry()).thenReturn(incMdEntry2);
        StoredMdEntry<Long>[] incStoredMdEntries = (StoredMdEntry<Long>[]) new StoredMdEntry[] {incStoredMdEntry1, incStoredMdEntry2};
        Mockito.when(sequenceValidator.stopRecovering(380922L)).thenReturn(incStoredMdEntries);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, times(2)).onSnapshot(messageCaptor.capture());
        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 101);
        Mockito.verify(sequenceValidator).onIncrementalSeq(380922L, 102);
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry1), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler).onIncremental(Mockito.eq(incMdEntry2), Mockito.any(PerformanceData.class));
        Mockito.verify(marketDataHandler, times(3)).flushIncrementals();

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
    }

    @Test
    public void testSnapshot3Fragments() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = mock(Message.class);
        Mockito.when(message3.getString("MessageType")).thenReturn("W");
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, times(3)).onSnapshot(messageCaptor.capture());

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
        assert messageCaptor.getAllValues().get(2).getInt("MsgSeqNum") == 3;
    }

    @Test
    public void testSnapshot3FragmentsWithRecoveringInMiddle() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = mock(Message.class);
        Mockito.when(message3.getString("MessageType")).thenReturn("W");
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(1);

        Mockito.when(sequenceValidator.isRecovering(380922L, true)).thenReturn(false);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        Mockito.when(sequenceValidator.isRecovering(380922L, true)).thenReturn(true);

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.any(Message.class));
    }


    @Test
    public void testSnapshot3FragmentsOutOfOrder() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);

        Message message3 = mock(Message.class);
        Mockito.when(message3.getString("MessageType")).thenReturn("W");
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message3.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message3.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message3.getInt("LastFragment")).thenReturn(0);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));
    }

    @Test
    public void testSnapshot3FragmentsOneMissing() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
    }

    @Test
    public void testSnapshot3FragmentsTwoTimes() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = mock(Message.class);
        Mockito.when(message3.getString("MessageType")).thenReturn("W");
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        Mockito.when(message.getLong("SendingTime")).thenReturn(2L);
        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, times(3)).onSnapshot(messageCaptor.capture());

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
        assert messageCaptor.getAllValues().get(2).getInt("MsgSeqNum") == 3;
    }

    @Test
    public void testSnapshot4FragmentsOutOfOrder() {
        Message message = mock(Message.class);
        Mockito.when(message.getString("MessageType")).thenReturn("W");
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(1);
        Mockito.when(message.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message.getInt("RouteFirst")).thenReturn(1);
        Mockito.when(message.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message.getInt("LastFragment")).thenReturn(0);
        Mockito.when(message.getLong("SendingTime")).thenReturn(1L);

        Message message2 = mock(Message.class);
        Mockito.when(message2.getString("MessageType")).thenReturn("W");
        Mockito.when(message2.getInt("MsgSeqNum")).thenReturn(2);
        Mockito.when(message2.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message2.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message2.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message2.getInt("LastFragment")).thenReturn(0);

        Message message3 = mock(Message.class);
        Mockito.when(message3.getString("MessageType")).thenReturn("W");
        Mockito.when(message3.getInt("MsgSeqNum")).thenReturn(3);
        Mockito.when(message3.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message3.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message3.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message3.getValue("LastFragment")).thenReturn(mock(FieldValue.class));
        Mockito.when(message3.getInt("LastFragment")).thenReturn(0);

        Message message4 = mock(Message.class);
        Mockito.when(message4.getString("MessageType")).thenReturn("W");
        Mockito.when(message4.getInt("MsgSeqNum")).thenReturn(4);
        Mockito.when(message4.getLong("SecurityID")).thenReturn(97516102L);
        Mockito.when(message4.getValue("RouteFirst")).thenReturn(mock(FieldValue.class));
        Mockito.when(message4.getInt("RouteFirst")).thenReturn(0);
        Mockito.when(message4.getInt("LastFragment")).thenReturn(1);

        snapshotProcessor.handleMessage(message, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message4));

        snapshotProcessor.handleMessage(message3, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message4));

        snapshotProcessor.handleMessage(message2, context, coder);
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message2));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message3));
        Mockito.verify(marketDataHandler, times(0)).onSnapshot(Mockito.eq(message4));

        snapshotProcessor.handleMessage(message4, context, coder);
        Mockito.verify(marketDataHandler, times(4)).onSnapshot(messageCaptor.capture());

        assert messageCaptor.getAllValues().get(0).getInt("MsgSeqNum") == 1;
        assert messageCaptor.getAllValues().get(1).getInt("MsgSeqNum") == 2;
        assert messageCaptor.getAllValues().get(2).getInt("MsgSeqNum") == 3;
        assert messageCaptor.getAllValues().get(3).getInt("MsgSeqNum") == 4;
    }
}
