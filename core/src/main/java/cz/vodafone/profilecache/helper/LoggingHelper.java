package cz.vodafone.profilecache.helper;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggingHelper {

    private static final SimpleDateFormat df;

    static {
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    public static void info(Object message, Logger logger) {
        log(message, logger, Level.INFO, null);
    }

    public static void warn(Object message, Logger logger) {
        log(message, logger, Level.WARN, null);
    }

    public static void error(Object message, Logger logger) {
        log(message, logger, Level.ERROR, null);
    }

    public static void error(Object message, Logger logger, Throwable t) {
        log(message, logger, Level.ERROR, t);
    }

    public static void fatal(Object message, Logger logger) {
        log(message, logger, Level.FATAL, null);
    }

    public static void fatal(Object message, Logger logger, Throwable t) {
        log(message, logger, Level.FATAL, t);
    }

    public static void log(Object message, Logger logger, Level level, Throwable t) {
        PrintStream ps;
        if (level.isGreaterOrEqual(Level.ERROR)) {
            ps = System.err;
        } else {
            ps = System.out;
        }

        ps.println(String.format("%s %s %s", df.format(new Date()), level.toString(), message.toString()));
        if (t != null) {
            t.printStackTrace(ps);
        }
        logger.log(level, message, t);
    }


}
