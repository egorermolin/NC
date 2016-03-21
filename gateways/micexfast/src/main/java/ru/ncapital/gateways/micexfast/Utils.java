package ru.ncapital.gateways.micexfast;

import org.openfast.GroupValue;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by egore on 2/3/16.
 */
public class Utils {

    private static boolean USING_DOT_NET = true;

    public static final long JANUARY_1ST_1970_IN_MILLIS = 62135596800L * 1000L;

    public static volatile long TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970 = 0;

    static {
        try {
            new cli.System.DateTime();
        } catch (UnsatisfiedLinkError e) {
            USING_DOT_NET = false;
        } catch (NoClassDefFoundError e1) {
            USING_DOT_NET = false;
        }

        updateTodayInMillis();
    }

    public static void updateTodayInMillis() {
        Calendar cal = Calendar.getInstance();

        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970 = cal.getTimeInMillis();
    }

    public static long convertTodayMicrosToTicks(long millis, long micros) {
        return (JANUARY_1ST_1970_IN_MILLIS + TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970 + millis) * 10000L + micros * 10L;
    }

    public static long convertTicksToTodayMicros(long ticks) {
        long micros = (ticks / 10) % 1000L;
        long millis = (ticks / 10 / 1000L) - TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970 - JANUARY_1ST_1970_IN_MILLIS;
        return millis * 1000L + micros;
    }

    public static long convertTicksToToday(long ticks) {
        long micros = convertTicksToTodayMicros(ticks);
        long secondsTotal = micros / 1000L / 1000L;
        long seconds = secondsTotal % (60L * 60L) % 60L;
        long minutes = (secondsTotal / 60L) % 60L;
        long hours = (secondsTotal / 60L / 60L);

        return hours * 100L * 100L * 1000L * 1000L + minutes * 100L * 1000L * 1000L + seconds * 1000L * 1000L + micros % (1000L * 1000L);
    }

    public static long currentTimeInToday() {
        long millis = currentTimeInTodayMillis();
        long secondsTotal = millis / 1000L;
        long seconds = secondsTotal % (60L * 60L) % 60L;
        long minutes = (secondsTotal / 60L) % 60L;
        long hours = (secondsTotal / 60L / 60L);

        return hours * 100L * 100L * 1000L + minutes * 100L * 1000L + seconds * 1000L + millis % (1000L);
    }

    public static long currentTimeInTodayMillis() {
        return Calendar.getInstance().getTimeInMillis() - TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970;
    }

    public static long currentTimeInTicks() {
        if (USING_DOT_NET)
            return cli.System.DateTime.get_UtcNow().get_Ticks();
        else
            return (System.currentTimeMillis() + JANUARY_1ST_1970_IN_MILLIS) * 10000L;
    }

    public static long getEntryTimeInTicks(GroupValue mdEntry) {
        long entryTime = getEntryTimeInMicros(mdEntry);
        if (entryTime == 0)
            return Utils.currentTimeInTicks();

        return Utils.convertTodayMicrosToTicks(entryTime / 1000, entryTime % 1000);
    }

    public static long getEntryTimeInMicros(GroupValue mdEntry) {
        long entryTimeToday = mdEntry.getValue("MDEntryTime") != null ? mdEntry.getInt("MDEntryTime") / 1000 : 0;
        long entryTimeMicros = mdEntry.getValue("OrigTime") != null ? mdEntry.getInt("OrigTime") : 0;
        long entryTimeInMicros = 1000000 * (entryTimeToday / 10000 * 3600 + ((entryTimeToday / 100) % 100) * 60 + (entryTimeToday % 100)) + entryTimeMicros;

        return entryTimeInMicros;
    }
}
