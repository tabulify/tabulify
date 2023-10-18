package net.bytle.vertx;

public class Log4JManager {

  /**
   * The configuration of Log4J (not the Log4j configuration)
   * <p>
   * Because Log4j is mostly static, it's configured via system properties
   * <p>
   * We call this function almost in all `main` class
   */
  @SuppressWarnings("deprecation")
  public static void setConfigurationProperties() {

    /**
     * Advertise the configuration factory to call this class
     */
    System.setProperty("log4j.configurationFactory", Log4jConfigurationFactoryPlugin.class.getName());
    /**
     * Debug purpose
     */
    System.setProperty("log4j2.debug","false"); // set it to true if you want to see debug statement

    /**
     * Log4J has the default logger for vertx is deprecated
     * @deprecated https://github.com/eclipse-vertx/vert.x/issues/2774
     * <p>
     * and {@link io.vertx.core.logging.LoggerFactory#configureWith(String, boolean, ClassLoader)}
     * was silencing all errors at initialization (we don't want that as we want a working log4j)
     */
    //System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");

  }

}
