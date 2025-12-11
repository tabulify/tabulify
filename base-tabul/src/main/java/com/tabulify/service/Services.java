package com.tabulify.service;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;

import java.util.List;

/**
 * Service static methods
 */
public class Services {

  public static Service createService(Tabular tabular, Attribute serviceName, Attribute type) {
    List<ServiceProvider> installedProviders = ServiceProvider.installedProviders();
    for (ServiceProvider serviceProvider : installedProviders) {
      if (serviceProvider.accept(type.getValueOrDefaultAsStringNotNull())) {
        return serviceProvider.createService(tabular, serviceName);
      }
    }
    return new NoOpService(tabular, serviceName);
  }


}

