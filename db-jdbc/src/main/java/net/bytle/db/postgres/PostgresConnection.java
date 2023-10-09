package net.bytle.db.postgres;

import net.bytle.db.Tabular;
import net.bytle.db.jdbc.SqlConnection;
import net.bytle.db.jdbc.SqlDataPath;
import net.bytle.db.jdbc.SqlDataPathType;
import net.bytle.type.MediaType;
import net.bytle.type.Variable;

public class PostgresConnection extends SqlConnection {

  public PostgresConnection(Tabular tabular, Variable name, Variable url) {
    super(tabular, name, url);
  }


  @Override
  public PostgresDataSystem getDataSystem() {
    return new PostgresDataSystem(this);
  }


  @Override
  public SqlDataPath getDataPath(String path, MediaType mediaType) {

    return new PostgresDataPath(this, path, mediaType);

  }


}
