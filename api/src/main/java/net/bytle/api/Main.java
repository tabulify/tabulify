package net.bytle.api;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import net.bytle.api.http.VerticleHttpServer;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Main {


  public static void main(String[] args) {

    JsonObject config = new JsonObject();
    Vertx vertx = getVertex();
    /**
     * Vertex Config
     * The ConfigRetriever provide a way to access the stream of configuration. It’s a ReadStream of JsonObject.
     * By registering the right set of handlers you are notified:
     *   * when a new configuration is retrieved
     *   * when an error occur while retrieving a configuration
     *   * when the configuration retriever is closed (the endHandler is called).
     */

    ConfigRetrieverOptions configRetrieverOptions = getConfigRetrieverOptions(config);
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);

    /**
     * Deploy Verticle and listen to config changes
     */
    // Start the verticle from the config
    configRetriever.getConfig(
      ar -> {
        int instances = Runtime.getRuntime().availableProcessors();
        // Test
        if (!config.fieldNames().equals(0)) {
          instances = 1;
        }
        JsonObject configRetrieve = ar.result();
        configRetrieve.mergeIn(config);
        vertx.deployVerticle(VerticleHttpServer.class,
          new DeploymentOptions()
            .setInstances(instances)
            .setConfig(configRetrieve));
      });

    // listen is called each time configuration changes
    configRetriever.listen(
      configChangeEvent -> {
        JsonObject updatedConfiguration = configChangeEvent.getNewConfiguration();
        vertx.eventBus().publish(
          EventBusChannels.CONFIGURATION_CHANGED.name(),
          updatedConfiguration);
      });
  }


  public static Vertx getVertex() {

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

    // Initialize vertx with the metric registry
    DropwizardMetricsOptions metricsOptions = new DropwizardMetricsOptions()
      .setEnabled(true)
      .setMetricRegistry(registry);

    /**
     * Vertex
     */
    // Initialize vertx with the metric registry
    VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(metricsOptions);
    Vertx vertx = Vertx.vertx(vertxOptions);



    return vertx;

  }

  /**
   * <a href="https://vertx.io/docs/vertx-config/java/">Doc</a>
   *
   * @param config
   * @return a configuration object that defines a list of store where to find the configuration
   */
  private static ConfigRetrieverOptions getConfigRetrieverOptions(JsonObject config) {

    ConfigRetrieverOptions configRetriever = new ConfigRetrieverOptions();

    /**
     * Http Store to set the configuration of the client
     * <a href="https://vertx.io/docs/vertx-config/java/#_http">Doc</a>
     * The Json object is going at  {@link io.vertx.config.impl.spi.HttpConfigStoreFactory#create(Vertx, JsonObject)}
     * Creating a WebClient or HTTP client will be done with this default variable
     */
    if (config.fieldNames().contains(ConfKeys.HOST.toString())) {
      ConfigStoreOptions httpStore = new ConfigStoreOptions()
        .setType("http")
        .setConfig(config);
      configRetriever.addStore(httpStore);
    }

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
      .forEach(s -> envVarKeys.add(s.name()));
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
