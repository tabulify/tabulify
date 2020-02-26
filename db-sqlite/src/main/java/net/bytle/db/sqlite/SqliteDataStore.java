package net.bytle.db.sqlite;

import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.type.Strings;

public class SqliteDataStore extends JdbcDataStore {


  public SqliteDataStore(String name, String url) {
    super(name, url);
  }

  @Override
  public SqliteDataPath getDataPath(String... names) {

    switch (names.length) {
      case 0:
        throw new RuntimeException("We can't create a path without names");
      case 1:
        switch (names[0]) {
          case JdbcDataPath.CURRENT_WORKING_DIRECTORY:
            return new SqliteDataPath(this, null, JdbcDataPath.CURRENT_WORKING_DIRECTORY, null);
          case JdbcDataPath.PARENT_DIRECTORY:
            throw new RuntimeException("Sqlite does not have the notion of catalog or schema, you can't ask therefore for a parent");
          default:
            return new SqliteDataPath(this, null, null, names[0]);
        }
      case 2:
        if (names[0].equals(JdbcDataPath.CURRENT_WORKING_DIRECTORY)) {
          return new SqliteDataPath(this, null, null, names[1]);
        }
      default:
        throw new RuntimeException(
          Strings.multiline("Sqlite does not support any catalog or schema",
            "A path can not have more than one name. ",
            "We got more than one (" + names + ")"));
    }


  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

}
