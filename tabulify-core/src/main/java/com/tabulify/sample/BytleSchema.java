package com.tabulify.sample;

import com.tabulify.connection.Connection;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The test model of Bytle
 */
public class BytleSchema implements SchemaSample {


  /**
   * The date table and columns name (on day level)
   */
  static public final String TABLE_DATE_NAME = "d_date";
  static public final String COLUMN_DATE_ID = "date_id";
  static public final String COLUMN_DATE_NAME = "date_name";
  static public final String COLUMN_MONTH_ID = "month_id";
  static public final String COLUMN_MONTH_NAME = "month_name";
  static public final String COLUMN_MONTH_DESC = "month_desc";
  static public final String COLUMN_MONTH_DESC_SHORT = "month_desc_short";
  static public final String COLUMN_MONTH_NUMBER = "month_number";
  static public final String COLUMN_YEAR_NUMBER = "year_number";

  /**
   * A category table
   */
  static public final String TABLE_CATEGORY_NAME = "d_category";
  static public final String COLUMN_CATEGORY_DESC_NAME = "cat_desc";
  static public final String COLUMN_CATEGORY_ID = "cat_id";
  static public final String COLUMN_CATEGORY_LOAD_TIMESTAMP = "load_timestamp";
  /**
   * The fact table and columns name
   */
  static public final String TABLE_FACT_NAME = "f_sales";
  static public final String COLUMN_FACT_ID = "id";
  static public final String COLUMN_SALES_QTY = "sales_qty";
  static public final String COLUMN_SALES_PRICE = "sales_price";

  static final List<String> tables = Arrays.asList(
    TABLE_FACT_NAME
    , TABLE_CATEGORY_NAME
    , TABLE_DATE_NAME
  );

  private final Connection connection;

  private Map<String, DataPath> bytleTables = new HashMap<>();


  public BytleSchema(Connection connection) {
    this.connection = connection;
    buildTables();
  }

  public static BytleSchema create(Connection connection) {
    return new BytleSchema(connection);
  }


  void buildTables() {

    // Dim Cat Table
    final DataPath catTable = connection.getDataPath(TABLE_CATEGORY_NAME);
    bytleTables.put(TABLE_CATEGORY_NAME, catTable);
    catTable.createEmptyRelationDef()
      .addColumn(COLUMN_CATEGORY_ID, SqlDataTypeAnsi.INTEGER)
      .addColumn(COLUMN_CATEGORY_DESC_NAME, SqlDataTypeAnsi.CHARACTER_VARYING, 30)
      .addColumn(COLUMN_CATEGORY_LOAD_TIMESTAMP, SqlDataTypeAnsi.TIMESTAMP)
      .setPrimaryKey(COLUMN_CATEGORY_ID)
      .addUniqueKey(COLUMN_CATEGORY_DESC_NAME);


    // Dim timeTable
    final DataPath timeTable = connection.getDataPath(TABLE_DATE_NAME);
    bytleTables.put(TABLE_DATE_NAME, timeTable);
    timeTable.createEmptyRelationDef()
      .addColumn(COLUMN_DATE_ID, SqlDataTypeAnsi.DATE)
      .addColumn(COLUMN_DATE_NAME, SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn(COLUMN_MONTH_ID, SqlDataTypeAnsi.CHARACTER_VARYING, 6)
      .addColumn(COLUMN_MONTH_NUMBER, SqlDataTypeAnsi.INTEGER)
      .addColumn(COLUMN_MONTH_NAME, SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn(COLUMN_MONTH_DESC, SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn(COLUMN_MONTH_DESC_SHORT, SqlDataTypeAnsi.CHARACTER_VARYING, 10)
      .addColumn(COLUMN_YEAR_NUMBER, SqlDataTypeAnsi.CHARACTER_VARYING, 4)
      .addUniqueKey(COLUMN_DATE_NAME)
      .setPrimaryKey(COLUMN_DATE_ID);


    // Fact Table
    final DataPath factTable = connection.getDataPath(TABLE_FACT_NAME);
    bytleTables.put(TABLE_FACT_NAME, factTable);
    PrimaryKeyDef timeTablePrimaryKey = timeTable.getOrCreateRelationDef().getPrimaryKey();
    factTable.createEmptyRelationDef()
      .addColumn(COLUMN_FACT_ID, SqlDataTypeAnsi.INTEGER)
      .addColumn(COLUMN_DATE_ID, SqlDataTypeAnsi.DATE)
      .addColumn(COLUMN_CATEGORY_ID, SqlDataTypeAnsi.INTEGER)
      .addColumn(COLUMN_SALES_QTY, SqlDataTypeAnsi.REAL)
      .addColumn(COLUMN_SALES_PRICE, SqlDataTypeAnsi.NUMERIC, 20, 2)
      .setPrimaryKey(COLUMN_FACT_ID)
      .addForeignKey(timeTablePrimaryKey, COLUMN_DATE_ID)
      .addForeignKey(catTable.getOrCreateRelationDef().getPrimaryKey(), COLUMN_CATEGORY_ID);


  }


  @Override
  public List<DataPath> createDataPaths() {
    List<DataPath> dataPaths = getDataPaths();
    Tabulars.createIfNotExist(dataPaths);
    return dataPaths;
  }

  @Override
  public DataPath getAndCreateDataPath(String tableName) {
    DataPath dataPath = getDataPath(tableName);
    Tabulars.create(dataPath);
    return dataPath;
  }

  @Override
  public List<DataPath> getDataPaths(String... tableNames) {
    return Arrays.stream(tableNames).map(name -> bytleTables.get(name)).collect(Collectors.toList());
  }


  public DataPath getDataPath(String name) {
    return bytleTables.get(name);
  }

  /**
   * Drop all tables and recreate the schema
   */
  public BytleSchema dropAllAndCreateDataPaths() {
    dropAll();
    createDataPaths();
    return this;
  }

  public List<DataPath> getDataPaths() {
    return new ArrayList<>(bytleTables.values());
  }

  public DataPath getCategoryTable() {
    return getDataPath(TABLE_CATEGORY_NAME);
  }

  public DataPath getFactTable() {
    return getDataPath(TABLE_FACT_NAME);
  }

  /**
   * Just a utility function to drop all
   */
  public BytleSchema dropAll() {

    // Delete all
    Tabulars.dropIfExists(Tabulars.getChildren(connection.getCurrentDataPath()));

    // Otherwise the Bytle schema may be not dropped from the cache
    // if they were not in the database
    Tabulars.dropIfExists(this.getDataPaths());

    return this;

  }

  public DataPath getDateTable() {
    return getDataPath(TABLE_DATE_NAME);
  }


}
