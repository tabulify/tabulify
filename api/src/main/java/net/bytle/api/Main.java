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
    getVertex();
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

    /**
     * Vertex Config
     * The ConfigRetriever provide a way to access the stream of configuration. It’s a ReadStream of JsonObject.
     * By registering the right set of handlers you are notified:
     *   * when a new configuration is retrieved
     *   * when an error occur while retrieving a configuration
     *   * when the configuration retriever is closed (the endHandler is called).
     */
    ConfigRetrieverOptions configRetrieverOptions = getConfigRetrieverOptions();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);

    /**
     * Deploy Verticle and listen to config changes
     */
    // Start the verticle from the config
    configRetriever.getConfig(
      ar -> {
        int instances = Runtime.getRuntime().availableProcessors();
        vertx.deployVerticle(VerticleHttpServer.class,
          new DeploymentOptions()
            .setInstances(instances)
            .setConfig(ar.result()));
      });

    // listen is called each time configuration changes
    configRetriever.listen(
      configChangeEvent -> {
        JsonObject updatedConfiguration = configChangeEvent.getNewConfiguration();
        vertx.eventBus().publish(
          EventBusChannels.CONFIGURATION_CHANGED.name(),
          updatedConfiguration);
      });

    return vertx;

  }

  /**
   * <a href="https://vertx.io/docs/vertx-config/java/">Doc</a>
   *
   * @return a configuration object that defines a list of store where to find the configuration
   */
  private static ConfigRetrieverOptions getConfigRetrieverOptions() {

    /**
     * Http Store
     */
    ConfigStoreOptions httpStore = new ConfigStoreOptions()
      .setType("http")
      .setConfig(
        new JsonObject()
          .put("host", "localhost")
          .put("port", 8083)
          .put("ssl", true)
      );

    /**
     * File variable
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
     * Directory variable
     */
    JsonObject envFileConfiguration = new JsonObject().put("path", "/etc/default/demo");
    ConfigStoreOptions envFile =
      new ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(envFileConfiguration)
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

    return new ConfigRetrieverOptions()
      .addStore(httpStore)
      .addStore(classpathFile) // local values : exhaustive list with sane defaults
      .addStore(environment)   // Container / PaaS friendly to override defaults
      .addStore(envFile)       // external file, IaaS friendly to override defaults and config hot reloading
      .addStore(sysPropsStore)
      .setScanPeriod(5000);
  }
}
