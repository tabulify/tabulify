package net.bytle.vertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A wrapper around a {@link ConfigRetriever}
 * to manage the configuration
 */
public class ConfigManager {

  private static final Logger LOGGER = LogManager.getLogger(ServerStartLogger.class.getSimpleName() + "." + ConfigManager.class.getSimpleName());

  private static ConfigRetriever configRetriever;
  public final Vertx vertx;
  private final Path secretFilePath;
  private final JsonObject actualConfig;

  /**
   * This file is never deleted in contrario to the secret file.
   * <p>
   * In dev, this file may contain also basic secrets such as admin/admin
   * because the secret file is deleted
   * after the web server starts.
   */
  private final Path configFile;

  private final String configName;


  public ConfigManager(ConfigManagerConfig configManagerConfig) {

    this.configName = configManagerConfig.name;
    this.vertx = configManagerConfig.vertx;

    Path currentPath = Paths.get(".");
    //noinspection ReplaceNullCheck
    if (configManagerConfig.setSecretFilePath == null) {
      this.secretFilePath = currentPath.resolve("." + this.configName + ".secret.yml").normalize();
    } else {
      this.secretFilePath = configManagerConfig.setSecretFilePath;
    }
    this.configFile = currentPath.resolve("." + this.configName + ".yml").normalize();

    this.actualConfig = configManagerConfig.jsonConfig;


    /**
     * Vertex Config
     * The ConfigRetriever provide a way to access the stream of configuration. It’s a ReadStream of JsonObject.
     * By registering the right set of handlers you are notified:
     *   * when a new configuration is retrieved
     *   * when an error occur while retrieving a configuration
     *   * when the configuration retriever is closed (the endHandler is called).
     */
    ConfigRetrieverOptions configRetrieverOptions = buildConfigStoresAndOptions();
    configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);

    /**
     * configRetriever will listen to change (it is called each time configuration changes)
     * and publish them on the {@link EventBusChannels}
     */
    configRetriever.listen(
      configChangeEvent -> {
        JsonObject updatedConfiguration = configChangeEvent.getNewConfiguration();
        vertx.eventBus().publish(
          EventBusChannels.CONFIGURATION_CHANGED.name(),
          updatedConfiguration);
      });


  }

  /**
   * Use {@link #getConfigAccessor()} instead
   */
  private Future<JsonObject> getConfig() {
    return configRetriever.getConfig()
      .compose(json -> {

        if (Files.exists(this.secretFilePath)) {
          if (!JavaEnvs.IS_DEV) {
            try {
              Fs.write(this.secretFilePath, "");
              // we don't delete because the retriever will try to load it
              LOGGER.info("The secret configuration file content was deleted.");
            } catch (RuntimeException e) {
              String errorMessage = "The secret configuration file content could not be deleted";
              InternalException internal = new InternalException(errorMessage, e);
              return Future.failedFuture(internal);
            }
          }
        }

        /**
         * In test, the verticle configurations take over
         * (for instance to set the port)
         */
        json.mergeIn(actualConfig);

        return Future.succeededFuture(json);

      });
  }

  public Future<ConfigAccessor> getConfigAccessor() {
    return getConfig()
      .compose(jsonObject -> {
        ConfigAccessor result;
        try {
          result = ConfigAccessor.init(this.configName, jsonObject);
        } catch (ConfigIllegalException e) {
          return Future.failedFuture(e);
        }
        return Future.succeededFuture(result);
      });
  }

  public String getName() {
    return this.configName;
  }


  /**
   * @param name   - the name of the configuration used to create the conf file name and env
   * @param vertx  - vertx (needed by vertx ConfigRetriever)
   *               We don't pass the {@link io.vertx.core.Verticle} to be able
   *               to test it without starting a verticle
   * @param config - the extra configuration (We make it mandatory so that the verticle config is always given)
   */
  public static ConfigManagerConfig config(String name, Vertx vertx, JsonObject config) {

    return new ConfigManagerConfig(name, vertx, config);

  }

  /**
   * <a href="https://vertx.io/docs/vertx-config/java/">Doc</a>
   *
   * @return a configuration object that defines a list of store where to find the configuration
   */
  private ConfigRetrieverOptions buildConfigStoresAndOptions() {

    ConfigRetrieverOptions configRetriever = new ConfigRetrieverOptions();

    /**
     * File in the resources directory
     * <a href="https://vertx.io/docs/vertx-config/java/#_yaml_configuration_format">Doc</a>
     * We use Yaml and not properties because it can handle multiple line values.
     * It's handy for cryptographic key
     */

    /**
     * The user/env value in the running directory
     * <a href="https://vertx.io/docs/vertx-config/java/#_file">Doc</a>
     *
     */
    /**
     * Retrieve the config data
     */
    if (Files.exists(this.configFile)) {
      LOGGER.info("Configuration: tower configuration file found (" + this.configFile.toAbsolutePath() + ")");
    } else {
      LOGGER.warn("Configuration: tower configuration file not found (" + this.configFile.toAbsolutePath() + ")");
    }

    ConfigStoreOptions yamlFile = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setOptional(true)
      .setConfig(new JsonObject()
        .put("path", this.configFile.toAbsolutePath().toString())
        .put("raw-data", true)
      );

    if (Files.exists(secretFilePath)) {
      LOGGER.info("Configuration: tower secret configuration file found (" + secretFilePath.toAbsolutePath() + ")");
    } else {
      LOGGER.warn("Configuration: tower secret configuration file not found (" + secretFilePath.toAbsolutePath() + ")");
    }

    /**
     * The user/env value in the running directory
     */
    ConfigStoreOptions yamlSecretFile = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setOptional(true)
      .setConfig(new JsonObject()
        .put("path", this.secretFilePath.toAbsolutePath().toString())
        .put("raw-data", true)
      );

    /**
     * Env store
     * <a href="https://vertx.io/docs/vertx-config/java/#_environment_variables">Doc</a>
     */
    JsonArray envVarKeys = new JsonArray();
    System.getenv().keySet()
      .forEach(s -> {
        if (s.startsWith(this.configName)) {
          LOGGER.info("Configuration: environment variable (" + s + ") found and monitored");
          envVarKeys.add(s);
        }
      });
    ConfigStoreOptions environment = new ConfigStoreOptions()
      .setType("env")
      .setOptional(true)
      .setConfig(
        new JsonObject()
          .put("keys", envVarKeys)
      );

    /**
     * Sys store
     */
    ConfigStoreOptions sysPropsStore = new ConfigStoreOptions()
      .setType("sys")
      .setConfig(
        new JsonObject()
          .put("cache", true)
          .put("raw-data", false)
      );


    return configRetriever
      .addStore(environment)
      .addStore(yamlSecretFile)
      .addStore(yamlFile)
      .addStore(sysPropsStore)
      .setScanPeriod(10000);

  }


  public static class ConfigManagerConfig {

    private final String name;
    private final Vertx vertx;
    public JsonObject jsonConfig;
    private Path setSecretFilePath;

    public ConfigManagerConfig(String name, Vertx vertx, JsonObject config) {
      this.name = name;
      this.vertx = vertx;
      this.jsonConfig = config;
    }

    public ConfigManagerConfig setSecretFilePath(Path secretFilePath) {
      this.setSecretFilePath = secretFilePath;
      return this;
    }


    public ConfigManager build() {

      return new ConfigManager(this);

    }

  }
}
