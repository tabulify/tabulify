package net.bytle.api;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;


public class Conf {

  protected static ConfigRetriever getConfigRetriever(Vertx vertx){
    /**
     * Vertex Config
     * The ConfigRetriever provide a way to access the stream of configuration. It’s a ReadStream of JsonObject.
     * By registering the right set of handlers you are notified:
     *   * when a new configuration is retrieved
     *   * when an error occur while retrieving a configuration
     *   * when the configuration retriever is closed (the endHandler is called).
     */

    ConfigRetrieverOptions configRetrieverOptions = buildConfigStoresAndOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);

    // listen is called each time configuration changes
    configRetriever.listen(
      configChangeEvent -> {
        JsonObject updatedConfiguration = configChangeEvent.getNewConfiguration();
        vertx.eventBus().publish(
          EventBusChannels.CONFIGURATION_CHANGED.name(),
          updatedConfiguration);
      });

    return configRetriever;

  }
  /**
   * <a href="https://vertx.io/docs/vertx-config/java/">Doc</a>
   *
   * @return a configuration object that defines a list of store where to find the configuration
   */
  private static ConfigRetrieverOptions buildConfigStoresAndOptions() {

    ConfigRetrieverOptions configRetriever = new ConfigRetrieverOptions();


    /**
     * File in the resources directory
     * <a href="https://vertx.io/docs/vertx-config/java/#_file">Doc</a>
     */
    ConfigStoreOptions classpathFile =
      new ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(
          new JsonObject()
            .put("path", "default.properties")
        );

    /**
     * File on the file system
     */
    ConfigStoreOptions envFile =
      new ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(
          new JsonObject()
            .put("path", "/etc/default/demo")
        )
        .setOptional(true);

    /**
     * Env store
     * <a href="https://vertx.io/docs/vertx-config/java/#_environment_variables">Doc</a>
     */
    JsonArray envVarKeys = new JsonArray();
    Arrays.asList(ConfKeys.values())
      .forEach(s -> envVarKeys.add(s.toString()));
    ConfigStoreOptions environment = new ConfigStoreOptions()
      .setType("env")
      .setOptional(true)
      .setConfig(
        new JsonObject()
          .put("keys", envVarKeys));

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


    return configRetriever.addStore(classpathFile) // local values : exhaustive list with sane defaults
      .addStore(environment)   // Container / PaaS friendly to override defaults
      .addStore(envFile)       // external file, IaaS friendly to override defaults and config hot reloading
      .addStore(sysPropsStore)
      .setScanPeriod(5000);


  }

}
