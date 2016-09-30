import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.FieldValue;
import org.openfast.GroupValue;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SequenceArray;
import ru.ncapital.gateways.micexfast.messagehandlers.MessageHandlerType;

import java.util.concurrent.*;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by egore on 27.01.2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class OtherTests {
    ScheduledFuture<?> futureTask ;

    @Test
    public void testGetMdEntryTimeInUsec() {
        GroupValue mdEntry = Mockito.mock(GroupValue.class);

        Mockito.when(mdEntry.getValue("MDEntryTime")).thenReturn(Mockito.mock(FieldValue.class));
        Mockito.when(mdEntry.getValue("OrigTime")).thenReturn(Mockito.mock(FieldValue.class));
        Mockito.when(mdEntry.getInt("MDEntryTime")).thenReturn(110908000);
        Mockito.when(mdEntry.getInt("OrigTime")).thenReturn(555444);

        Assert.assertEquals(40148555444L, Utils.getEntryTimeInTodayMicros(mdEntry));
    }

    @Test
    public void testConvertToday() {
        long sendingTime = 20160526064844726L;
        long todayMicros = Utils.convertTodayToTodayMicros((sendingTime % 1_00_00_00_000L) * 1_000L);
        long ticks = Utils.convertTodayMicrosToTicks(todayMicros);
        long todayMicros2 = Utils.convertTicksToTodayMicros(ticks);
        long ticks2 = Utils.convertTodayToTicks((sendingTime % 1_00_00_00_000L) * 1_000L);
        System.out.println(todayMicros);
        System.out.println(ticks);
        System.out.println(todayMicros2);
        System.out.println(ticks2);

        assertEquals(ticks, ticks2);
        assertEquals(todayMicros, todayMicros2);
    }

    @Test
    public void testConvertTicks() {
        long ticks = 7805452000L;
        System.out.println(Utils.convertTodayMicrosToTodayString(ticks));
    }

    @Test
    public void testCurrentTimeInMillis() {
        System.out.println(Utils.currentTimeInTodayMicros());
    }

    @Test
    public void testSubstring() {
        String str = "AAA:AAA";
        assert str.substring(0, str.indexOf(':')).equals("AAA");
    }

    @Test
    public void testConvertTodayToTicksAndBack() {
        long millis = Utils.currentTimeInTodayMicros() / 1000L;
        long micros = 667L;
        long ticks = Utils.convertTodayMicrosToTicks(millis * 1000L + micros);

        System.out.println(millis * 1000L + micros);
        System.out.println(ticks);
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

    @Test
    public void testMessageHandlerType() {
        assert MessageHandlerType.ORDER_LIST.equals("OrderList");
        assert MessageHandlerType.STATISTICS.equals("Statistics");
        assert MessageHandlerType.PUBLIC_TRADES.equals("PublicTrades");
    }

    @Test
    public void testConversion() {

    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();

        class Task implements Runnable {

            private String id;

            private Task(String id) {
                this.id = id;
            }

            @Override
            public void run() {
                System.out.println("STARTED " + id);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("FINISHED " + id);
            }
        }

        for (int i = 0; i < 10; ++i) {
            executor.execute(new Task(String.valueOf(i + 1)));
        }

        executor.shutdown();
    }
}
