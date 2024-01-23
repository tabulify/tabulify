package net.bytle.vertx.analytics.sink;

import io.vertx.core.json.JsonObject;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.type.KeyNameNormalizer;
import net.bytle.vertx.Log4JXmlConfiguration;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.routing.Route;
import org.apache.logging.log4j.core.appender.routing.Routes;
import org.apache.logging.log4j.core.appender.routing.RoutingAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;

import java.util.List;
import java.util.Map;

/**
 * Manage the writing of analytics to log file
 * <p>
 */
public class AnalyticsFileSystemLogger {


  private static final String LOG4J_LOGGER_NAME = AnalyticsFileSystemLogger.class.getName();
  private static final String FILE_SYSTEM_LOGGER_NAME = "analytics";

  private static final Logger ANALYTICS_LOGGER = LogManager.getLogger(LOG4J_LOGGER_NAME);


  /**
   * <a href="https://logging.apache.org/log4j/2.x/manual/lookups.html#context-map-lookup">Lookup variable</a>
   * They are file system value.
   */
  /**
   * The event name in a file system format
   */
  private static final String LOOKUP_VARIABLE_EVENT_SLUG = "event-slug";
  /**
   * The realm
   */
  private static final String LOOKUP_VARIABLE_REALM_GUID = "realm-guid";
  private static final String APPENDER_NAME = "AnalyticsLogger";

  public static String ANALYTICS_CONF_FILE = "log4j2-analytics.xml";

  /**
   * We use Log4j for the rolling and writing
   */
  public static void configure(Log4JXmlConfiguration config) {


    /**
     * For the analytics event, we create a Rolling File
     * that will role every day.
     * <p>
     * See example:
     * http://logging.apache.org/log4j/2.x/faq.html#separate_log_files
     */
    Appender analyticsLogger = config.getAppender(APPENDER_NAME);
    if (analyticsLogger == null) {
      /**
       * We don't create one dynamically because it's a nightmare
        */
      throw new InternalException("The appender " + APPENDER_NAME + " was not found in the log4j configuration.");
    }

    checkAppender(analyticsLogger, config);


    /**
     * Config logger with this appender
     */
    LoggerConfig analyticsEventLogger = LoggerConfig.newBuilder()
      .withLoggerName(LOG4J_LOGGER_NAME)
      .withConfig(config)
      .withLevel(Level.INFO)
      /**
       * By default, log4j2 logging is additive.
       * All the parent loggers will also get the message.
       * We don't want that.
       */
      .withAdditivity(false)
      .build();
    /**
     * Where to send the logs
     * An appender ref is the name of the Appenders to invoke asynchronously.
     */
    analyticsEventLogger.addAppender(analyticsLogger, Level.INFO, null);
    config.addLogger(analyticsEventLogger.getName(), analyticsEventLogger);



  }

