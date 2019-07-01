package net.bytle.cli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class CliLogFormatter extends Formatter {

    private static Map<String, CliLogFormatter> cliLogFormatters = new HashMap<>();

    // format string for printing the log record

    private final Date dat = new Date();
    private final String sourceModuleName;
    private String format;

    // Windows
    // https://superuser.com/questions/413073/windows-console-with-ansi-colors-handling/1105718#1105718
    // Enable https://github.com/rg3/youtube-dl/issues/15758
    static final String ESC = "\033"; // octal \033 or unicode \u001B
    static final String ANSI_RESET = ESC + "[0m";
    static final String ANSI_RED = ESC + "[31m";


    static CliLogFormatter get(String moduleName) {
        CliLogFormatter cliLogFormatter = cliLogFormatters.get(moduleName);
        if (cliLogFormatter == null) {
            cliLogFormatter = new CliLogFormatter(moduleName);
            cliLogFormatters.put(moduleName, cliLogFormatter);
        }
        return cliLogFormatter;
    }

    /**
     * Use the {@link #get(String)} function
     *
     * @param moduleName
     */
    private CliLogFormatter(String moduleName) {
        this.sourceModuleName = moduleName;
    }

    public synchronized String format(LogRecord record) {
        dat.setTime(record.getMillis());

        String source;
        String qualifiedClassName = record.getSourceClassName();
        String sourceClassName = "";
        String loggerName = record.getLoggerName();
        String sourceMethodName = record.getSourceMethodName();
        if (qualifiedClassName != null) {
            sourceClassName = qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".") + 1, qualifiedClassName.length());
            source = sourceClassName + "." + sourceMethodName;
        } else {
            source = loggerName;
        }

        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }

        String formatted = String.format(format,
                dat,
                source,
                loggerName,
                record.getLevel().getLocalizedName(),
                message,
                throwable,
                sourceModuleName,
                sourceClassName,
                sourceMethodName);

        if (record.getLevel() == Level.SEVERE) {
            formatted = ANSI_RED + formatted + ANSI_RESET;
        }
        return formatted;
    }

    public CliLogFormatter setFormat(String format) {
        this.format = format;
        return this;
    }
}