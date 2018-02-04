package org.encryptor4j.android.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Debug utilities class.
 *
 * Created by Martin on 13-4-2017.
 */

public class DebugUtils {

    private DebugUtils() {
    }

    /**
     * Converts and returns a stacktrace as a string.
     * @param e     the exception
     * @return the stacktrace as a string
     */
    public static String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Returns the exception's first found message.
     * @param e the exception
     * @return the message
     */
    public static String getFirstExceptionMessage(Exception e) {
        Throwable throwable = e;
        while(throwable != null) {
            String message = throwable.getMessage();
            if(message != null && !message.isEmpty()) {
                return message;
            }
            throwable = throwable.getCause();
        }
        return "";
    }
}
