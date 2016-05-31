import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import ru.ncapital.gateways.micexfast.GatewayModule;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.StoredMdEntry;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorForOrderList;
import ru.ncapital.gateways.micexfast.domain.PerformanceData;

import java.util.Arrays;

/**
 * Created by egore on 1/14/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageSequenceValidatorTest {

    IMessageSequenceValidator sequenceValidator = Guice.createInjector(new GatewayModule()).getInstance(MessageSequenceValidatorFactory.class).createMessageSequenceValidatorForOrderList();;

    @Before
    public void setup() {
    }

    private Message getIncermentalMock(int seqNum, int numMdEntries) {
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
        assert sequenceValidator.isRecovering("SYMB:CETS", true);
        sequenceValidator.onSnapshotSeq("SYMB:CETS", 100);
        sequenceValidator.stopRecovering("SYMB:CETS");
        assert !sequenceValidator.isRecovering("SYMB:CETS", true);
    }

    @Test
    public void testRecoveringIncremental() {
        assert !sequenceValidator.isRecovering("SYMB:CETS", false);
        sequenceValidator.startRecovering("SYMB:CETS");
        assert sequenceValidator.isRecovering("SYMB:CETS", false);
        sequenceValidator.onSnapshotSeq("SYMB:CETS", 100);
        sequenceValidator.stopRecovering("SYMB:CETS");
        assert !sequenceValidator.isRecovering("SYMB:CETS", false);
    }

    @Test
    public void testOnSnapshot() {
        assert sequenceValidator.onSnapshotSeq("SYMB:CETS", 10);
    }

    @Test
    public void testOnIncremental() {
        assert sequenceValidator.onSnapshotSeq("SYMB:CETS", 10);
        assert sequenceValidator.onIncrementalSeq("SYMB:CETS", 11);
        assert sequenceValidator.onIncrementalSeq("SYMB:CETS", 12);
        assert sequenceValidator.onIncrementalSeq("SYMB:CETS", 13);
    }

    @Test
    public void testOnIncrementalOutOfSequence() {
        assert sequenceValidator.onSnapshotSeq("SYMB:CETS", 10);
        assert !sequenceValidator.onIncrementalSeq("SYMB:CETS", 12);
    }

    @Test
    public void testStoreIncermental() {
        GroupValue mdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry2 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry3 = Mockito.mock(GroupValue.class);

        sequenceValidator.startRecovering("SYMB:CETS");
        sequenceValidator.storeIncremental("SYMB:CETS", 100, mdEntry1, new PerformanceData(0));
        sequenceValidator.storeIncremental("SYMB:CETS", 101, mdEntry2, new PerformanceData(0));
        sequenceValidator.storeIncremental("SYMB:CETS", 102, mdEntry3, new PerformanceData(0));

        assert sequenceValidator.onSnapshotSeq("SYMB:CETS", 100);
        StoredMdEntry[] storedMdEntries = sequenceValidator.stopRecovering("SYMB:CETS");
        assert storedMdEntries[0].getMdEntry().equals(mdEntry2);
        assert storedMdEntries[1].getMdEntry().equals(mdEntry3);
        assert !sequenceValidator.isRecovering("SYMB:CETS", false);
    }

    @Test
    public void testStoreIncermentalFailed() {
        GroupValue mdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry2 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry3 = Mockito.mock(GroupValue.class);

        sequenceValidator.startRecovering("SYMB:CETS");
        sequenceValidator.storeIncremental("SYMB:CETS", 100, mdEntry1, new PerformanceData(0));
        sequenceValidator.storeIncremental("SYMB:CETS", 101, mdEntry2, new PerformanceData(0));
        sequenceValidator.storeIncremental("SYMB:CETS", 104, mdEntry3, new PerformanceData(0));

        assert sequenceValidator.onSnapshotSeq("SYMB:CETS", 100);
        assert sequenceValidator.stopRecovering("SYMB:CETS") == null;
        assert sequenceValidator.isRecovering("SYMB:CETS", false);
    }

}
