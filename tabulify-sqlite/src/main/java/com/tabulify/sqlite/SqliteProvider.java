package com.tabulify.sqlite;

import com.tabulify.Tabular;
import com.tabulify.jdbc.SqlDataStoreProvider;
import com.tabulify.conf.Attribute;

public class SqliteProvider extends SqlDataStoreProvider {

  public static final String URL_PREFIX = "jdbc:sqlite:";
  public static final String ROOT = "///";

  @Override
  public SqliteConnection getJdbcDataStore(Tabular tabular, Attribute name, Attribute url) {

    return new SqliteConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }

}
