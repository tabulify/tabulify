package net.bytle.db.sqlite;

import net.bytle.db.Tabular;
import net.bytle.db.jdbc.*;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import net.bytle.type.MediaType;
import net.bytle.type.Variable;
import net.bytle.type.time.Date;
import net.bytle.type.time.Time;
import net.bytle.type.time.Timestamp;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.bytle.db.connection.ConnectionAttValueTimeDataType.EPOCH_MS;
import static net.bytle.db.connection.ConnectionAttValueTimeDataType.EPOCH_SEC;


public class SqliteConnection extends SqlConnection {


  private SqliteDataSystem sqliteDataSystem;
  private SqliteConnectionMetadata sqliteFeatures;

  public SqliteConnection(Tabular tabular, Variable name, Variable url) {

    super(tabular, name, url);

    // be sure that the repository exists
    Path databasePath = SqliteConnection.getPathFromUrl(url.getValueOrDefaultAsStringNotNull());
    Fs.createDirectoryIfNotExists(databasePath.getParent());

  }

  protected static Path getPathFromUrl(String url) {

    String localPath = url.substring(SqliteProvider.URL_PREFIX.length());
    if (localPath.startsWith(SqliteProvider.ROOT)) {
      localPath = localPath.substring(SqliteProvider.ROOT.length());
    }
    if (localPath.startsWith("file:")) {
      URI uri = URI.create(localPath);
      return Paths.get(uri);
    }
    return Paths.get(localPath);

  }


  @Override
  public SqliteDataPath getCurrentDataPath() {

    return getDataPath(DataUri.CURRENT_CONNECTION_PATH, SqlDataPathType.SCHEMA);
  }

  @Override
  public SqliteDataPath getDataPath(String pathOrName, MediaType mediaType) {

    return new SqliteDataPath(this, pathOrName, mediaType);

  }


  @Override
  public SqliteDataSystem getDataSystem() {
    if (sqliteDataSystem == null) {
      sqliteDataSystem = new SqliteDataSystem(this);
    }
    return sqliteDataSystem;
  }


  @Override
  public SqliteDataPath createSqlDataPath(String catalog, String schema, String name) {
    String resourcePath = SqlConnectionResourcePath.createOfCatalogSchemaAndObjectName(this, catalog, schema, name)
      .toRelative()
      .toString();
    return (SqliteDataPath) getDataPath(resourcePath);
  }


  @Override
  public SqlDataPath createScriptDataPath(DataPath dataPath) {
    return new SqliteDataPath(this, dataPath);
  }


  @Override
  public SqlConnectionMetadata getMetadata() {
    if (sqliteFeatures == null) {
      sqliteFeatures = new SqliteConnectionMetadata(this);
    }
    return sqliteFeatures;
  }

  @Override
  public Object toSqlObject(Object sourceObject, SqlDataType targetColumnType) throws CastException {


    // Default
    return super.toSqlObject(sourceObject, targetColumnType);

  }

  @Override
  public String toSqlString(Object objectInserted, SqlDataType targetDataType) {

    return super.toSqlString(objectInserted, targetDataType);
  }

  @Override
  public <T> T getObject(Object valueObject, Class<T> clazz) {
    if (clazz.equals(java.sql.Date.class)) {
      if (valueObject.getClass().equals(Long.class)) {
        if (this.getMetadata().getDateDataTypeOrDefault() == EPOCH_MS) {
          return clazz.cast(Date.createFromEpochMilli((Long) valueObject).toSqlDate());
        }
      } else if (valueObject.getClass().equals(Integer.class)) {
        if (this.getMetadata().getDateDataTypeOrDefault() == EPOCH_SEC) {
          return clazz.cast(Date.createFromEpochSec(((Integer) valueObject).longValue()).toSqlDate());
        }
      }
    } else if (clazz.equals(java.sql.Timestamp.class)) {

      try {
        long longValue = Casts.cast(valueObject, Long.class);
        switch (this.getMetadata().getTimestampDataType()) {
          case EPOCH_SEC:
            return clazz.cast(Timestamp.createFromEpochSec(longValue).toSqlTimestamp());
          case EPOCH_MS:
            return clazz.cast(Timestamp.createFromEpochMilli(longValue).toSqlTimestamp());
        }
      } catch (CastException e) {
        // not a long
      }


    } else if (clazz.equals(java.sql.Time.class)) {
      if (valueObject.getClass().equals(Integer.class)) {
        if (this.getMetadata().getTimeDataType() == EPOCH_MS) {
          return clazz.cast(Time.createFromEpochMs(((Integer) valueObject).longValue()).toSqlTime());
        }
      }
    }
    return super.getObject(valueObject, clazz);
  }
}
