package net.bytle.vertx;

import static net.bytle.vertx.analytics.AnalyticsLogger.ANALYTICS_CONF_FILE;

public class Log4JManager {

  /**
   * The configuration of Log4J (not the Log4j configuration)
   * <p>
   * Because Log4j is mostly static, it's configured via system properties
   * <p>
   * We call this function almost in all `main` class
   */
  public static void setConfigurationProperties() {

    /**
     * Debug purpose
     */
    System.setProperty("log4j2.debug", "false"); // set it to true if you want to see debug statement

    /**
     * Composite File Configuration
     * https://logging.apache.org/log4j/2.x/manual/configuration.html#CompositeConfiguration
     */
    System.setProperty("log4j.configurationFile", "log4j2.xml," + ANALYTICS_CONF_FILE);

    /**
     * Advertise the configuration factory to call this class
     */
    System.setProperty("log4j.configurationFactory", Log4jConfigurationFactoryPlugin.class.getName());

    /**
     * Log4J has the default logger for vertx is deprecated
     * @deprecated https://github.com/eclipse-vertx/vert.x/issues/2774
     * <p>
     * and io.vertx.core.logging.LoggerFactory#configureWith(String, boolean, ClassLoader)
     * was silencing all errors at initialization (we don't want that as we want a working log4j)
     */
    //System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");

  }

}
