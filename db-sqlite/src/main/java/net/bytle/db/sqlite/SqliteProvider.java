package net.bytle.db.sqlite;

import net.bytle.db.jdbc.JdbcDataStoreExtensionProvider;

public class SqliteProvider extends JdbcDataStoreExtensionProvider {

  public final String URL_PREFIX = "jdbc:sqlite:";

  @Override
  public SqliteDataStore getJdbcDataStore(String name, String url) {

    return new SqliteDataStore(name, url);

  }

  @Override
  public boolean accept(String url) {
    return url.startsWith(URL_PREFIX);
  }

}
