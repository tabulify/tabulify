package net.bytle.vertx;


import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.ext.web.Router;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * MicroMeter Prometheus wrapper
 * <a href="https://vertx.io/docs/vertx-micrometer-metrics/java/#_prometheus">...</a>
 * Example:
 * <a href="https://github.com/vert-x3/vertx-examples/tree/master/micrometer-metrics-examples">...</a>
 * To create metrics, you get the {@link #getRegistry()} and add micrometer metrics
 * See <a href="https://micrometer.io/docs/concepts#_registry">...</a>
 *
 */
public class VertxPrometheusMetrics {

  private static final Logger LOGGER = LogManager.getLogger(VertxPrometheusMetrics.class);
  private static final String REGISTRY_NAME = "tower";

  public static MetricsOptions getInitMetricsOptions() {

    LOGGER.info("Setting Prometheus Metrics");
    return new MicrometerMetricsOptions()
      .setEnabled(true)
      .setRegistryName(REGISTRY_NAME)
      .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));

  }

  /**
   * Get the registry
   */
  public static PrometheusMeterRegistry getRegistry() {
    /**
     * By default, a unique registry is used and is shared across the Vert.x instances of the JVM.
     * We just show how to get another registry
     */
    return (PrometheusMeterRegistry) BackendRegistries.getNow(REGISTRY_NAME);
  }

  /**
   * Expose the metrics
   */
  public static void mountOnRouter(Router router) {

    LOGGER.info("Prometheus /metrics point mounted");
    router.route("/metrics").handler(PrometheusScrapingHandler.create());

  }

  /**
   * After the Vert.x instance has been created,
   * we can configure the metrics registry to enable histogram buckets
   * for percentile approximations.
   * By default, histogram-kind metrics will not contain averages or quantile stats.
   * See <a href="https://vertx.io/docs/vertx-micrometer-metrics/java/#_averages_and_quantiles_in_prometheus">Ref</a>
   */
  public static void configEnableHistogramBuckets() {
    LOGGER.info("Enable Prometheus Histogram Buckets");
    PrometheusMeterRegistry registry = (PrometheusMeterRegistry) BackendRegistries.getDefaultNow();
    registry.config().meterFilter(
      new MeterFilter() {
        @Override
        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
          return DistributionStatisticConfig.builder()
            .percentilesHistogram(true)
            .build()
            .merge(config);
        }
      });
  }
}
