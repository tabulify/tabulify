package net.bytle.db.sqlserver;

import net.bytle.db.database.DataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtensionProvider;

public class SqlServerDataStoreExtensionProvider extends JdbcDataStoreExtensionProvider {

  public final String URL_PREFIX = "jdbc:sqlserver:";

  @Override
  public DataStore getJdbcDataStore(String name, String url) {
    return new SqlServerDataStore(name, url);
  }

  @Override
  public boolean accept(String url) {
    return url.startsWith(URL_PREFIX);
  }

}
