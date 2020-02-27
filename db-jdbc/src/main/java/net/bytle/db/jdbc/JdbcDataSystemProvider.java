package net.bytle.db.jdbc;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataStoreProvider;

public class JdbcDataSystemProvider extends DataStoreProvider {


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
  public DataStore createDataStore(String name, String url) {

    // check installed providers
    for (JdbcDataStoreExtensionProvider provider : JdbcDataStoreExtensionProvider.installedProviders()) {
      if (provider.accept(url)) {
        return provider.getJdbcDataStore(name, url);
      }
    }

    // No provider found, return the default data store
    return new SqlDataStore(name, url);

  }

  @Override
  public boolean accept(String url) {
    return url.toLowerCase().startsWith(JDBC_SCHEME);
  }

}
