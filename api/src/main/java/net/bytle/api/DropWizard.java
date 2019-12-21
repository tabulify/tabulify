package net.bytle.api;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class DropWizard {

  public static DropwizardMetricsOptions getMetricsOptions() {

    /**
     * Dropwizard
     */
    // Initialize Dropwizard metric registry
    String registryName = "registry";
    MetricRegistry registry = SharedMetricRegistries.getOrCreate(registryName);
    SharedMetricRegistries.setDefault(registryName);

    // Initialize Dropwizard reporter
    Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
      .outputTo(LoggerFactory.getLogger(Launcher.class))
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
    reporter.start(1, TimeUnit.MINUTES);

    return new DropwizardMetricsOptions()
      .setEnabled(true)
      .setMetricRegistry(registry);


  }

}
