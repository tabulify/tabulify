package com.tabulify.jdbc;

import com.tabulify.DbLoggers;
import com.tabulify.model.*;
import com.tabulify.spi.StrictException;
import com.tabulify.spi.Tabulars;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NoCatalogException;
import com.tabulify.exception.NoColumnException;
import com.tabulify.exception.NoSchemaException;
import com.tabulify.type.KeyNormalizer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


public class SqlDataPathRelationDef extends RelationDefDefault {


  public SqlDataPathRelationDef(SqlDataPath dataPath, Boolean buildFromMeta) {
    super(dataPath);
    if (!buildFromMeta) {
      return;
    }
    switch (this.getDataPath().getMediaType()) {
      case TABLE:
      case VIEW:
      case SYSTEM_VIEW:
      case SYSTEM_TABLE:

        if (!Tabulars.exists(this.getDataPath())) {
          return;
        }

        // Columns building
        addColumnsFromMetadata();
        // Pk Building
        addPrimaryKeyFromMetaData();
        // Foreign Key building
        addForeignKeysFromMetadata();
        // Unique Key
        addUniqueKeysFromMetaData();

      case REQUEST:
        /**
         * Done in {@link SqlRequestRelationDef}
         */
        break;
      default:
        // no structure
    }


  }


  @Override
  public SqlDataPath getDataPath() {
    return (SqlDataPath) super.getDataPath();
  }

  /**
   * Add the columns from the database data store metadata
   */
  protected void addColumnsFromMetadata() {

    List<SqlMetaColumn> metaColumns = this.getDataPath().getConnection().getDataSystem().getMetaColumns(this.getDataPath());
    for (SqlMetaColumn meta : metaColumns) {


      SqlConnection connection = this.getDataPath().getConnection();
      KeyNormalizer typeName;
      try {
        typeName = KeyNormalizer.create(meta.getTypeName());
      } catch (CastException e) {
        throw new RuntimeException("The column " + meta.getColumnName() + " has a type value that is not a valid identifier string. Error: " + e.getMessage());
      }
      Integer typeCode = meta.getTypeCode();

      SqlDataType<?> dataType = connection.getSqlDataType(typeName, typeCode);
      if (dataType == null) {
        String message = "The column " + meta.getColumnName() + " has a unknown type name/type code (" + typeName + "/" + typeCode + ") for the system " + connection.getDatabaseName() + " (connection " + connection + ").\nThe actual defined data type are: " + connection.getSqlDataTypes()
          .stream()
          .filter(SqlDataType::getIsSupported)
          .map(SqlDataType::getKey)
          .distinct()
          .sorted()
          .map(SqlDataTypeKey::toString)
          .collect(Collectors.joining(", "));
        if (getDataPath().getConnection().getTabular().isStrictExecution()) {
          throw new StrictException(message);
        }
        dataType = connection.getSqlDataType(typeName, typeCode);
        if (dataType == null) {
          throw new RuntimeException(message);
        }
      }


      /**
       * For Postgresql, Sqlserver, by default
       * the precision of the time is in the decimal digit
       */
      int scale;
      int precision;
      if (SqlDataTypes.timeTypes.contains(dataType.getAnsiType())) {
        precision = meta.getDecimalDigits();
        scale = 0;
      } else {
        precision = meta.getColumnSize();
        scale = meta.getDecimalDigits();
      }

      /**
       * Driver may return
       * * precision that are greater than the max precision of the type
       * * precision value for a type without scale
       * (We see you Postgres)
       */
      int maxPrecision = dataType.getMaxPrecision();
      if (dataType.hasPrecision()) {
        if (precision > maxPrecision) {
          precision = maxPrecision;
        }
      } else {
        precision = 0;
      }

      int maximumScale = dataType.getMaximumScale();
      if (dataType.hasScale()) {
        if (scale > maximumScale) {
          scale = maximumScale;
        }
      } else {
        scale = 0;
      }

      this.getOrCreateColumn(meta.getColumnName(), dataType)
        .setPrecision(precision)
        .setScale(scale)
        .setIsAutoincrement(meta.isAutoIncrement())
        .setIsGeneratedColumn(meta.isGeneratedColumn())
        .setNullable(SqlDataTypeNullable.cast(meta.isNullable()))
        .setComment(meta.getComment())
      ;
    }
  }


