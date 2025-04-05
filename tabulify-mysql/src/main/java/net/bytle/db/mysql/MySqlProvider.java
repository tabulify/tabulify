package net.bytle.db.mysql;

import net.bytle.db.Tabular;
import net.bytle.db.jdbc.SqlDataStoreProvider;
import net.bytle.type.Variable;

public class MySqlProvider extends SqlDataStoreProvider {

  public final String URL_PREFIX = "jdbc:mysql:";

  @Override
  public MySqlConnection getJdbcDataStore(Tabular tabular, Variable name, Variable url) {

    return new MySqlConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Variable url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }

}
