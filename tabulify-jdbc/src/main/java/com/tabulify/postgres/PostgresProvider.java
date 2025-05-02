package com.tabulify.postgres;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreProvider;
import com.tabulify.conf.Attribute;

public class PostgresProvider extends SqlDataStoreProvider {

  public final String URL_PREFIX = "jdbc:postgresql:";

  @Override
  public Connection getJdbcDataStore(Tabular tabular, Attribute name, Attribute url) {
    return new PostgresConnection(tabular, name, url);
  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }
}
