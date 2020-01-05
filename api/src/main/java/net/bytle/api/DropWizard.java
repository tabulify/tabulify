package net.bytle.api;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Dropwizard
 */
public class DropWizard {

  public static DropwizardMetricsOptions getMetricsOptions() {

    // Initialize Dropwizard metric registry
    String registryName = "vertx";
    MetricRegistry registry = SharedMetricRegistries.getOrCreate(registryName);
    SharedMetricRegistries.setDefault(registryName);

    // Initialize Dropwizard csv
    Path csvPath = Log.LOG_DIR_PATH;

    // The reporter
    CsvReporter reporter = CsvReporter.forRegistry(registry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build(csvPath.toFile());
    reporter.start(1, TimeUnit.MINUTES);

    return new DropwizardMetricsOptions()
      .setEnabled(true)
      .setMetricRegistry(registry);


  }

}
