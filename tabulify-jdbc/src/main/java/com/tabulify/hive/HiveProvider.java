package com.tabulify.hive;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreProvider;
import net.bytle.type.Variable;

public class HiveProvider extends SqlDataStoreProvider {

  @Override
  public Connection getJdbcDataStore(Tabular tabular, Variable name, Variable uri) {
    return new HiveConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Variable uri) {
    return uri.getValueOrDefaultAsStringNotNull().startsWith("jdbc:hive2:");
  }

}
