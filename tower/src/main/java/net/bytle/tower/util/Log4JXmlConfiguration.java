package net.bytle.tower.util;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

/**
 * The central log4j class
 * <p>
 * This object is called by the {@link Log4jConfigurationFactoryPlugin}
 * and call {@link #doConfigure()} to configure dynamically log4j.
 * <p>
 * Every LoggerContext has an active Configuration.
 * The Configuration contains all the Appenders, context-wide Filters, LoggerConfigs and contains the reference to the StrSubstitutor.
 * During reconfiguration two Configuration objects will exist.
 * Once all Loggers have been redirected to the new Configuration, the old Configuration will be stopped and discarded.
 */
public class Log4JXmlConfiguration extends XmlConfiguration {


  public Log4JXmlConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
    super(loggerContext, source);
  }

  @Override
  protected void doConfigure() {
    /**
     * Load the appenders from the log4j.xml file
     */
    super.doConfigure();

    Log4jConfigure.configureOnXmlRead(this);


  }


  public ConsoleAppender getConsoleAppender() {
    return Log4jConfigure.getConsoleAppender(this);
  }

}
