package net.bytle.log;


import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.io.File.separator;

public class Log extends Logger {


  /**
   * Format
   * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html">...</a>
   * <p>
   * Time at sec - level - message
   */
  public static final String DEFAULT_FORMAT = "%1$tH:%1$tM:%1$tS - %4$s - %5$s%n";

  /**
   * Time with ms - level - function - message
   */
  public static final String EXTENDED_FORMAT = "%1$tH:%1$tM:%1$tS.%1$tL - %4$s - %8$s.%9$s() - %5$s%n";


  /**
   * The name of the logger
   */
  private final String name;

  /**
   * The format
   */
  private String format = DEFAULT_FORMAT;


  /**
   * Use the {@link Logs#createFromClazz} function to get a log
   *
   * @param name
   */
  /**
   * This function returns a logger.
   * <p>
   * By default, it will show on the console only message that have the INFO level.
   * <p>
   * The logger returned is the same for all CliCommand.
   *
   *
   */
  Log(String name) {
    super(name,null);
    this.name = name;


      if (this.getHandlers().length == 0) {
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
          .create(this);

        // Handler streamHandler = new StreamHandler(System.out,fmt);
        // streamHandler.setLevel(Level.INFO);
        LogHandler consoleHandler = new LogHandler();
        consoleHandler.setFormatter(fmt);
        this.addHandler(consoleHandler);

        // Parent
        this.setUseParentHandlers(false);
        // Default INFO
        this.setLevel(Level.INFO);

    }
  }


  /**
   * @param string the content that should be on one line
   * @return a compact string that is written on one line and has no double space
   */
  static public String onOneLine(String string) {
    return string.replaceAll("\r\n|\n", " ") // No new line
      .replaceAll(" ", " "); // No double space;
  }

  public void makeLoggerVerbose() {

    Handler consoleHandler = this.getHandlers()[0];
    consoleHandler.setLevel(Level.ALL);
    this.setLevel(Level.ALL);

  }



  public Log setFormat(String format) {
    this.format = format;
    return this;
  }





  public void severe(String... s) {
    this.severe(String.join(" - ", s));
  }

  public void info(String... s) {
    this.info(String.join(" - ", s));
  }

  public static final Level TIP = new LogLevel("TIP", 850);

  public void tip(String s) {

    this.log(TIP,s);

  }

  /**
   *
   * @return The last name of the path
   */
  public String getSimpleName() {
    return this.name.substring(this.name.lastIndexOf(".")+1);
  }

  public void fine(String... s) {
    super.fine(
      this.createMultiLineFromStrings(s).toString()
    );
  }

  private Object createMultiLineFromStrings(String[] strings) {
    return  Arrays.stream(strings)
      .map(s -> s == null ? "null" : s)
      .collect(Collectors.joining(separator));
  }

  @Override
  public void setLevel(Level newLevel) throws SecurityException {
    if (this.getHandlers().length > 0) {
      this.getHandlers()[0].setLevel(newLevel);
    }
    if(newLevel.intValue() <= Level.FINE.intValue()){
      this.setFormat(Log.EXTENDED_FORMAT);
    }
    super.setLevel(newLevel);
  }

  public String getFormat() {
    return format;
  }
}
