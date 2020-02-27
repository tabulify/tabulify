package net.bytle.db.sqlite;

import net.bytle.db.jdbc.AnsiDataDef;

public class SqliteDataDef extends AnsiDataDef {

  public SqliteDataDef(SqliteDataPath dataPath) {
    super(dataPath);
  }

  @Override
  public SqliteDataPath getDataPath() {
    return (SqliteDataPath) super.dataPath;
  }

}
