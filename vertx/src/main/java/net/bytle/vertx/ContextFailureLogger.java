package net.bytle.vertx;

import net.bytle.java.JavaEnvs;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class ContextFailureLogger {

  private static final String CONTEXT_FAILURE_LOGGER_NAME = ContextFailureLogger.class.getName();

  public static final Logger CONTEXT_FAILURE_LOGGER = LogManager.getLogger(CONTEXT_FAILURE_LOGGER_NAME);


  public static void configure(Log4JXmlConfiguration config) {

    /**
     * Config logger with this appender
     */
    LoggerConfig contextFailureLoggerConf = LoggerConfig.newBuilder()
      .withLoggerName(CONTEXT_FAILURE_LOGGER_NAME)
      .withConfig(config)
      .withLevel(Level.INFO)
      /**
       * By default, log4j2 logging is additive.
       * All the parent loggers will also get the message.
       * We don't want that.
       */
      .withAdditivity(false)
      .build();


    if (JavaEnvs.IS_DEV) {

      ConsoleAppender consoleAppender = config.getConsoleAppender();
      contextFailureLoggerConf.addAppender(consoleAppender, null, null);

    }


  }

}
