package net.bytle.tower.util;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;

/**
 * Dropwizard
 */
public class DropWizard {

  public static DropwizardMetricsOptions getMetricsOptions() {


    String registryName = "vertx";
    MetricRegistry registry = SharedMetricRegistries.getOrCreate(registryName);
    MetricRegistry metricRegistry = SharedMetricRegistries.tryGetDefault();
    if (metricRegistry==null) {
      SharedMetricRegistries.setDefault(registryName);
    }

//    // Initialize Dropwizard csv
//    Path csvPath = Paths.get(Log.LOG_DIR_PATH.toString(),"dropwizard");
//    Fs.createDirectoryIfNotExists(csvPath);
//
//    // The reporter
//    CsvReporter reporter = CsvReporter.forRegistry(registry)
//      .convertRatesTo(TimeUnit.SECONDS)
//      .convertDurationsTo(TimeUnit.MILLISECONDS)
//      .build(csvPath.toFile());
//    reporter.start(1, TimeUnit.MINUTES);

    return new DropwizardMetricsOptions()
      .setEnabled(true)
      .setMetricRegistry(registry);


  }

}
