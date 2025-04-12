package com.tabulify.jdbc;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.spi.ConnectionProvider;
import net.bytle.type.Variable;

/**
 * This is the general provider
 * that manages the Sql DataStore {@link SqlDataStoreProvider}
 */
public class JdbcConnectionProvider extends ConnectionProvider {


  public static final String JDBC_SCHEME = "jdbc";


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
   * @throws SecurityException If a security manager is installed and it denies an unspecified
   *                           permission.
   */
  @Override
  public Connection createConnection(Tabular tabular, Variable name, Variable url) {

    // check installed providers
    for (SqlDataStoreProvider provider : SqlDataStoreProvider.installedProviders()) {
      if (provider.accept(url)) {
        return provider.getJdbcDataStore(tabular, name, url);
      }
    }

    // No provider found, return the default data store
    return new SqlConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Variable uri) {
    return uri.getValueOrDefaultAsStringNotNull().toLowerCase().startsWith(JDBC_SCHEME);
  }

}
