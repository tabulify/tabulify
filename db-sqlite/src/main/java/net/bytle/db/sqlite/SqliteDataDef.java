package net.bytle.db.sqlite;

import net.bytle.db.jdbc.JdbcDataDef;
import net.bytle.db.jdbc.JdbcDataPath;

public class SqliteDataDef extends JdbcDataDef {

  private final SqliteDataPath sqliteDataPath;

  public SqliteDataDef(SqliteDataPath dataPath) {

    super(dataPath);
    this.sqliteDataPath = dataPath;
  }

  @Override
  public JdbcDataPath getDataPath() {
    return sqliteDataPath;
  }

}
