package net.bytle.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class LogFormatter extends Formatter {

  // Windows
  // https://superuser.com/questions/413073/windows-console-with-ansi-colors-handling/1105718#1105718
  // Enable https://github.com/rg3/youtube-dl/issues/15758
  static final String ESC = "\033"; // octal \033 or unicode \u001B

  // format string for printing the log record
  static final String ANSI_RESET = ESC + "[0m";
  static final String ANSI_RED = ESC + "[31m";

  // For orange (xterm extends SGR codes 38 and 48 to provide a palette of colors using RGB)
  // 38: foreground (48 for background)
  // 2: dim
  // 255, 153, 0: Orange color
  static final String ANSI_ORANGE = ESC + "[38;2;255;153;0m";
  private static final String ANSI_YELLOW = ESC + "[33m";

  private final Date now = new Date();
  private final Log log;

  public LogFormatter(Log log) {
    this.log = log;
  }


  static LogFormatter create(Log log) {
    return new LogFormatter(log);
  }

  public synchronized String format(LogRecord record) {

    now.setTime(record.getMillis());

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
    if (record.getThrown() != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println();
      record.getThrown().printStackTrace(pw);
      pw.close();
      throwable = sw.toString();
    }

    String format = this.log.getFormat();
    String formatted = String.format(
      format,
      now,
      source,
      loggerName,
      record.getLevel().getLocalizedName(),
      message,
      throwable,
      log.getSimpleName(),
      sourceClassName,
      sourceMethodName);

    if (record.getLevel() == Level.SEVERE) {
      formatted = ANSI_RED + formatted + ANSI_RESET;
    } else if (record.getLevel() == Log.TIP){
      formatted = ANSI_YELLOW + formatted + ANSI_RESET;
    } else if (record.getLevel() == Level.WARNING){
      formatted = ANSI_ORANGE + formatted + ANSI_RESET;
    }
    return formatted;
  }


}
