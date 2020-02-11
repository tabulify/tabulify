package net.bytle.db.sqlite;

import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataStoreExtensionProvider;

/**
 * Created by gerard on 02-12-2015.
 */
public class SqliteProvider extends JdbcDataStoreExtensionProvider {


  @Override
  public String getProductName() {
    return "sqlite";
  }


  /**
   *
   * @param jdbcdataStore URI reference
   * @return The sql database
   * @throws SecurityException If a security manager is installed and it denies an unspecified
   *                           permission.
   */
  @Override
  public JdbcDataStoreExtension getJdbcDataStoreExtension(JdbcDataStore jdbcdataStore) {
    return new SqliteJdbcDataStoreExtension(jdbcdataStore);
  }

}
