package net.bytle.db.jdbc;

import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;
import net.bytle.log.Log;
import net.bytle.type.Strings;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.db.jdbc.AnsiDataStore.DB_SQLITE;

/**
 * Static method
 */
public class Jdbcs {


  private static final Log LOGGER = JdbcDataSystemLog.LOGGER_DB_JDBC;


  public static List<DataPath> getChildrenDataPath(JdbcDataPath jdbcDataPath) {
    return getDescendants(jdbcDataPath, null);
  }

  public static List<DataPath> getDescendants(JdbcDataPath jdbcDataPath, String tableNamePattern) {

    List<DataPath> jdbcDataPaths = new ArrayList<>();
    try {

      String schema = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : null;
      String catalog = jdbcDataPath.getCatalog();
      String tableName = tableNamePattern;

      ResultSet tableResultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getTables(catalog, schema, tableName, null);
      while (tableResultSet.next()) {
        final String table_name = tableResultSet.getString("TABLE_NAME");
        final String schema_name = tableResultSet.getString("TABLE_SCHEM");
        final String cat_name = tableResultSet.getString("TABLE_CAT");
        final String type_name = tableResultSet.getString("TABLE_TYPE");
        JdbcDataPath childDataPath = JdbcDataPath.of(jdbcDataPath.getDataStore(), cat_name, schema_name, table_name)
          .setType(type_name);
        jdbcDataPaths.add(childDataPath);
      }


    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return jdbcDataPaths;

  }

  /**
   * @param jdbcDataPath
   * @return a list of data path that reference the primary key of the jdbcDataPath
   */
  public static List<DataPath> getReferencingDataPaths(JdbcDataPath jdbcDataPath) {

    List<DataPath> jdbcDataPaths = new ArrayList<>();
    try {

      String schema = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName() : null;
      String catalog = jdbcDataPath.getCatalog();
      String tableName = jdbcDataPath.getName();

      ResultSet tableResultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getExportedKeys(catalog, schema, tableName);
      while (tableResultSet.next()) {
        final String table_name = tableResultSet.getString("FKTABLE_NAME");
        final String schema_name = tableResultSet.getString("FKTABLE_SCHEM");
        final String cat_name = tableResultSet.getString("FKTABLE_CAT");
        JdbcDataPath fkDataPath = JdbcDataPath.of(jdbcDataPath.getDataStore(), cat_name, schema_name, table_name);
        jdbcDataPaths.add(fkDataPath);
      }


    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return jdbcDataPaths;
  }

  private static void buildPrimaryKey(RelationDef tableDef) throws SQLException {


    // Bug in SQLite Driver - Hack
    // that doesn't return the good primary ley
    final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
    final String column_name = "COLUMN_NAME";
    final String pk_name = "PK_NAME";
    final String key_seq = "KEY_SEQ";
    List<String> pkProp = new ArrayList<>();
    pkProp.add(column_name);
    pkProp.add(pk_name);
    pkProp.add(key_seq);

    // Primary Key building
    String schemaName = null;
    if (dataPath.getSchema() != null) {
      schemaName = dataPath.getSchema().getName();
    }
    ResultSet pkResultSet = dataPath.getDataStore().getCurrentConnection().getMetaData().getPrimaryKeys(dataPath.getCatalog(), schemaName, dataPath.getName());
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

    if (columns.size() > 0) {
      tableDef.primaryKeyOf(columns.toArray(new String[0]))
        .setName(pkName);
    }


  }

  /**
   * Build a table from a database
   * if no table is found, return null
   * The table of a schema but the whole schema will not be build
   *
   * @param tableDef
   * @return null if no table is found
   */
  public static RelationDef getTableDef(RelationDef tableDef) {


    try {
      JdbcDataPath jdbcDataPath = (JdbcDataPath) tableDef.getDataPath();
      LOGGER.fine("Building the table structure for the data path (" + jdbcDataPath + ")");

      String[] types = {"TABLE"};

      final JdbcDataPath schemaPath = jdbcDataPath.getSchema();
      String schema = null;
      if (schemaPath != null) {
        schema = schemaPath.getName();
      }
      String catalog = jdbcDataPath.getCatalog();
      String tableName = jdbcDataPath.getName();

      ResultSet tableResultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getTables(catalog, schema, tableName, types);
      boolean tableExist = tableResultSet.next(); // For TYPE_FORWARD_ONLY

      if (!tableExist) {

        tableResultSet.close();
        return tableDef;

      } else {

        jdbcDataPath.setType(tableResultSet.getString("TABLE_TYPE"));
        tableResultSet.close();

        // Columns building
        buildTableColumns(tableDef);
        // Pk Building
        buildPrimaryKey(tableDef);
        // Foreign Key building
        buildForeignKey(tableDef);
        // Unique Key
        buildUniqueKey(tableDef);

        // Return the table
        return tableDef;

      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }


  }

  private static void buildTableColumns(RelationDef tableDef) throws SQLException {

    final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();


    String schemaName = null;
    if (dataPath.getSchema() != null) {
      schemaName = dataPath.getSchema().getName();
    }
    ResultSet columnResultSet = dataPath.getDataStore().getCurrentConnection().getMetaData().getColumns(dataPath.getCatalog(), schemaName, dataPath.getName(), null);

    while (columnResultSet.next()) {

      String isGeneratedColumn = "";
      try {
        isGeneratedColumn = columnResultSet.getString("IS_GENERATEDCOLUMN");
      } catch (SQLException e) {
        // Not always supported
      }

      String column_name = columnResultSet.getString("COLUMN_NAME");

      String is_autoincrement = null;
      // Not implemented by the sqliteDriver
      try {
        is_autoincrement = columnResultSet.getString("IS_AUTOINCREMENT");
      } catch (SQLException e) {
        LOGGER.fine("The IS_AUTOINCREMENT column seems not to be implemented. Message: " + e.getMessage());
      }

      int column_size = columnResultSet.getInt("COLUMN_SIZE");


      final int sqlTypeCode = columnResultSet.getInt("DATA_TYPE");

      SqlDataType dataType = tableDef.getDataPath().getDataStore().getSqlDataType(sqlTypeCode);
      tableDef.getColumnOf(column_name, dataType.getClazz())
        .typeCode(sqlTypeCode)
        .precision(column_size)
        .scale(columnResultSet.getInt("DECIMAL_DIGITS"))
        .isAutoincrement(is_autoincrement)
        .isGeneratedColumn(isGeneratedColumn)
        .setNullable(columnResultSet.getInt("NULLABLE"));

    }
    columnResultSet.close();


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
   *
   * @param tableDef
   */
  private static void buildForeignKey(RelationDef tableDef) {

    // SQLite Driver doesn't return a empty string as key name
    // for all foreigns key
    final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
    AnsiDataStore dataStore = dataPath.getDataStore();

    // The column names of the fkresult set
    String col_fk_name = "FK_NAME";
    String col_fkcolumn_name = "FKCOLUMN_NAME";
    String col_fktable_schem = "FKTABLE_SCHEM";
    String col_fktable_cat = "FKTABLE_CAT";
    String col_fktable_name = "FKTABLE_NAME";
    //  --- Pk referenced
    String col_pkcolumn_name = "PKCOLUMN_NAME";
    String col_pktable_name = "PKTABLE_NAME";
    String col_pktable_schem = "PKTABLE_SCHEM";
    String col_pktable_cat = "PKTABLE_CAT";
    String col_pk_name = "PK_NAME";
    //  ---- Column seq for FK and PK
    String col_key_seq = "KEY_SEQ";


    List<String> resultSetColumnNames = Arrays.asList(
      col_fk_name,
      col_fkcolumn_name,
      col_fktable_schem,
      col_fktable_cat,
      col_fktable_name,
      col_pkcolumn_name,
      col_pktable_name,
      col_pktable_schem,
      col_pktable_cat,
      col_pk_name,
      col_key_seq);

    // Collect the data before processing it
    // because of the build that have a recursion nature, the data need first to be collected
    // processing the data and calling recursively the creation of an other table
    // with foreign key result in a "result set is closed" exception within the Ms Sql Driver

    // Just to hold the data a list of all fk columns values
    List<Map<String, String>> fkDatas = new ArrayList<>();

    String schemaName = null;
    if (dataPath.getSchema() != null) {
      schemaName = dataPath.getSchema().getName();
    }
    try (
      // ImportedKey = the primary keys imported by a table
      ResultSet fkResultSet = dataStore.getCurrentConnection().getMetaData().getImportedKeys(dataPath.getCatalog(), schemaName, dataPath.getName());
    ) {

      while (fkResultSet.next()) {

        // The foreign key name may be null
        Map<String, String> fkProperties = resultSetColumnNames
          .stream()
          .collect(Collectors.toMap(s -> s, s -> {
            try {
              String string = fkResultSet.getString(s);
              return string == null ? "" : string;
            } catch (Exception e) {
              String msg = "Error when retrieving the string value of " + s;
              throw new RuntimeException(msg, e);
            }
          }));
        fkDatas.add(fkProperties);

      }

    } catch (Exception e) {
      String s = Strings.multiline("Error when building Foreign Key (ie imported keys) for the table " + dataPath,
        e.getMessage());
      LOGGER.severe(s);
      System.err.println(s);
      if (dataStore.isStrict()) {
        throw new RuntimeException(e);
      } else {
        return;
      }
    }

    // How much foreign key (ie how much foreign key tables)
    List<JdbcDataPath> foreignTableNames = fkDatas.stream()
      .distinct()
      .map(s -> JdbcDataPath.of(dataStore, s.get(col_pktable_cat), s.get(col_pktable_schem), s.get(col_pktable_name)))
      .collect(Collectors.toList());


    for (JdbcDataPath foreignTable : foreignTableNames) {
      Map<Integer, String> cols = new HashMap<>();
      String fk_name = "";
      for (Map<String, String> fkData : fkDatas) {
        if (fkData.get(col_pktable_name).equals(foreignTable.getName())) {
          cols.put(Integer.valueOf(fkData.get(col_key_seq)), fkData.get(col_fkcolumn_name));
          fk_name = fkData.get(col_fk_name);
        }
      }
      List<String> columns = cols.keySet().stream()
        .sorted()
        .map(cols::get)
        .collect(Collectors.toList());

      final PrimaryKeyDef primaryKey = foreignTable.getDataDef().getPrimaryKey();
      if (primaryKey == null) {
        throw new RuntimeException("The foreign table (" + foreignTable + ") has no primary key");
      }
      tableDef
        .foreignKeyOf(primaryKey, columns)
        .setName(fk_name);
    }

  }

  /**
   * This function must be called after the function {@link #buildPrimaryKey(RelationDef)}
   * because the getIndex function of JDBC returns also the unique index of the primary
   * key. We need then the primary key information in order to exclude it from the building
   *
   * @param metaDataDef
   */
  private static void buildUniqueKey(RelationDef metaDataDef) {

    // Collect all data first because we need all columns that make a unique key before
    // building the object
    Map<String, Map<Integer, String>> indexData = new HashMap<>();
    final String ordinal_position_alias = "ORDINAL_POSITION";
    final String column_name_alias = "COLUMN_NAME";
    final JdbcDataPath dataPath = (JdbcDataPath) metaDataDef.getDataPath();
    final String schema = dataPath.getSchema() != null ? dataPath.getSchema().getName() : null;
    try (
      // Oracle need to have the approximate argument to true, otherwise we of a ORA-01031: insufficient privileges
      ResultSet indexResultSet = dataPath.getDataStore().getCurrentConnection().getMetaData().getIndexInfo(dataPath.getCatalog(), schema, dataPath.getName(), true, true);
    ) {
      while (indexResultSet.next()) {

        String index_name = indexResultSet.getString("INDEX_NAME");

        // With SQL Server we may of a line with only null values
        if (index_name == null) {
          continue;
        }

        Map<Integer, String> indexProperties = indexData.get(index_name);
        if (indexProperties == null) {
          indexProperties = new HashMap<>();
          indexData.put(index_name, indexProperties);
        }
        indexProperties.put(indexResultSet.getInt(ordinal_position_alias), indexResultSet.getString(column_name_alias));

      }

    } catch (SQLException e) {
      String s = "Error when building the unique key (ie indexinfo) of the table (" + dataPath + ")";
      LOGGER.severe(s);
      System.err.println(s);
      throw new RuntimeException(e);
    }

    // Process the data
    for (String indexName : indexData.keySet()) {
      Map<Integer, String> indexProperties = indexData.get(indexName);

      // Sort the column by order
      List<Integer> positions = new ArrayList<>(indexProperties.keySet());
      List<ColumnDef> columnDefs = new ArrayList<>();
      Collections.sort(positions);
      for (Integer pos : positions) {
        ColumnDef columnDef = metaDataDef.getColumnDef(indexProperties.get(pos));
        columnDefs.add(columnDef);
      }

      // We don't want the unique index of the primary key
      PrimaryKeyDef primaryKeyDef = metaDataDef.getPrimaryKey();
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
      metaDataDef.addUniqueKey(indexName, columnNames);

    }


  }


  public void printPrimaryKey(JdbcDataPath jdbcDataPath) {

    try (
      ResultSet resultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getPrimaryKeys(jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), jdbcDataPath.getName())
    ) {
      while (resultSet.next()) {
        System.out.println("Primary Key Column: " + resultSet.getString("COLUMN_NAME"));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  public void printUniqueKey(JdbcDataPath jdbcDataPath) {

    try (
      ResultSet resultSet = jdbcDataPath.getDataStore().getCurrentConnection().getMetaData().getIndexInfo(jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), jdbcDataPath.getName(), true, false)
    ) {
      while (resultSet.next()) {
        System.out.println("Unique Key Column: " + resultSet.getString("COLUMN_NAME"));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Todo: Add {@link DatabaseMetaData#getClientInfoProperties()}
   */
  public static void printDatabaseInformation(AnsiDataStore jdbcDataStore) {

    System.out.println("Information about the database (" + jdbcDataStore.getName() + "):");

    System.out.println();
    System.out.println("Driver Information:");
    DatabaseMetaData databaseMetadata = null;
    final Connection currentConnection = jdbcDataStore.getCurrentConnection();
    try {

      databaseMetadata = currentConnection.getMetaData();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    try {
      System.out.println("getDatabaseProductVersion: " + databaseMetadata.getDatabaseProductVersion());

      System.out.println("getDatabaseProductName: " + databaseMetadata.getDatabaseProductName());
      System.out.println("getDatabaseMajorVersion: " + databaseMetadata.getDatabaseMajorVersion());
      System.out.println("getDatabaseMinorVersion: " + databaseMetadata.getDatabaseMinorVersion());
      System.out.println("getMaxConnections: " + databaseMetadata.getMaxConnections());
      System.out.println("getJDBCMajorVersion: " + databaseMetadata.getJDBCMajorVersion());
      System.out.println("getJDBCMinorVersion: " + databaseMetadata.getJDBCMinorVersion());
      System.out.println("getURL: " + databaseMetadata.getURL());
      System.out.println("Driver Version: " + databaseMetadata.getDriverVersion());
      System.out.println("Driver Name: " + databaseMetadata.getDriverName());
      System.out.println("getUserName: " + databaseMetadata.getUserName());
      System.out.println("supportsNamedParameters: " + databaseMetadata.supportsNamedParameters());
      System.out.println("supportsBatchUpdates: " + databaseMetadata.supportsBatchUpdates());
      System.out.println();
      System.out.println("Connection");
      System.out.println("Catalog: " + currentConnection.getCatalog());
      String schema;
      if (databaseMetadata.getJDBCMajorVersion() >= 7) {
        schema = currentConnection.getSchema();
      } else {
        schema = "The JDBC Driver doesn't have this information.";
      }
      System.out.println("Schema: " + schema);
      System.out.println("Schema Current Connection: " + currentConnection.getSchema());
      System.out.println("Client Info");
      Properties clientInfos = currentConnection.getClientInfo();
      if (clientInfos != null && clientInfos.size() != 0) {
        for (String key : clientInfos.stringPropertyNames()) {
          System.out.println("  * (" + key + ") = (" + clientInfos.getProperty(key) + ")");
        }
      } else {
        System.out.println("   * No client infos");
      }

      System.out.println();
      URI url;
      try {
        url = new URI(jdbcDataStore.getConnectionString());
        URIExtended uriExtended = new URIExtended(url);
        System.out.println("URL (" + url + ")");
        System.out.println("Authority: " + url.getAuthority());
        System.out.println("Scheme: " + url.getScheme());
        System.out.println("Scheme Specific Part: " + url.getSchemeSpecificPart());
        System.out.println("Fragment: " + url.getFragment());
        System.out.println("Host: " + url.getHost());
        System.out.println("Path: " + url.getPath());
        System.out.println("Query: " + url.getQuery());
        System.out.println("Raw Query: " + url.getRawQuery());
        System.out.println("Raw Authority: " + url.getRawAuthority());
        System.out.println("Raw Fragment: " + url.getRawFragment());
        System.out.println("Raw Path: " + url.getRawPath());
        System.out.println("Raw Schema Specific Part: " + url.getRawSchemeSpecificPart());
        System.out.println("Driver: " + uriExtended.getDriver());
        System.out.println("Server: " + uriExtended.getServer());
      } catch (URISyntaxException e) {
        System.out.println("Error while reading the URI information. Message:" + e.getMessage());
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Print data type given by the driver
   */
  public static void printDataTypeInformation(AnsiDataStore jdbcDataStore) {

    Set<SqlDataType> sqlDataTypes = jdbcDataStore.getSqlDataTypes();

    // Headers
    System.out.println("Data Type\t" +
      "Type Name\t" +
      "Precision\t" +
      "literalPrefix\t" +
      "literalSuffix\t" +
      "createParams\t" +
      "nullable\t" +
      "caseSensitive\t" +
      "searchable\t" +
      "unsignedAttribute\t" +
      "fixedPrecScale\t" +
      "localTypeName\t" +
      "minimumScale\t" +
      "maximumScale"
    );

    for (SqlDataType typeInfo : sqlDataTypes) {
      System.out.println(
        typeInfo.getTypeCode() + "\t" +
          typeInfo.getTypeNames() + "\t" +
          typeInfo.getMaxPrecision() + "\t" +
          typeInfo.getLiteralPrefix() + "\t" +
          typeInfo.getLiteralSuffix() + "\t" +
          typeInfo.getCreateParams() + "\t" +
          typeInfo.getNullable() + "+\t" +
          typeInfo.getCaseSensitive() + "\t" +
          typeInfo.getSearchable() + "\t" +
          typeInfo.getUnsignedAttribute() + "\t" +
          typeInfo.getFixedPrecScale() + "\t" +
          typeInfo.getLocalTypeName() + "\t" +
          typeInfo.getMinimumScale() + "\t" +
          typeInfo.getMaximumScale()
      );

    }


  }


  /**
   * Return an object to be set in a prepared statement (for instance)
   * Example: if you want to load a double in an Oracle BINARY_DOUBLE, you need to cast it first as a
   * oracle.sql.BINARY_DOUBLE
   *
   * @param targetConnection the target connection
   * @param targetColumnType the target column type
   * @param sourceObject     the java object to be loaded
   * @return
   */
  public static Object castLoadObjectIfNecessary(Connection targetConnection, int targetColumnType, Object sourceObject) {

    String databaseProductName;
    try {
      databaseProductName = targetConnection.getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    // See oracle
    return sourceObject;

  }

  public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {
    /**
     * TODO: move that outside of the core
     * for now a hack
     * because Sqlite does not support alter table drop foreign keys
     */
    AnsiDataStore dataStore = (AnsiDataStore) foreignKeyDef.getTableDef().getDataPath().getDataStore();
    if (!dataStore.getProductName().equals(DB_SQLITE)) {
      JdbcDataPath jdbcDataPath = (JdbcDataPath) foreignKeyDef.getTableDef().getDataPath();
      String dropStatement = "alter table " + JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath) + " drop constraint " + foreignKeyDef.getName();
      try {

        Statement statement = dataStore.getCurrentConnection().createStatement();
        statement.execute(dropStatement);
        statement.close();

        JdbcDataSystemLog.LOGGER_DB_JDBC.info("Foreign Key (" + foreignKeyDef.getName() + ") deleted from the table (" + jdbcDataPath.toString() + ")");

      } catch (SQLException e) {

        System.err.println(dropStatement);
        throw new RuntimeException(e);

      }
    }
  }


}
