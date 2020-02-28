package net.bytle.db.jdbc;

import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStream;
import net.bytle.type.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.db.jdbc.AnsiDataPath.QUERY_TYPE;

public class AnsiDataDef extends TableDef implements RelationDef {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AnsiDataDef.class);

  private SqlSelectStream selectStream;


  /**
   * @param dataPath
   */
  public AnsiDataDef(AnsiDataPath dataPath, Boolean buildFromMeta) {
    super(dataPath);

    if (buildFromMeta) {
      // Do we have already a structure in the database
      if (existsAndSetTypeFromMetadata()) {

        // Columns building
        addColumnsFromMetadata();
        // Pk Building
        addPrimaryKeyFromMetaData();
        // Foreign Key building
        addForeignKeysFromMetadata();
        // Unique Key
        addUniqueKeysFromMetaData();

      } else {

        // Does this data path a query is
        if (dataPath.getType().equals(QUERY_TYPE)) {
          // The select stream build the data def
          selectStream = SqlSelectStream.of(dataPath);
          selectStream.runtimeDataDef(this);
          // sqlite for instance
          if (this.getDataPath().getDataStore().getMaxWriterConnection() == 1) {
            selectStream.close();
            selectStream = null;
          }
        }
      }
    }
  }


  /**
   * @return a select stream
   * <p>
   * The constructor {@link #AnsiDataDef(AnsiDataPath,Boolean)} may have initialized this select stream
   * when the data path is a query
   */
  public SelectStream getSelectStream() {

    if (selectStream == null) {
      selectStream = SqlSelectStream.of(this.getDataPath());
    }
    return selectStream;

  }

  @Override
  public AnsiDataPath getDataPath() {
    return (AnsiDataPath) super.dataPath;
  }

  /**
   * If the data path exists in the meta data store:
   * * Set the type of the data path {@link DataPath#getType()}
   * * and return true
   * or
   * * return false
   *
   * @return true if the table exists in the metadata or false
   * <p>
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   * We are not using the {@link SqlDataSystem#exists(DataPath)} function
   * because this function will return true for a query
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   */
  protected boolean existsAndSetTypeFromMetadata() {


    try {

      String[] types = {"TABLE"};

      final AnsiDataPath schemaPath = this.getDataPath().getSchema();
      String schema = null;
      if (schemaPath != null) {
        schema = schemaPath.getName();
      }
      String catalog = this.getDataPath().getCatalog();
      String tableName = this.getDataPath().getName();

      ResultSet tableResultSet = this.getDataPath().getDataStore().getCurrentConnection().getMetaData().getTables(catalog, schema, tableName, types);
      boolean exists = tableResultSet.next(); // For TYPE_FORWARD_ONLY
      if (exists) {
        this.getDataPath().setType(tableResultSet.getString("TABLE_TYPE"));
      }
      tableResultSet.close();

      return exists;

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }


  }

  /**
   * Add the columns from the data store metadata
   */
  protected void addColumnsFromMetadata() {

    try {
      String schemaName = null;
      if (this.getDataPath().getSchema() != null) {
        schemaName = this.getDataPath().getSchema().getName();
      }
      ResultSet columnResultSet = this.getDataPath().getDataStore().getCurrentConnection().getMetaData().getColumns(this.getDataPath().getCatalog(), schemaName, dataPath.getName(), null);
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
          LOGGER.trace("The IS_AUTOINCREMENT column seems not to be implemented. Message: " + e.getMessage());
        }

        int column_size = columnResultSet.getInt("COLUMN_SIZE");


        final int sqlTypeCode = columnResultSet.getInt("DATA_TYPE");

        SqlDataType dataType = this.getDataPath().getDataStore().getSqlDataType(sqlTypeCode);
        this.getColumnOf(column_name, dataType.getClazz())
          .typeCode(sqlTypeCode)
          .precision(column_size)
          .scale(columnResultSet.getInt("DECIMAL_DIGITS"))
          .isAutoincrement(is_autoincrement)
          .isGeneratedColumn(isGeneratedColumn)
          .setNullable(columnResultSet.getInt("NULLABLE"));

      }
      columnResultSet.close();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
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

    SqlDataStore dataStore = this.getDataPath().getDataStore();

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
    if (this.getDataPath().getSchema() != null) {
      schemaName = this.getDataPath().getSchema().getName();
    }
    try (
      // ImportedKey = the primary keys imported by a table
      ResultSet fkResultSet = dataStore.getCurrentConnection().getMetaData().getImportedKeys(this.getDataPath().getCatalog(), schemaName, dataPath.getName());
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
      LOGGER.error(s);
      System.err.println(s);
      if (dataStore.isStrict()) {
        throw new RuntimeException(e);
      } else {
        return;
      }
    }

    // How much foreign key (ie how much foreign key tables)
    List<AnsiDataPath> foreignTableNames = fkDatas.stream()
      .distinct()
      .map(s -> dataStore.getSqlDataPath(s.get(col_pktable_cat), s.get(col_pktable_schem), s.get(col_pktable_name)))
      .collect(Collectors.toList());


    for (AnsiDataPath foreignTable : foreignTableNames) {
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

      final PrimaryKeyDef primaryKey = foreignTable.getOrCreateDataDef().getPrimaryKey();
      if (primaryKey == null) {
        throw new RuntimeException("The foreign table (" + foreignTable + ") has no primary key");
      }
      this
        .foreignKeyOf(primaryKey, columns)
        .setName(fk_name);
    }

  }

  /**
   * Add the primary key that are in the data store metadata
   */
  protected void addPrimaryKeyFromMetaData() {

    try {
      // Bug in SQLite Driver - Hack
      // that doesn't return the good primary ley
      final AnsiDataPath dataPath = this.getDataPath();
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
    final AnsiDataPath dataPath = (AnsiDataPath) this.getDataPath();
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
      LOGGER.error(s);
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
      this.addUniqueKey(indexName, columnNames);

    }


  }


}
