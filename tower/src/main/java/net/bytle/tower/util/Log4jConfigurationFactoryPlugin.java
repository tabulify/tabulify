package net.bytle.tower.util;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * The recommended approach for customizing a configuration is to extend one of the standard Configuration classes
 * because configuration file may be reloaded.
 * <p>
 * This class will register the class {@link Log4JXmlConfiguration}
 * that will dynamically modify the Log4J xml configuration.
 * <p>
 * We may delete it and make it part of the programmation.
 * Log4j provide a default configuration if it cannot locate a configuration file.
 * <p>
 * This is used to modify the configuration dynamically
 * when the environment is a dev environment.
 * (
 * ie runtime error are not going only into a file but also into the console
 * so that we can see them when running test
 * )
 *
 * @see <a href="https://logging.apache.org/log4j/2.x/manual/customconfig.html#Hybrid">Initialize Log4j by Combining Configuration File with Programmatic Configuration</a>
 * <p>
 * Instantiation is a little bit tricky
 * but the best is to set the system property early
 * ```java
 * System.setProperty("log4j.configurationFactory", Log4jConfigurationFactoryPlugin.class.getName());
 * ```
 * More info see:
 * <a href="https://stackoverflow.com/questions/44227758/custom-configurationfactory-combined-with-configuration-file-in-log4j2">configurationfactory instantiation</a>
 * <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html">Configuration</a>
 * <a href="https://logging.apache.org/log4j/2.x/manual/customconfig.html">Custom Config</a>
 *
 * We do it now only for test. See the static block in the HttpBaseTest class.
 */
@Plugin(name = "Log4jConfigurationFactoryPlugin", category = ConfigurationFactory.CATEGORY)
@Order(10)
public class Log4jConfigurationFactoryPlugin extends ConfigurationFactory {

  /**
   * Valid file extensions for XML files.
   */
  public static final String[] SUFFIXES = new String[]{".xml", "*"};

  /**
   * Return the Configuration.
   *
   * @param source The InputSource.
   * @return The Configuration.
   */
  @Override
  public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
    return new Log4JXmlConfiguration(loggerContext, source);
  }

  /**
   * Returns the file suffixes for XML files.
   *
   * @return An array of File extensions.
   */
  public String[] getSupportedTypes() {
    return SUFFIXES;
  }


}
