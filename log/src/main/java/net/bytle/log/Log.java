package net.bytle.log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {


    /**
     * Format are part of the public API
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_FORMAT = "%1$tH:%1$tM:%1$tS - %4$s - %5$s%n";
    @SuppressWarnings("unused")
    public static final String EXTENDED_FORMAT = "%1$tH:%1$tM:%1$tS - %4$s - %8$s - %9$s - %5$s%n";
    /**
     * The module name (short name that the class)
     */
    static final String MODULE_NAME = "appHome";
    /**
     * The clis
     */
    private static Map<String, Log> cliLogs = new HashMap<>();
    /**
     * The namespace and its default
     */
    private static String CLI_NAMESPACE = "net.bytle.appHome";
    /**
     * The name of the logger
     */
    private final String name;
    /**
     * The format
     */
    private String format = DEFAULT_FORMAT;
    private String namespace = CLI_NAMESPACE;


    /**
     * Use the {@link #getCliLog(String)} function to get a cliLog
     *
     * @param name
     */
    Log(String name) {
        this.name = name;
    }



    public static Log getCliLog() {
        return getCliLog(MODULE_NAME);
    }

    public static Log getCliLog(String name) {
        Log log = cliLogs.get(name);
        if (log == null) {
            log = new Log(name);
            cliLogs.put(name, log);
        }
        return log;
    }

    /**
     * @param string
     * @return a compact string that is written on one line and has no double space
     */
    static public String onOneLine(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll("  ", " "); // No double space;
    }

    @SuppressWarnings("WeakerAccess")
    public void makeLoggerVerbose() {

        Handler consoleHandler = getLogger().getHandlers()[0];
        consoleHandler.setLevel(Level.ALL);
        getLogger().setLevel(Level.ALL);

    }

    /**
     * This function returns a logger.
     * <p>
     * By default, it will show on the console only message that have the INFO level.
     * <p>
     * The logger returned is the same for all CliCommand.
     *
     * @return a logger initialized
     */
    @SuppressWarnings("WeakerAccess")
    public Logger getLogger() {

        Logger logger = Logger.getLogger(this.namespace);
        if (logger.getHandlers().length == 0) {
            // Send logger output to the console
            // Message - The set property must be set before the creation of the simple formatter
            // System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tH:%1$tM:%1$tS - %4$s - %5$s (%2$s)%n");
            // See for the formatting of the value specified in % ... https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
            // See simpleFormatter for the number: https://docs.oracle.com/javase/7/docs/api/java/util/logging/SimpleFormatter.html#format(java.util.logging.LogRecord)
            // %1 = date - formatter %1$tY-%1$tm-%1$td
            // %2 = source (class + method) - formatter %2$s
            // %3 = NAMESPACE
            // %4 = level - formatter %4$s
            // %5 = message
            // %6 = thrown message if any
            LogFormatter fmt = LogFormatter
                    .get(name)
                    .setFormat(this.format);

//            Handler streamHandler = new StreamHandler(System.out,fmt);
//            streamHandler.setLevel(Level.INFO);
            LogHandler consoleHandler = new LogHandler();
            consoleHandler.setFormatter(fmt);
            logger.addHandler(consoleHandler);

            // Parent
            logger.setUseParentHandlers(false);
            // Default INFO
            logger.setLevel(Level.INFO);

        }

        return logger;

    }

    public Log setFormat(String format) {
        this.format = format;
        if (getLogger().getHandlers().length > 0) {
            LogFormatter fmt = LogFormatter.get(this.name)
                    .setFormat(this.format);
            getLogger().getHandlers()[0].setFormatter(fmt);
        }
        return this;
    }

    public Log setNameSpace(String namespace) {
        this.namespace = namespace;
        return this;
    }

}
