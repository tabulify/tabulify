package com.tabulify.howto;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.service.Service;
import com.tabulify.spi.ConnectionProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Howtos {

  public static Set<Connection> getConnections(Tabular tabular) {
    Set<Connection> connections = new HashSet<>();
    List<ConnectionProvider> installedProviders = ConnectionProvider.installedProviders();
    for (ConnectionProvider connectionProvider : installedProviders) {
      connections.addAll(connectionProvider.getHowToConnections(tabular));
    }
    return connections;
  }

  public static Set<Service> getServices(Tabular tabular) {

    List<ConnectionProvider> installedProviders = ConnectionProvider.installedProviders();
    Set<Service> services = new HashSet<>();
    for (ConnectionProvider connectionProvider : installedProviders) {
      services.addAll(connectionProvider.getHowToServices(tabular));
    }
    return services;
  }

}
