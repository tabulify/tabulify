package com.tabulify.sqlite;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.ConnectionAttValueTimeDataType;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.jdbc.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.uri.DataUriNode;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.type.Booleans;
import net.bytle.type.Casts;
import net.bytle.type.time.Date;
import net.bytle.type.time.Time;
import net.bytle.type.time.Timestamp;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.tabulify.connection.ConnectionAttValueTimeDataType.*;


public class SqliteConnection extends SqlConnection {


  private SqliteDataSystem sqliteDataSystem;
  private SqliteConnectionMetadata sqliteFeatures;


  public SqliteConnection(Tabular tabular, Attribute name, Attribute url) {

    super(tabular, name, url);

    // be sure that the repository exists
    Path databasePath = SqliteConnection.getPathFromUrl(url.getValueOrDefaultAsStringNotNull());
    Fs.createDirectoryIfNotExists(databasePath.getParent());

    this.addAttributesFromEnumAttributeClass(SqliteConnectionAttributeEnum.class);

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
  public String getCurrentSchema() {
    return DataUriNode.CURRENT_CONNECTION_PATH;
  }

  @Override
  public SqliteDataPath getCurrentDataPath() {
    return (SqliteDataPath) super.getCurrentDataPath();
  }


  @Override
  protected Supplier<SqlDataPath> getDataPathSupplier(String pathOrName, SqlMediaType mediaType) {
    return () -> new SqliteDataPath(this, pathOrName, mediaType);
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
  public SqlConnectionMetadata getMetadata() {
    if (sqliteFeatures == null) {
      sqliteFeatures = new SqliteConnectionMetadata(this);
    }
    return sqliteFeatures;
  }

  @Override
  public Object toSqlObject(Object sourceObject, SqlDataType<?> targetColumnType) throws CastException {


    // Default
    return super.toSqlObject(sourceObject, targetColumnType);

  }

  @Override
  public String toSqlString(Object objectInserted, ColumnDef<?> targetDataType) {

    return super.toSqlString(objectInserted, targetDataType);
  }

  @Override
  public <T> T getObjectFromResulSet(ResultSet resultSet, ColumnDef<?> columnDef, Class<T> clazz) {

    Object valueObject;
    try {
      valueObject = resultSet.getObject(columnDef.getColumnPosition());
    } catch (SQLException e) {
      throw new RuntimeException("Error while trying to retrieve the value of the column (" + columnDef + ") with the class " + clazz.getName() + ". Error: " + e.getMessage(), e);
    }

    /**
     * The {@link SqlConnection#toSqlObject(Object, SqlDataType)} has already the
     * one way {@link ConnectionAttValueTimeDataType} transformation
     * Should we move that to the parent or get the transformation back to SQLite
     * because it's used only for it ?
     */
    if (clazz.equals(java.sql.Date.class)) {
      ConnectionAttValueTimeDataType dateDataTypeOrDefault = this.getMetadata().getDateDataTypeOrDefault();
      if (valueObject.getClass().equals(Long.class)) {
        if (dateDataTypeOrDefault == EPOCH_MS) {
          return clazz.cast(Date.createFromEpochMilli((Long) valueObject).toSqlDate());
        }
      } else if (valueObject.getClass().equals(Integer.class)) {
        if (dateDataTypeOrDefault == EPOCH_SEC) {
          return clazz.cast(Date.createFromEpochSec(((Integer) valueObject).longValue()).toSqlDate());
        }
        if (dateDataTypeOrDefault == EPOCH_DAY) {
          return clazz.cast(Date.createFromEpochDay(((Integer) valueObject).longValue()).toSqlDate());
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
    } else if (clazz.equals(Boolean.class)) {
      return clazz.cast(Booleans.createFromObject(valueObject).toBoolean());
    }

    return super.getObjectFromResulSet(resultSet, columnDef, clazz);
  }

  @Override
  public List<Class<? extends ConnectionAttributeEnum>> getAttributeEnums() {
    ArrayList<Class<? extends ConnectionAttributeEnum>> enums = new ArrayList<>(super.getAttributeEnums());
    enums.add(SqliteConnectionAttributeEnum.class);
    return enums;
  }

  @Override
  public SqlDataType<?> getSqlDataTypeFromSourceColumn(ColumnDef<?> columnDef) {

    SqlDataType<?> dataType = columnDef.getDataType();
    String sqlNameLowerCase = dataType.toKeyNormalizer().toString().toLowerCase();
    SqlDataTypeAnsi standardSqlType = dataType.getAnsiType();


    if (this.getAffinityConversion()) {
      // int
      if (sqlNameLowerCase.contains("int")) {
        return this.getSqlDataType(SqliteTypeAffinity.INTEGER.toKeyNormalizer());
      }

      // real
      if (sqlNameLowerCase.contains("real") || sqlNameLowerCase.contains("floa") || sqlNameLowerCase.contains("doub")) {
        return this.getSqlDataType(SqliteTypeAffinity.REAL.toKeyNormalizer());
      }

      // text
      Set<SqlDataTypeAnsi> textDataTypes = Set.of(SqlDataTypeAnsi.CHARACTER, SqlDataTypeAnsi.NATIONAL_CLOB, SqlDataTypeAnsi.CLOB, SqlDataTypeAnsi.CHARACTER_VARYING, SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING);
      if (textDataTypes.contains(standardSqlType)) {
        return this.getSqlDataType(SqliteTypeAffinity.TEXT.toKeyNormalizer());
      }


      // blob
      Set<SqlDataTypeAnsi> blobDataTypes = Set.of(SqlDataTypeAnsi.BINARY, SqlDataTypeAnsi.VARBINARY, SqlDataTypeAnsi.LONG_VARBINARY, SqlDataTypeAnsi.BIT);
      if (blobDataTypes.contains(standardSqlType)) {
        return this.getSqlDataType(SqliteTypeAffinity.BLOB.toKeyNormalizer());
      }

      // numeric
      if (standardSqlType.equals(SqlDataTypeAnsi.DECIMAL) || standardSqlType.equals(SqlDataTypeAnsi.NUMERIC)) {
        return this.getSqlDataType(SqliteTypeAffinity.NUMERIC.toKeyNormalizer());
      }
    }

    // bit boolean
    if (standardSqlType == SqlDataTypeAnsi.BIT && columnDef.getPrecision() == 1) {
      return this.getSqlDataType(SqlDataTypeAnsi.BOOLEAN);
    }
    return super.getSqlDataTypeFromSourceColumn(columnDef);
  }

  public boolean getAffinityConversion() {
    return (boolean) this.getAttribute(SqliteConnectionAttributeEnum.TYPE_AFFINITY_CONVERSION).getValueOrDefault();
  }

}