  /**
   * Build Foreign Key based on
   * {@link DatabaseMetaData#getImportedKeys(String, String, String)}
   * <p>
   * See also the counter-part:
   * * Same schema
   * {@link DatabaseMetaData#getExportedKeys(String, String, String)}
   * * Cross Schmea ?
   * {@link DatabaseMetaData#getCrossReference(String, String, String, String, String, String)}
   */
  protected void addForeignKeysFromMetadata() {


    // Just to hold the data a list of all fk columns values
    List<SqlMetaForeignKey> metaForeignKeys = this.getDataPath()
      .getConnection()
      .getDataSystem()
      .getMetaForeignKeys(this.getDataPath());

    /**
     * For every primary table, create the foreign key
     */
    String catalogName;
    try {
      catalogName = this.getDataPath().getCatalogDataPath().getName();
    } catch (NoCatalogException e) {
      catalogName = null;
    }
    for (SqlMetaForeignKey sqlMetaForeignKey : metaForeignKeys) {

      /**
       * Bug: The catalog name may become null
       * from the driver
       */
      String primaryTableCatalogName = sqlMetaForeignKey.getPrimaryTableCatalogName();
      if (
        (primaryTableCatalogName == null || primaryTableCatalogName.isEmpty())
          &&
          (catalogName != null && !catalogName.isEmpty())
      ) {
        primaryTableCatalogName = catalogName;
      }

      /**
       * Build the primary key
       */
      SqlDataPath primaryKeyTable = this.getDataPath().getConnection().createSqlDataPath(
        primaryTableCatalogName,
        sqlMetaForeignKey.getPrimaryTableSchemaName(),
        sqlMetaForeignKey.getPrimaryTableName()
      );
      final PrimaryKeyDef primaryKey = primaryKeyTable.getOrCreateRelationDef().getPrimaryKey();
      if (primaryKey == null) {
        throw new RuntimeException("The primary table (" + sqlMetaForeignKey + ") has no primary key");
      }
      this
        .foreignKeyOf(primaryKey, sqlMetaForeignKey.getForeignKeyColumns())
        .setName(sqlMetaForeignKey.getName());

    }

  }

