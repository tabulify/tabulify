package net.bytle.log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * A wrapper around static log object
 */
public class Logs {


  /**
   * The logs created
   */
  protected static Map<String, Log> logs = new HashMap<>();

  /**
   * Level for all logs
   * By default, this is warning
   * because we don't want any log printed.
   * Even from the cli.
   * The client can set it to another level.
   */
  protected static Level level = Level.WARNING;


  /**
   * Get a log with the name of the parent package
   *
   * @param clazz
   * @return a log
   */
  public static Log createFromClazz(Class clazz) {
    String logName = clazz.getPackage().getName();
    Log log = logs.get(logName);
    // Initially Logger.getLogger()
    if (log == null) {
      log = new Log(logName);
      log.setLevel(level);
      logs.put(logName, log);
    }
    return log;

  }


  public static void setLevel(Level level) {
    Logs.level = level;
    logs.values().forEach(l->l.setLevel(level));
  }


  public static Level getLevel() {
    return level;
  }
}
