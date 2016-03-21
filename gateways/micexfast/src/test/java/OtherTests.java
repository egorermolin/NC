import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.FieldValue;
import org.openfast.GroupValue;
import ru.ncapital.gateways.micexfast.Utils;
import ru.ncapital.gateways.micexfast.connection.messageprocessors.SequenceArray;

/**
 * Created by egore on 27.01.2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class OtherTests {
    @Test
    public void testGetMdEntryTimeInUsec() {
        GroupValue mdEntry = Mockito.mock(GroupValue.class);

        Mockito.when(mdEntry.getValue("MDEntryTime")).thenReturn(Mockito.mock(FieldValue.class));
        Mockito.when(mdEntry.getValue("OrigTime")).thenReturn(Mockito.mock(FieldValue.class));
        Mockito.when(mdEntry.getInt("MDEntryTime")).thenReturn(110908000);
        Mockito.when(mdEntry.getInt("OrigTime")).thenReturn(555444);

        Assert.assertEquals(40148555444L, Utils.getEntryTimeInMicros(mdEntry));
    }

    @Test
    public void testCurrentTimeInMillis() {
        System.out.println(Utils.currentTimeInTodayMillis());
    }

    @Test
    public void testSubstring() {
        String str = "AAA:AAA";
        assert str.substring(0, str.indexOf(':')).equals("AAA");
    }

    @Test
    public void testConvertTodayToTicksAndBack() {
        long millis = Utils.currentTimeInTodayMillis();
        long micros = 667L;
        long ticks = Utils.convertTodayMicrosToTicks(millis, micros);

        System.out.println(millis * 1000L + micros);
        System.out.println(ticks);
        System.out.println(Utils.convertTicksToToday(ticks));
        System.out.println(Utils.convertTicksToTodayMicros(ticks));
        assert Utils.convertTicksToTodayMicros(ticks) == (millis * 1000L + micros);
    }

    @Test
    public void testSequenceArray() {
        SequenceArray sa = new SequenceArray();
        assert sa.checkSequence(3) == SequenceArray.Result.OUT_OF_SEQUENCE;
        assert sa.checkSequence(2) == SequenceArray.Result.OUT_OF_SEQUENCE;
        assert sa.checkSequence(1) == SequenceArray.Result.IN_SEQUENCE;
        assert sa.checkSequence(4) == SequenceArray.Result.IN_SEQUENCE;

        assert sa.checkSequence(4) == SequenceArray.Result.DUPLICATE;
        assert sa.checkSequence(1) == SequenceArray.Result.DUPLICATE;
        assert sa.checkSequence(2) == SequenceArray.Result.DUPLICATE;
        assert sa.checkSequence(3) == SequenceArray.Result.DUPLICATE;

        assert sa.checkSequence(7) == SequenceArray.Result.OUT_OF_SEQUENCE;
        assert sa.checkSequence(6) == SequenceArray.Result.OUT_OF_SEQUENCE;
        assert sa.checkSequence(5) == SequenceArray.Result.IN_SEQUENCE;
        assert sa.checkSequence(8) == SequenceArray.Result.IN_SEQUENCE;

        assert sa.checkSequence(8) == SequenceArray.Result.DUPLICATE;
        assert sa.checkSequence(5) == SequenceArray.Result.DUPLICATE;
        assert sa.checkSequence(6) == SequenceArray.Result.DUPLICATE;
        assert sa.checkSequence(7) == SequenceArray.Result.DUPLICATE;
    }
}
