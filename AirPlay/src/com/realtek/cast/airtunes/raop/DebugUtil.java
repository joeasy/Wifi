
package com.realtek.cast.airtunes.raop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;


/**
 * DebugUtil.
 */
public abstract class DebugUtil {

    /**
     * Bug Report
     */
    public static void setDebug(final Activity activity) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
Log.e("StackTraceElement", ex.getMessage(), ex);
                String errorMessage = ex.toString() + "\n\n";
Log.d("StackTraceElement", errorMessage);

                StackTraceElement stackTrace[] = ex.getStackTrace();

                final StringBuilder bugReport = new StringBuilder();

                bugReport.append(errorMessage + "\n");
                for (int i = 0; i < stackTrace.length; i++) {
                    String className = stackTrace[i].getClassName();
                    int lineNumber = stackTrace[i].getLineNumber();
                    String elementString = className + " : " + String.valueOf(lineNumber);
                    bugReport.append(elementString + "\n");
Log.d("StackTraceElement", stackTrace[i].getClassName());
                }
                bugReport.append("\n\nDevice Name: " + Build.DEVICE + "\n");
                bugReport.append("MODELï¼?" + Build.MODEL + "\n");
                Log.d("model", Build.MODEL);
                bugReport.append("Version.SDK: " + Build.VERSION.SDK + "\n");
                Intent it = new Intent();
                it.setAction(Intent.ACTION_SENDTO);
                it.setData(Uri.parse("mailto:"));
                it.putExtra(Intent.EXTRA_SUBJECT, "error");
                it.putExtra(Intent.EXTRA_TEXT, bugReport.toString());
                activity.startActivity(it);

                activity.finish();
            }
        });
    }
}

/* */
