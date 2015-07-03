package com.bufarini.reminders;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Log {
    public final static String LOGTAG = "Reminders";

    /** This must be false for production.  If true, turns on logging,
     test code, etc. */
    public static final boolean LOGV = true;
    public static final boolean LOGD = true;

    public static void v(String logMe) {
        android.util.Log.v(LOGTAG, /* SystemClock.uptimeMillis() + " " + */ logMe);
    }

    public static void i(String logMe) {
        android.util.Log.i(LOGTAG, logMe);
    }

    public static void e(String logMe) {
        android.util.Log.e(LOGTAG, logMe);
    }

    public static void e(String logMe, Exception ex) {
        android.util.Log.e(LOGTAG, logMe, ex);
    }

    public static void w(String logMe) {
        android.util.Log.w(LOGTAG, logMe);
    }

    public static void wtf(String logMe) {
        android.util.Log.wtf(LOGTAG, logMe);
    }

    public static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss.SSS/E", Locale.getDefault()).format(new Date(millis));
    }
}
