package net.bytle.api;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * The Vertx Launcher
 * https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher
 * By default, it executed the run command {@link }
 */
public class Main extends io.vertx.core.Launcher {


  private ConfigRetriever configRetriever;

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    options.setMetricsOptions(getMetricsOptions());
  }

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    // Start the verticle from the config
    int instances = 1; // May be Runtime.getRuntime().availableProcessors();
    deploymentOptions
      .setInstances(instances);

    Future<JsonObject> future = Future.future(configRetriever::getConfig);
    future.setHandler(ar -> {
      if (ar.failed()) {
        // Failed to retrieve the configuration
      } else {
        deploymentOptions.setConfig(ar.result());
      }
    });
  }

  private JsonObject addHttpConfig(JsonObject jsonObject) {
    return jsonObject.mergeIn(new JsonObject()
      .put(ConfKeys.HOST.toString(), "localhost")
      .put(ConfKeys.PORT.toString(), 8080)
      .put(ConfKeys.POKE_API_PATH.toString(), "v2/pokemon")
    );
  }

  @Override
  public void afterStartingVertx(Vertx vertx) {

    /**
     * Vertex Config
     * The ConfigRetriever provide a way to access the stream of configuration. It’s a ReadStream of JsonObject.
     * By registering the right set of handlers you are notified:
     *   * when a new configuration is retrieved
     *   * when an error occur while retrieving a configuration
     *   * when the configuration retriever is closed (the endHandler is called).
     */

    ConfigRetrieverOptions configRetrieverOptions = getConfigRetrieverOptions();
    configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);

    // listen is called each time configuration changes
    configRetriever.listen(
      configChangeEvent -> {
        JsonObject updatedConfiguration = configChangeEvent.getNewConfiguration();
        vertx.eventBus().publish(
          EventBusChannels.CONFIGURATION_CHANGED.name(),
          updatedConfiguration);
      });

  }


  private static DropwizardMetricsOptions getMetricsOptions() {

    /**
     * Dropwizard
     */
    // Initialize Dropwizard metric registry
    String registryName = "registry";
    MetricRegistry registry = SharedMetricRegistries.getOrCreate(registryName);
    SharedMetricRegistries.setDefault(registryName);

    // Initialize Dropwizard reporter
    Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
      .outputTo(LoggerFactory.getLogger(Main.class))
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
    reporter.start(1, TimeUnit.MINUTES);

    return new DropwizardMetricsOptions()
      .setEnabled(true)
      .setMetricRegistry(registry);


  }

  /**
   * <a href="https://vertx.io/docs/vertx-config/java/">Doc</a>
   *
   * @return a configuration object that defines a list of store where to find the configuration
   */
  private static ConfigRetrieverOptions getConfigRetrieverOptions() {

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

  public static void main(String[] args) {

    new Main().dispatch(args);

  }
}
