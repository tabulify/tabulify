package net.bytle.db.oracle;

import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.jdbc.SqlDataStoreProvider;
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
