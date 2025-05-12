package com.tabulify.jdbc;

import com.tabulify.DbLoggers;
import com.tabulify.fs.sql.SqlQuery;
import com.tabulify.fs.sql.SqlQueryColumnIdentifierExtractor;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlDataType;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.SelectStream;
import net.bytle.crypto.Digest;
import net.bytle.exception.*;
import net.bytle.type.Enums;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.tabulify.jdbc.SqlDataPathAttribute.QUERY_METADATA_DETECTION;
import static com.tabulify.jdbc.SqlDataPathQueryMetadataDetectionMethod.TEMPORARY_VIEW;


public class SqlRelationDef extends RelationDefDefault {


  /**
   *
   */
  public SqlRelationDef(SqlDataPath dataPath, Boolean buildFromMeta) {
    super(dataPath);
    if (buildFromMeta) {
      // Do we have already a structure in the database
      if (Tabulars.exists(this.getDataPath())) {

        // Columns building
        addColumnsFromMetadata();
        // Pk Building
        addPrimaryKeyFromMetaData();
        // Foreign Key building
        addForeignKeysFromMetadata();
        // Unique Key
        addUniqueKeysFromMetaData();


      } else {


        if (this.getDataPath().getMediaType() == SqlMediaType.SCRIPT) {

          /**
           * Query
           *
           * To extract the columns identifier and structure, there is an history of three methods
           *   * the first one was to get the structure by sending the query and retrieving the structure from the {@link DatabaseMetaData}
           *   via the {@link SqlSelectStream#getRuntimeDataDef(DataDef)}
           *   * then we parsed the script via {@link SqlQueryColumnIdentifierExtractor} because we needed it to create SQL data processing script such as upsert, create as
           *   * then we saw that we could create a temporary view, read the metadata and delete it
           *
           * We use the {@link SqlDataPathQueryMetadataDetectionMethod#TEMPORARY_VIEW} but the code
           * is still here to show the history
           */
          SqlDataPathQueryMetadataDetectionMethod queryMetadataDetectionMethod;
          try {
            queryMetadataDetectionMethod = dataPath.getAttributeSafe(QUERY_METADATA_DETECTION).getValueOrDefaultCastAs(SqlDataPathQueryMetadataDetectionMethod.class);
          } catch (NoValueException e) {
            // should not
            queryMetadataDetectionMethod = TEMPORARY_VIEW;
          } catch (CastException e) {
            throw new RuntimeException("The value of the variable " + QUERY_METADATA_DETECTION + " is not conform. You can use any of the following " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(SqlDataPathQueryMetadataDetectionMethod.class), e);
          }
          if (queryMetadataDetectionMethod == TEMPORARY_VIEW && !this.getDataPath().getQuery().toLowerCase().contains("select")) {
            // query can be other thing than a select
            // Example: `PRAGMA table_info(f_sales)`
            // creating a temporary view will not work because this is not a sql statement
            // not the best, but it works as we read the columns after query execution.
            return;
          }
          switch (queryMetadataDetectionMethod) {
            //noinspection ConstantConditions
            case TEMPORARY_VIEW:

              /**
               * We don't want to see the creation/dropping
               * of the view in the INFO log
               */
              Level oldLevel = DbLoggers.LOGGER_DB_ENGINE.getLevel();
              SqlLog.LOGGER_DB_JDBC.setLevel(Level.WARNING);
              SqlConnection dataStore = this.getDataPath().getConnection();
              SqlDataSystem dataSystem = dataStore.getDataSystem();
              try {

                /**
                 * Due to side effect such as recursive call, we create another
                 * data path view
                 * A drop may ask for foreign key of the runtime data resource
                 * creating a recursive call
                 */
                String name = "tmp_tabulify_" + Digest.createFromString(Digest.Algorithm.MD5, this.getDataPath().getQuery()).getHashHex();
                DataPath scriptDataPath = this.getDataPath().getConnection().getTabular().getAndCreateRandomMemoryDataPath()
                  .setContent(this.getDataPath().getQuery())
                  .setLogicalName(name);
                SqlDataPath temporaryQueryDataPath = dataStore.createScriptDataPath(scriptDataPath);

                SqlDataPath view = dataSystem.createViewFromQueryDataPath(temporaryQueryDataPath);
                this.mergeStruct(view);
                // Drop the temp view
                dataSystem.drop(view);

              } catch (Exception e) {
                /**
                 * You can't create a view when the column name are the same
                 * Example: ERROR: column "avg" specified more than once
                 * Or Sql Server: The ORDER BY clause is invalid in views, inline functions, derived tables, subqueries, and common table expressions, unless TOP, OFFSET or FOR XML is also specified.
                 */
                throw new RuntimeException("Error when getting the metadata of the sql query: " + e.getMessage(), e);

              } finally {

                SqlLog.LOGGER_DB_JDBC.setLevel(oldLevel);
              }
              break;
            case RESULT_SET:
              /**
               * Weakness:
               *   * the query will run
               * <p></p>
               * This is now mandatory to get the data def at runtime
               * with the function {@link SelectStream#getRuntimeRelationDef()}
               *
               * @deprecated
               */
              throw new RuntimeException("Deprecated");
            case PARSING:
              /**
               * Weakness
               * This method:
               *   * returns only the identifier not the data type (data type is then VARCHAR for all columns)
               *   * does not work with the star `select * from`
               */
              parseQueryAndAddColumns();
              break;
          }


        }
      }
    }
  }

