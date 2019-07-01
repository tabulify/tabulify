package net.bytle.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CliLog {



    /**
     * The module name (short name that the class)
     */
    static final String MODULE_NAME = "cli";

    /**
     * Format are part of the public API
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_FORMAT = "%1$tH:%1$tM:%1$tS - %4$s - %5$s%n";
    @SuppressWarnings("unused")
    public static final String EXTENDED_FORMAT = "%1$tH:%1$tM:%1$tS - %4$s - %8$s - %9$s - %5$s%n";

    /**
     * The clis
     */
    private static Map<String, CliLog> cliLogs = new HashMap<>();

    /**
     * The name of the logger
     */
    private final String name;

    /**
     * The format
     */
    private String format = DEFAULT_FORMAT;

    /**
     * The namespace and its default
     */
    private static String CLI_NAMESPACE = "net.bytle.cli";
    private String namespace = CLI_NAMESPACE;



    /**
     * Use the {@link #getCliLog(String)} function to get a cliLog
     *
     * @param name
     */
    CliLog(String name) {
        this.name = name;
    }

    /**
     * A function to help written error message in the log that are safe
     *
     * @param o the input object
     * @return "null" if the object is null or the string representation of the object
     */
    static public String toStringNullSafe(Object o) {

        if (o == null) {
            return "null";
        } else {
            return o.toString();
        }

    }


    public static CliLog getCliLog() {
        return getCliLog(MODULE_NAME);
    }


    @SuppressWarnings("WeakerAccess")
    public void makeLoggerVerbose() {

        Handler consoleHandler = getLogger().getHandlers()[0];
        consoleHandler.setLevel(Level.ALL);
        getLogger().setLevel(Level.ALL);

    }

    public static CliLog getCliLog(String name) {
        CliLog cliLog = cliLogs.get(name);
        if (cliLog == null) {
            cliLog = new CliLog(name);
            cliLogs.put(name, cliLog);
        }
        return cliLog;
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
            CliLogFormatter fmt = CliLogFormatter
                    .get(name)
                    .setFormat(this.format);

//            Handler streamHandler = new StreamHandler(System.out,fmt);
//            streamHandler.setLevel(Level.INFO);
            CliLogHandler consoleHandler = new CliLogHandler();
            consoleHandler.setFormatter(fmt);
            logger.addHandler(consoleHandler);

            // Parent
            logger.setUseParentHandlers(false);
            // Default INFO
            logger.setLevel(Level.INFO);

        }

        return logger;

    }


    public CliLog setFormat(String format) {
        this.format = format;
        if (getLogger().getHandlers().length > 0) {
            CliLogFormatter fmt = CliLogFormatter.get(this.name)
                    .setFormat(this.format);
            getLogger().getHandlers()[0].setFormatter(fmt);
        }
        return this;
    }

    public CliLog setNameSpace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * @param string
     * @return a compact string that is written on one line and has no double space
     */
    static public String onOneLine(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll("  ", " "); // No double space;
    }

}
