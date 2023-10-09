package net.bytle.tower.util;

import net.bytle.exception.InternalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * A central class to configure Log4J
 */
public class Log4jConfigure {


  /**
   * Configuration when the Log4J Xml is used for the first time
   * (ie on XML read)
   */
  public static void configureOnXmlRead(Log4JXmlConfiguration log4JXmlConfiguration) {
    try {

      Log4jRootLogger.configure(log4JXmlConfiguration);

      AnalyticsLogger.configure(log4JXmlConfiguration);

      ContextFailureLogger.configure(log4JXmlConfiguration);

    } catch (Exception e) {

      throw new InternalException("Error while configuring Log4J: log activities will not work", e);

    }
  }

  /**
   * Reconfigure Log4 after being started
   * as we need to pass parameters
   * such as smtp
   * Based on <a href="https://logging.apache.org/log4j/2.x/manual/customconfig.html#programmatically-modifying-the-current-configuration-after-initi">...</a>
   */
  public static void configureOnVertxInit(MailSmtpInfo mailSmtpInfo) {

    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();

    /**
     * All the loggers can hook below
     */
    Log4jRootLogger.configureOnAppInit(config, mailSmtpInfo);

    /**
     * Update
     */
    ctx.updateLoggers();

  }

  /**
   * @return The console appender
   */
  public static ConsoleAppender getConsoleAppender(Configuration configuration) {
    return configuration.getAppender("Console");
  }

}