  /**
   * We check that the configuration is consistent with the code
   */
  private static void checkAppender(Appender analyticsLogger, Log4JXmlConfiguration config) {

    String eventSlugLookupExpression = "${ctx:" + LOOKUP_VARIABLE_EVENT_SLUG + "}";
    String realmLookupExpression = "${ctx:" + LOOKUP_VARIABLE_REALM_GUID + "}";

    /**
     * Check that this is a RoutingAppender
     * <p>
     * Why a routing Appender and not a RollingFileAppender directly?
     * The Rolling File Appender has variable from the context (the slug and the realm)
     * It should therefore be calculated for each event. That's what routing does
     * <p>
     * Otherwise, it will try to create the RollingFileAppender and you get this error:
     * ERROR StatusConsoleListener Unable to create file logs/analytics/${ctx:realm-guid}/${ctx:event-slug}.jsonl
     * java.io.IOException: The filename, directory name, or volume label syntax is incorrect
     */
    assert analyticsLogger.getClass().equals(RoutingAppender.class);
    RoutingAppender analyticsLoggerRouting = (RoutingAppender) analyticsLogger;
    Routes routesObject = analyticsLoggerRouting.getRoutes();


    Route[] routes = routesObject.getRoutes();
    if (routes.length != 1) {
      throw new InternalException("The appender " + APPENDER_NAME + " has " + routes.length + " routes but we except only 1");
    }
    Node routeNode = routes[0].getNode();
    assert routeNode.getType().getPluginClass().equals(Route.class);
    Object routeObject = new PluginBuilder(routeNode.getType()).withConfiguration(config).withConfigurationNode(routeNode).build();
    assert routeObject.getClass().equals(Route.class);
    Route route = (Route) routeObject;
    List<Node> rootNodeChildrens = route.getNode().getChildren();
    assert rootNodeChildrens.size() == 1;

    /**
     * Checking the Rolling File Appender
     * https://logging.apache.org/log4j/2.x/manual/appenders.html#rollingfileappender
     */
    Node rollingFileAppenderNode = rootNodeChildrens.get(0);
    Class<?> rollingFileAppenderNodeClass = rollingFileAppenderNode.getType().getPluginClass();
    if(!rollingFileAppenderNodeClass.equals(RollingFileAppender.class)){
      throw new InternalException("The first rootNode childrens of the routing appender is not a rolling file appender but a "+rollingFileAppenderNodeClass.getName());
    }
    /**
     * An error is triggered when trying to build the object with {@link PluginBuilder}
     * because it did not see the policies ???
     * We check the node attributes directly then
     */
    Map<String, String> attributes = rollingFileAppenderNode.getAttributes();
    String fileName = attributes.get("fileName");
    String expectedFileName = "logs/" + FILE_SYSTEM_LOGGER_NAME + "/" + realmLookupExpression + "/" + eventSlugLookupExpression + ".jsonl";
    if (!fileName.equals(expectedFileName)) {
      throw new InternalException("The file Name of the rolling file pattern appender (" + APPENDER_NAME + ") is not correct. Value: " + fileName + ", expected value:" + expectedFileName);
    }
    /**
     * The file pattern contains:
     * * a <a href="https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">Simple Date Format</a>
     * {@link java.text.SimpleDateFormat}
     * * a `%i` which represents an integer counter (in case the file is too big)
     * <p>
     * We have added `history` as subdirectory so that there is in the realm directory:
     * * only one directory
     * * and the current events
     * The file name is also unique so that it can be sent without any further info
     */
    String filePattern = attributes.get("filePattern");
    String expectedFilePattern = "logs/" + FILE_SYSTEM_LOGGER_NAME + "/" + realmLookupExpression + "/history/$${date:yyyy-MM}/" + FILE_SYSTEM_LOGGER_NAME + "_" + realmLookupExpression + "_" + eventSlugLookupExpression + "_%d{yyyy-dd-MM}_%i.jsonl";
    if (!filePattern.equals(expectedFilePattern)) {
      throw new InternalException("The file Pattern of the rolling file pattern appender (" + APPENDER_NAME + ") is not correct. Value: " + filePattern + ", expected value:" + expectedFilePattern);
    }

  }

  static public void log(AnalyticsEvent analyticsEvent) throws IllegalStructure {


    // Event Name
    String eventName = analyticsEvent.getName();
    if (eventName == null) {
      /**
       * The easy solution is to park this event.
       * There is nothing to do
       */
      eventName = "unknown";
      ANALYTICS_LOGGER.error("The event name is mandatory");
    }
    String realmIdFromEvent = analyticsEvent.getApp().getAppRealmId();
    if (realmIdFromEvent == null) {
      /**
       * The easy solution is to park this event.
       * There is nothing to do
       */
      realmIdFromEvent = "unknown";
      ANALYTICS_LOGGER.error("The realm id is mandatory");
    }

    /**
     * the event-file system name is used in the name of the file and directory
     * See MDC
     * https://logging.apache.org/log4j/2.x/manual/thread-context.html
     */
    KeyNameNormalizer keyNameNormalizer = KeyNameNormalizer.createFromString(eventName);
    String eventFileSystemName = keyNameNormalizer.toFileCase();
    ThreadContext.put(LOOKUP_VARIABLE_EVENT_SLUG, eventFileSystemName);
    ThreadContext.put(LOOKUP_VARIABLE_REALM_GUID, realmIdFromEvent);
    String jsonString = JsonObject.mapFrom(analyticsEvent).toString();
    ANALYTICS_LOGGER.info(jsonString);
    ThreadContext.clearAll();

  }


}
