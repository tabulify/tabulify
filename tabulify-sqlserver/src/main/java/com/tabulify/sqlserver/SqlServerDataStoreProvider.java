package com.tabulify.sqlserver;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreProvider;
import net.bytle.type.Variable;

public class SqlServerDataStoreProvider extends SqlDataStoreProvider {

  public final String URL_PREFIX = "jdbc:sqlserver:";

  @Override
  public Connection getJdbcDataStore(Tabular tabular, Variable name, Variable url) {
    return new SqlServerConnection(tabular, name, url);
  }



  @Override
  public boolean accept(Variable url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }

}
