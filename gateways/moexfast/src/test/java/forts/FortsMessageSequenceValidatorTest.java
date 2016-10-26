package forts;

import com.google.inject.Guice;
import com.google.inject.Key;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.fortsfast.FortsGatewayModule;
import ru.ncapital.gateways.fortsfast.FortsMarketDataManager;
import ru.ncapital.gateways.micexfast.MicexGatewayModule;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorForOrderList;
import ru.ncapital.gateways.moexfast.performance.PerformanceData;

/**
 * Created by egore on 1/14/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class FortsMessageSequenceValidatorTest {
    @Mock
    private FortsMarketDataManager marketDataManager;

    private MessageSequenceValidator<Long> sequenceValidator = new MessageSequenceValidatorForOrderList<>();

    @Before
    public void setup() {
        sequenceValidator.setMarketDataManager(marketDataManager);
    }

    private Message getIncrementalMock(int seqNum, int numMdEntries) {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getInt("MsgSeqNum")).thenReturn(seqNum);

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
    public void testRecoveringSnapshot() {
        assert sequenceValidator.isRecovering(380922L, true);
        sequenceValidator.onSnapshotSeq(380922L, 100);
        sequenceValidator.stopRecovering(380922L);
        assert !sequenceValidator.isRecovering(380922L, true);
    }

    @Test
    public void testRecoveringIncremental() {
        assert !sequenceValidator.isRecovering(380922L, false);
        sequenceValidator.startRecovering(380922L);
        assert sequenceValidator.isRecovering(380922L, false);
        sequenceValidator.onSnapshotSeq(380922L, 100);
        sequenceValidator.stopRecovering(380922L);
        assert !sequenceValidator.isRecovering(380922L, false);
    }

    @Test
    public void testOnSnapshot() {
        assert sequenceValidator.onSnapshotSeq(380922L, 10);
    }

    @Test
    public void testOnIncremental() {
        assert sequenceValidator.onSnapshotSeq(380922L, 10);
        assert sequenceValidator.onIncrementalSeq(380922L, 11);
        assert sequenceValidator.onIncrementalSeq(380922L, 12);
        assert sequenceValidator.onIncrementalSeq(380922L, 13);
    }

    @Test
    public void testOnIncrementalOutOfSequence() {
        assert sequenceValidator.onSnapshotSeq(380922L, 10);
        assert !sequenceValidator.onIncrementalSeq(380922L, 12);
    }

    @Test
    public void testStoreIncermental() {
        GroupValue mdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry2 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry3 = Mockito.mock(GroupValue.class);

        sequenceValidator.startRecovering(380922L);
        sequenceValidator.storeIncremental(380922L, 100, mdEntry1, new PerformanceData());
        sequenceValidator.storeIncremental(380922L, 101, mdEntry2, new PerformanceData());
        sequenceValidator.storeIncremental(380922L, 102, mdEntry3, new PerformanceData());

        assert sequenceValidator.onSnapshotSeq(380922L, 100);
        StoredMdEntry[] storedMdEntries = sequenceValidator.stopRecovering(380922L);
        assert storedMdEntries[0].getMdEntry().equals(mdEntry2);
        assert storedMdEntries[1].getMdEntry().equals(mdEntry3);
        assert !sequenceValidator.isRecovering(380922L, false);
    }

    @Test
    public void testStoreIncermentalFailed() {
        GroupValue mdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry2 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry3 = Mockito.mock(GroupValue.class);

        sequenceValidator.startRecovering(380922L);
        sequenceValidator.storeIncremental(380922L, 100, mdEntry1, new PerformanceData());
        sequenceValidator.storeIncremental(380922L, 101, mdEntry2, new PerformanceData());
        sequenceValidator.storeIncremental(380922L, 104, mdEntry3, new PerformanceData());

        assert sequenceValidator.onSnapshotSeq(380922L, 100);
        assert sequenceValidator.stopRecovering(380922L) == null;
        assert sequenceValidator.isRecovering(380922L, false);
    }

}
