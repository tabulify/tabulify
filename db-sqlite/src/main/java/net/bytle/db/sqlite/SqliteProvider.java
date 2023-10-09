package net.bytle.db.sqlite;

import net.bytle.db.Tabular;
import net.bytle.db.jdbc.SqlDataStoreProvider;
import net.bytle.type.Variable;

public class SqliteProvider extends SqlDataStoreProvider {

  public static final String URL_PREFIX = "jdbc:sqlite:";
  public static final String ROOT = "///";

  @Override
  public SqliteConnection getJdbcDataStore(Tabular tabular, Variable name, Variable url) {

    return new SqliteConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Variable url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }

}
