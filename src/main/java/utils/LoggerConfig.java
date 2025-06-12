package utils;

import java.util.logging.*;

public class LoggerConfig {

    // Configures global logging to show only the log message (no timestamps, class, or method names).
    public static void configureSimpleLogging() {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();

        for (Handler handler : handlers) {
            handler.setFormatter(new SimpleFormatter() {
                @Override
                public synchronized String format(LogRecord record) {
                    return record.getMessage() + "\n";
                }
            });
        }
    }
}
