package ru.ncapital.gateways.micexfast;

import org.openfast.GroupValue;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by egore on 2/3/16.
 */
public class Utils {

    private static boolean USING_DOT_NET = true;

    public static final long JANUARY_1ST_1970_IN_MICROS = 62135596800L * 1000L;

    public static volatile long TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970 = 0;

    static {
        try {
            new cli.System.DateTime();
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
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

    public static String convertTicksToTodayString(long ticks) {
        return convertTodayMicrosToTodayString(convertTicksToTodayMicros(ticks));
    }

    public static String convertTodayMicrosToTodayString(long todayMicros) {
        int todaySeconds = (int) (todayMicros / 1_000_000L);
        int seconds = todaySeconds % 60;
        int todayMinutes = (todaySeconds - seconds) / 60;
        int minutes = todayMinutes % 60;
        int hours = (todayMinutes - minutes) / 60;

        return new StringBuilder()
                .append(hours < 10 ? "0" : "").append(hours).append(":")
                .append(minutes < 10 ? "0" : "").append(minutes).append(":")
                .append(seconds < 10 ? "0" : "").append(seconds).append(" UTC").toString();
    }

    /*
         CURRENT TICKS (ticks since 1 JAN 0001)
    */
    public static long currentTimeInTicks() {
        if (USING_DOT_NET)
            return cli.System.DateTime.get_UtcNow().get_Ticks();
        else
            return (System.currentTimeMillis() + JANUARY_1ST_1970_IN_MICROS) * 10000L;
    }
    /*
         CURRENT TODAY_MICROS (micros since midnight)
    */
    public static long currentTimeInTodayMicros() {
        return convertTicksToTodayMicros(currentTimeInTicks());
    }
    /*
        CONVERT TODAY_MICROS (micros since midnight) to TICKS (ticks since 1 JAN 0001)
    */
    public static long convertTodayMicrosToTicks(long micros) {
        return (JANUARY_1ST_1970_IN_MICROS + TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970 + micros / 1000L) * 10000L + (micros % 1000L) * 10L;
    }
    /*
        CONVERT TICKS (ticks since 1 JAN 0001) to TODAY_MICROS (micros since midnight)
    */
    public static long convertTicksToTodayMicros(long ticks) {
        long micros = (ticks / 10) % 1000L;
        long millis = (ticks / 10 / 1000L) - TODAY_IN_MILLIS_SINCE_JANUARY_1ST_1970 - JANUARY_1ST_1970_IN_MICROS;
        return millis * 1000L + micros;
    }

    /*
         CONVERT TODAY (HHMMSSsssmmm) to TODAY_MICROS (micros since midnight)
    */
    public static long convertTodayToTicks(long today) {
        return convertTodayMicrosToTicks(convertTodayToTodayMicros(today));
    }

    /*
         CONVERT TODAY (HHMMSSsssmmm) to TODAY_MICROS (micros since midnight)
    */
    public static long convertTodayToTodayMicros(long today) {
        long aboveSeconds = today / 1_000_000L;
        long underSeconds = today % 1_000_000L;

        return 1_000_000L * (aboveSeconds / 10_000 * 3600 + ((aboveSeconds / 100) % 100) * 60 + (aboveSeconds % 100)) + underSeconds;
    }

    /*
         GET ENTRY (HHMMSSsssmmm) in TICKS (ticks since 1 JAN 0001)
    */
    public static long getEntryTimeInTicks(GroupValue mdEntry) {
        long entryTime = getEntryTimeInTodayMicros(mdEntry);

        return entryTime == 0 ? Utils.currentTimeInTicks() : Utils.convertTodayMicrosToTicks(entryTime);
    }

    /*
         GET ENTRY (HHMMSSsssmmm) in TODAY_MICROS (micros since midnight)
    */
    public static long getEntryTimeInTodayMicros(GroupValue mdEntry) {
        long entryTimeToday = mdEntry.getValue("MDEntryTime") != null ? mdEntry.getInt("MDEntryTime") * 1_000L : 0;
        long entryTimeMicros = mdEntry.getValue("OrigTime") != null ? mdEntry.getInt("OrigTime") : 0;

        return convertTodayToTodayMicros(entryTimeToday + entryTimeMicros);
    }

    public static void printStackTrace(Exception e, Logger logger, String message) {
        if (message != null)
            logger.error(message);

        logger.error(e.toString());
        for (StackTraceElement trace : e.getStackTrace())
           logger.error("--> " + trace.toString());
    }
}
