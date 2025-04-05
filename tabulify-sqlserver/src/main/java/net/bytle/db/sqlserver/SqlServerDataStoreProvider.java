package net.bytle.db.sqlserver;

import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.jdbc.SqlDataStoreProvider;
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
