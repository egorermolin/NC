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
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.IMessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidator;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorFactory;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.sequencevalidators.MessageSequenceValidatorForOrderList;

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
    public void testRecovering() {
        assert sequenceValidator.isRecovering("SYMB");
        sequenceValidator.onSnapshotSeq("SYMB", 100);
        sequenceValidator.stopRecovering("SYMB");
        assert !sequenceValidator.isRecovering("SYMB");
    }

    @Test
    public void testRecovering2() {
        sequenceValidator.startRecovering("SYMB");
        assert sequenceValidator.isRecovering("SYMB");
        sequenceValidator.onSnapshotSeq("SYMB", 100);
        sequenceValidator.stopRecovering("SYMB");
        assert !sequenceValidator.isRecovering("SYMB");
    }

    @Test
    public void testOnSnapshot() {
        assert sequenceValidator.onSnapshotSeq("SYMB", 10);
    }

    @Test
    public void testOnIncremental() {
        assert sequenceValidator.onSnapshotSeq("SYMB", 10);
        assert sequenceValidator.onIncrementalSeq("SYMB", 11);
        assert sequenceValidator.onIncrementalSeq("SYMB", 12);
        assert sequenceValidator.onIncrementalSeq("SYMB", 13);
    }

    @Test
    public void testOnIncrementalOutOfSequence() {
        assert sequenceValidator.onSnapshotSeq("SYMB", 10);
        assert !sequenceValidator.onIncrementalSeq("SYMB", 12);
    }

    @Test
    public void testStoreIncermental() {
        GroupValue mdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry2 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry3 = Mockito.mock(GroupValue.class);

        sequenceValidator.startRecovering("SYMB");
        sequenceValidator.storeIncremental(mdEntry1, "SYMB", 100);
        sequenceValidator.storeIncremental(mdEntry2, "SYMB", 101);
        sequenceValidator.storeIncremental(mdEntry3, "SYMB", 102);

        assert sequenceValidator.onSnapshotSeq("SYMB", 100);
        assert Arrays.equals(sequenceValidator.stopRecovering("SYMB"), new GroupValue[]{mdEntry2, mdEntry3});
        assert !sequenceValidator.isRecovering("SYMB");
    }

    @Test
    public void testStoreIncermentalFailed() {
        GroupValue mdEntry1 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry2 = Mockito.mock(GroupValue.class);
        GroupValue mdEntry3 = Mockito.mock(GroupValue.class);

        sequenceValidator.startRecovering("SYMB");
        sequenceValidator.storeIncremental(mdEntry1, "SYMB", 100);
        sequenceValidator.storeIncremental(mdEntry2, "SYMB", 101);
        sequenceValidator.storeIncremental(mdEntry3, "SYMB", 104);

        assert sequenceValidator.onSnapshotSeq("SYMB", 100);
        assert sequenceValidator.stopRecovering("SYMB") == null;
        assert sequenceValidator.isRecovering("SYMB");
    }

}
