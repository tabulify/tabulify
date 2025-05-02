package com.tabulify.oracle;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.jdbc.SqlDataStoreProvider;
import com.tabulify.conf.Attribute;

public class OraDataStoreProvider extends SqlDataStoreProvider {


  @Override
  public Connection getJdbcDataStore(Tabular tabular, Attribute name, Attribute url) {
    return new OracleConnection(tabular,name,url);
  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().contains("oracle");
  }

}
