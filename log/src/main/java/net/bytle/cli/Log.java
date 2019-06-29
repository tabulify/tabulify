package net.bytle.cli;

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
     * The logs
     */
    private static Map<String, Log> Logs = new HashMap<>();


    /**
     * The default prefix for the default namespace
     * Default to prefix + logger name
     */
    private static String LOGGER_NAMESPACE_PREFIX = "net.bytle.";
    /**
     * The namespace
     */
    private String namespace;
    /**
     * The name of the logger
     */
    private final String name;

    /**
     * The format
     */
    private String format = DEFAULT_FORMAT;



    /**
     * Use the {@link #getLog(String)} function to get a log
     *
     * @param name
     */
    Log(String name) {
        this.name = name;
    }




    public static Log getLog(String name) {
        Log log = Logs.get(name);
        if (log == null) {
            log = new Log(name)
            .setNameSpace(LOGGER_NAMESPACE_PREFIX+name.toLowerCase());
            Logs.put(name, log);
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

    /**
     * Get a log with the name of the parent package
     * and the canonical namespace. See {@link #setNameSpace(Class)}
     * @param clazz
     * @return a log
     */
    public static Log getLog(Class clazz) {
        return getLog(clazz.getPackage().getName())
                .setNameSpace(clazz);
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
    protected Logger getLogger() {

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

    /**
     * Logger Name is normally hierarchic "com.foo.bar"
     * and you set it with myClass.class.getCanonicalName
     *
     * @param namespace
     * @return
     */
    public Log setNameSpace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void info(String msg) {
        getLogger().info(msg);
    }

    public void severe(String msg) {
        getLogger().severe(msg);
    }

    public void fine(String msg) {
        getLogger().fine(msg);
    }

    public void warning(String msg) {
        getLogger().fine(msg);
    }

    /**
     * Set the namespace with the canonical name of the class
     * @param clazz
     * @return
     */
    public Log setNameSpace(Class clazz) {
        setNameSpace(clazz.getCanonicalName());
        return this;
    }

    public Level getLevel() {
        return getLogger().getLevel();
    }
}