  /**
   * Add the primary key that are in the data store metadata
   * <p>
   * A wrapper around {@link DatabaseMetaData}
   * Note: there is also {@link DatabaseMetaData#getBestRowIdentifier(String, String, String, int, boolean)}
   * What's the difference, I still don't know.
   */
  protected void addPrimaryKeyFromMetaData() {

    try {

      final SqlDataPath dataPath = this.getDataPath();
      final String column_name = "COLUMN_NAME";
      final String pk_name = "PK_NAME";
      final String key_seq = "KEY_SEQ";
      List<String> pkProp = new ArrayList<>();
      pkProp.add(column_name);
      pkProp.add(pk_name);
      pkProp.add(key_seq);

      // Primary Key building
      String schemaName;
      try {
        schemaName = dataPath.getSchema().getName();
      } catch (NoSchemaException e) {
        schemaName = null;
      }

      String catalogName;
      try {
        catalogName = dataPath.getCatalogDataPath().getName();
      } catch (NoCatalogException e) {
        catalogName = null;
      }

      ResultSet pkResultSet = dataPath.getConnection().getCurrentJdbcConnection().getMetaData().getPrimaryKeys(catalogName, schemaName, dataPath.getName());
      // Collect all the data because we don't know if they will be in order
      // and because in a recursive call, the result set may be closed
      List<Map<String, String>> pkColumns = new ArrayList<>();
      String pkName = "";
      while (pkResultSet.next()) {
        Map<String, String> pkProps = new HashMap<>();
        pkColumns.add(pkProps);
        for (String prop : pkProp) {
          pkProps.put(prop, pkResultSet.getString(prop));
        }
        pkName = pkResultSet.getString(pk_name);
      }
      pkResultSet.close();

      /**
       * Processing
       */
      List<String> columns = pkColumns
        .stream()
        .sorted(Comparator.comparing(o -> Integer.valueOf(o.get(key_seq))))
        .map(s -> s.get(column_name))
        // Pfff
        // MySQL returns 2 column with the same name
        .distinct()
        .collect(Collectors.toList());

      if (!columns.isEmpty()) {
        this.primaryKeyOf(columns.toArray(new String[0]))
          .setName(pkName);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Get the unique key from the data store metadata and
   * add them to this data definition.
   * <p>
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   * This function must be called after the function {@link #addPrimaryKeyFromMetaData()}
   * because the getIndex function of JDBC returns also the unique index of the primary
   * key. We need then the primary key information in order to exclude it from the building
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   */
  protected void addUniqueKeysFromMetaData() {

    // Collect all data first because we need all columns that make a unique key before
    // building the object
    Map<String, Map<Integer, String>> indexData = new HashMap<>();
    final String ordinal_position_alias = "ORDINAL_POSITION";
    final String column_name_alias = "COLUMN_NAME";
    final SqlDataPath dataPath = this.getDataPath();
    String schema;
    try {
      schema = dataPath.getSchema().getName();
    } catch (NoSchemaException e) {
      schema = null;
    }
    String catalog;
    try {
      catalog = dataPath.getCatalogDataPath().getName();
    } catch (NoCatalogException e) {
      catalog = null;
    }
    DatabaseMetaData metaData = dataPath.getConnection().getMetadata().getDatabaseMetaData();
    try (
      ResultSet indexResultSet = metaData.getIndexInfo(catalog, schema, dataPath.getName(), true, true)
    ) {
      while (indexResultSet.next()) {

        String index_name = indexResultSet.getString("INDEX_NAME");

        if (index_name == null) {
          continue;
        }

        Map<Integer, String> indexProperties = indexData.get(index_name);
        //noinspection Java8MapApi
        if (indexProperties == null) {
          indexProperties = new HashMap<>();
          indexData.put(index_name, indexProperties);
        }
        indexProperties.put(indexResultSet.getInt(ordinal_position_alias), indexResultSet.getString(column_name_alias));

      }

    } catch (SQLException e) {
      String s = "Error when getting the unique key (via Jdbc IndexInfo function) for the table (" + dataPath + "): ";
      throw new RuntimeException(s + e.getMessage(), e);
    }

    // Process the data
    for (String indexName : indexData.keySet()) {
      try {
        Map<Integer, String> indexProperties = indexData.get(indexName);

        // Sort the column by order
        List<Integer> positions = new ArrayList<>(indexProperties.keySet());
        List<ColumnDef> columnDefs = new ArrayList<>();
        Collections.sort(positions);
        for (Integer pos : positions) {
          ColumnDef columnDef = this.getColumnDef(indexProperties.get(pos));
          columnDefs.add(columnDef);
        }

        // We don't want the unique index of the primary key
        PrimaryKeyDef primaryKeyDef = this.getPrimaryKey();
        if (primaryKeyDef != null) {
          if (primaryKeyDef.getColumns().equals(columnDefs)) {
            continue;
          }
        }

        // Construct the unique key
        String[] columnNames = columnDefs
          .stream()
          .map(ColumnDef::getColumnName)
          .toArray(String[]::new);
        this.getOrCreateUniqueKey(columnNames).name(indexName);

      } catch (NoColumnException e) {
        String s = "A column could not be found for the index (" + indexName + ") for the resource (" + this.getDataPath() + "). " + e.getMessage();
        if (this.getDataPath().getConnection().getTabular().isIdeEnv()) {
          throw new IllegalStateException(s);
        }
        DbLoggers.LOGGER_DB_ENGINE.warning("An index column was discarded. " + s);
      }
    }


  }


}
