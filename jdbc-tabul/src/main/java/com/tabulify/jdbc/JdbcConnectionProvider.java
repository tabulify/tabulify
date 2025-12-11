package com.tabulify.jdbc;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.service.Service;
import com.tabulify.spi.ConnectionProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the general provider
 * that manages the Sql DataStore {@link SqlConnectionProvider}
 */
public class JdbcConnectionProvider extends ConnectionProvider {


  public static final String JDBC_SCHEME = "jdbc";
  private final List<SqlConnectionProvider> installedProviders;

  public JdbcConnectionProvider() {
    installedProviders = SqlConnectionProvider.installedProviders();
  }

  /**
   * Returns an existing {@code work} created by this provider.
   * <p/>
   * The work is identified by its {@code URI}. Its exact form
   * is highly provider dependent.
   * <p/>
   * <p> If a security manager is installed then a provider implementation
   * may require to check a permission before returning a reference to an
   * existing work.
   *
   * @return The sql database
   * @throws SecurityException If a security manager is installed, and it denies an unspecified
   *                           permission.
   */
  @Override
  public Connection createConnection(Tabular tabular, Attribute name, Attribute url) {

    // check installed providers
    for (SqlConnectionProvider provider : installedProviders) {
      if (provider.accept(url)) {
        return provider.createSqlConnection(tabular, name, url);
      }
    }

    // No provider found, return the default sql connection
    return new SqlConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Attribute uri) {
    return uri.getValueOrDefaultAsStringNotNull().toLowerCase().startsWith(JDBC_SCHEME);
  }

  @Override
  public Set<Connection> getHowToConnections(Tabular tabular) {
    Set<Connection> connections = new HashSet<>();
    for (SqlConnectionProvider provider : installedProviders) {
      connections.addAll(provider.getHowToConnections(tabular));
    }
    return connections;
  }

  @Override
  public Set<Service> getHowToServices(Tabular tabular) {
    Set<Service> services = new HashSet<>();
    for (SqlConnectionProvider provider : installedProviders) {
      services.addAll(provider.getHowToServices(tabular));
    }
    return services;
  }
}
