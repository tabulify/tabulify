package com.tabulify.sqlite;

import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlMediaType;
import com.tabulify.spi.DataPath;
import net.bytle.exception.NoCatalogException;
import net.bytle.exception.NoSchemaException;
import net.bytle.type.MediaType;

public class SqliteDataPath extends SqlDataPath implements DataPath {


  public SqliteDataPath(SqliteConnection jdbcDataStore, String path, MediaType mediaType) {
    super(jdbcDataStore, path, mediaType);

    try {
      String catalog = this.getSqlConnectionResourcePath().getCatalogPart();
      throw new IllegalStateException("Sqlite does not have the notion of catalog. The catalog should be null bit was (" + catalog + ")");
    } catch (NoCatalogException e) {
      // ok
    }

    try {
      String schema = this.getSqlConnectionResourcePath().getSchemaPart();
      throw new IllegalStateException("Sqlite does not have the notion of schema. The schema should be null bit was (" + schema + ")");
    } catch (NoSchemaException e) {
      // ok
    }

  }


  public SqliteDataPath(SqliteConnection sqliteConnection, DataPath dataPath) {
    super(sqliteConnection, dataPath);
  }


  @Override
  public SqliteDataPath getSchema() throws NoSchemaException {

    throw new NoSchemaException("No schema is supported");

  }

  @Override
  public SqliteConnection getConnection() {
    return (SqliteConnection) super.getConnection();
  }

  @Override
  public SqliteRelationDef getOrCreateRelationDef() {
    if (relationDef == null) {
      relationDef = new SqliteRelationDef(this, true);
    }
    return (SqliteRelationDef) relationDef;
  }

  @Override
  public SqliteRelationDef createRelationDef() {
    relationDef = new SqliteRelationDef(this, false);
    return (SqliteRelationDef) relationDef;
  }

  @Override
  public SqliteDataPath getChild(String name) {
    if (this.getMediaType() != SqlMediaType.SCHEMA) {
      throw new RuntimeException("In Sqlite, you can't ask a children only from a schema. You are asking a children from the " + SqlMediaType.SCHEMA + " (" + this + ")");
    }

    return this.getConnection().createSqlDataPath(null, null, name);

  }


}
