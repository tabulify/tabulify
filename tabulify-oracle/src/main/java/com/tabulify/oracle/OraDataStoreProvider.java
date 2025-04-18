package com.tabulify.oracle;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreProvider;
import net.bytle.type.Variable;

public class OraDataStoreProvider extends SqlDataStoreProvider {


  @Override
  public Connection getJdbcDataStore(Tabular tabular, Variable name, Variable url) {
    return new OracleConnection(tabular,name,url);
  }

  @Override
  public boolean accept(Variable url) {
    return url.getValueOrDefaultAsStringNotNull().contains("oracle");
  }

}
