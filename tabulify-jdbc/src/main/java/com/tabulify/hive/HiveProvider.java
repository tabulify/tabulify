package com.tabulify.hive;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreProvider;
import com.tabulify.conf.Attribute;

public class HiveProvider extends SqlDataStoreProvider {

  @Override
  public Connection getJdbcDataStore(Tabular tabular, Attribute name, Attribute uri) {
    return new HiveConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Attribute uri) {
    return uri.getValueOrDefaultAsStringNotNull().startsWith("jdbc:hive2:");
  }

}
