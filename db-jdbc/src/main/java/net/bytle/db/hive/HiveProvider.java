package net.bytle.db.hive;

import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.jdbc.SqlDataStoreProvider;
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
