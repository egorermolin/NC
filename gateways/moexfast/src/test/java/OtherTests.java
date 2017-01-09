import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openfast.*;
import org.openfast.codec.Coder;
import org.openfast.template.MessageTemplate;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import ru.ncapital.gateways.moexfast.Utils;
import ru.ncapital.gateways.moexfast.connection.messageprocessors.SequenceArray;
import ru.ncapital.gateways.moexfast.messagehandlers.MessageHandlerType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by egore on 27.01.2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class OtherTests {
    ScheduledFuture<?> futureTask ;

    @Test
    public void testDecimal() {
        long mantissa = 6148123412L;
        int exponent = -5;

        DecimalValue dv = new DecimalValue(mantissa, exponent);

        System.out.println(dv.toString());
        System.out.println(dv.toBigDecimal().doubleValue());
    }

    @Ignore
    @Test
    public void testDecode() throws FileNotFoundException {
        MessageInputStream stream = new MessageInputStream(new InputStream() {
            private ByteBuffer data = ByteBuffer.wrap(PacketData.BYTES_PRODUCT_SNAPSHOT_START);

            @Override
            public int read() throws IOException {
                if (data.hasRemaining())
                    return data.get() & 0xFF;

                return -1;
            }
        });

        for (MessageTemplate template : new XMLMessageTemplateLoader().load(new FileInputStream("C:\\Users\\Egor\\Desktop\\RDDFastTemplates.xml"))) {
            System.out.println("Loaded " + template.getId() + " " + template.getName());
            stream.registerTemplate(Integer.valueOf(template.getId()), template);
        }

        stream.addMessageHandler(new MessageHandler() {
            @Override
            public void handleMessage(Message readMessage, Context context, Coder coder) {
                System.out.println(readMessage);
            }
        });

        stream.setBlockReader(MessageBlockReader.NULL);

        // stream.getContext().setTraceEnabled(true);

        while (true) {
            if (stream.readMessage() == null)
                break;
        }
    }

    @Test
    public void testGetMdEntryTimeInUsec() {
        GroupValue mdEntry = Mockito.mock(GroupValue.class);

        Mockito.when(mdEntry.getValue("MDEntryTime")).thenReturn(Mockito.mock(FieldValue.class));
        Mockito.when(mdEntry.getValue("OrigTime")).thenReturn(Mockito.mock(FieldValue.class));
        Mockito.when(mdEntry.getLong("MDEntryTime")).thenReturn(110908000L);
        Mockito.when(mdEntry.getInt("OrigTime")).thenReturn(555444);

        Assert.assertEquals(40148555444L, Utils.getEntryTimeInTodayMicros(mdEntry, Utils.SecondFractionFactor.MILLISECONDS));
    }

    @Test
    public void testGetMdEntryTimeInNsec() {
        GroupValue mdEntry = Mockito.mock(GroupValue.class);

        Mockito.when(mdEntry.getValue("MDEntryTime")).thenReturn(Mockito.mock(FieldValue.class));
        Mockito.when(mdEntry.getValue("OrigTime")).thenReturn(null);
        Mockito.when(mdEntry.getLong("MDEntryTime")).thenReturn(110908555444333L);

        Assert.assertEquals(40148555444L, Utils.getEntryTimeInTodayMicros(mdEntry, Utils.SecondFractionFactor.NANOSECONDS));
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
    }
/*
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
    }*/
}
