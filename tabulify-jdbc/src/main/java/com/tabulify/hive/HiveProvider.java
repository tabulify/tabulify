package com.tabulify.hive;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlConnectionProvider;

public class HiveProvider extends SqlConnectionProvider {

  @Override
  public Connection createSqlConnection(Tabular tabular, Attribute name, Attribute uri) {
    return new HiveConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Attribute uri) {
    return uri.getValueOrDefaultAsStringNotNull().startsWith("jdbc:hive2:");
  }

}
