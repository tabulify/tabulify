package com.tabulify.sqlite;

import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlMediaType;
import com.tabulify.spi.DataPath;
import com.tabulify.exception.NoCatalogException;
import com.tabulify.exception.NoSchemaException;
import com.tabulify.type.MediaType;

public class SqliteDataPath extends SqlDataPath implements DataPath {


  public SqliteDataPath(SqliteConnection jdbcDataStore, String path, MediaType mediaType) {
    super(jdbcDataStore, path, null, mediaType);

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



  @Override
  public SqliteDataPath getSchema() throws NoSchemaException {

    throw new NoSchemaException("No schema is supported");

  }

  @Override
  public SqliteConnection getConnection() {
    return (SqliteConnection) super.getConnection();
  }


  @Override
  public SqliteDataPathRelationDef createEmptyRelationDef() {

    this.relationDef = new SqliteDataPathRelationDef(this, false);
    return (SqliteDataPathRelationDef) this.relationDef;

  }

  @Override
  public SqliteDataPathRelationDef createRelationDef() {
    this.relationDef = new SqliteDataPathRelationDef(this, true);
    return (SqliteDataPathRelationDef) relationDef;
  }

  @Override
  public SqliteDataPath resolve(String name, MediaType mediaType) {
    if (this.getMediaType() != SqlMediaType.SCHEMA) {
      throw new RuntimeException("In Sqlite, you can't ask a children only from a schema. You are asking a children from the " + SqlMediaType.SCHEMA + " (" + this + ")");
    }

    return this.getConnection().createSqlDataPath(null, null, name);

  }


}