  /**
   * Parse the query, extract the column identifier
   * and add the columns accordingly
   */
  private void parseQueryAndAddColumns() {
    List<String> columnIdentifiers = SqlQuery
      .createFromString(getDataPath().getScript())
      .createColumnIdentifierExtractor()
      .setFunctionNameAsIdentifier(true)
      .setLowerCaseIdentifier(true)
      .extractColumnIdentifiers();
    for (int i = 0; i < columnIdentifiers.size(); i++) {
      String columnName = columnIdentifiers.get(i);
      if (!this.hasColumn(columnName)) {
        this.addColumn(columnName);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning("The column name (" + columnName + ") is specified more than once in the query (" + this.getDataPath() + "). This will surely cause a problem during a data transfer.");
        /**
         * Postgres can returns the same name for the columns
         * (ie the name of the function for instance)
         * If we have two avg function, we will get two columns with the same name `avg`
         */
        this.addColumn(columnName + "-" + i);
      }

    }

  }

  @Override
  public SqlDataPath getDataPath() {
    return (SqlDataPath) super.getDataPath();
  }

  /**
   * Add the columns from the data store metadata
   */
  protected void addColumnsFromMetadata() {

    this.getDataPath().getConnection().getDataSystem().getMetaColumns(this.getDataPath())
      .forEach(meta -> {
          Integer typeCode = meta.getTypeCode();
          SqlDataType dataType;
          if (typeCode != null) {
            dataType = this.getDataPath().getConnection().getSqlDataType(typeCode);
          } else {
            String typeName = meta.getTypeName();
            if (typeName == null) {
              throw new RuntimeException("The column " + meta.getColumnName() + " has no type code or name");
            }
            dataType = this.getDataPath().getConnection().getSqlDataType(typeName);
            if (dataType == null) {
              throw new RuntimeException("The column " + meta.getColumnName() + " has a unknown type name (" + typeName + ")");
            }
          }
          Class<?> sqlClass = dataType.getSqlClass();
          this.getOrCreateColumn(meta.getColumnName(), dataType, sqlClass)
            .precision(meta.getPrecision())
            .scale(meta.getScale())
            .setIsAutoincrement(meta.isAutoIncrement())
            .setIsGeneratedColumn(meta.isGeneratedColumn())
            .setNullable(meta.isNullable());
        }
      );

  }


  /**
   * Build Foreign Key based on
   * {@link DatabaseMetaData#getImportedKeys(String, String, String)}
   * <p>
   * See also the counter part:
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
        (primaryTableCatalogName == null || primaryTableCatalogName.equals(""))
          &&
          (catalogName != null && !catalogName.equals(""))
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

      ResultSet pkResultSet = dataPath.getConnection().getCurrentConnection().getMetaData().getPrimaryKeys(catalogName, schemaName, dataPath.getName());
      // Collect all the data because we don't known if they will be in order
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

      List<String> columns = pkColumns
        .stream()
        .sorted(Comparator.comparing(o -> Integer.valueOf(o.get(key_seq))))
        .map(s -> s.get(column_name))
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
