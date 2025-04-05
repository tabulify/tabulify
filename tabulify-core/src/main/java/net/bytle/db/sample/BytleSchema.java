package net.bytle.db.sample;

import net.bytle.db.connection.Connection;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;

import java.sql.Types;
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

  private final Connection datastore;

  private Map<String, DataPath> bytleTables = new HashMap<>();


  public BytleSchema(Connection connection) {
    this.datastore = connection;
    buildTables();
  }

  public static BytleSchema createFromDataStore(Connection connection) {
    return new BytleSchema(connection);
  }


  void buildTables() {

    // Dim Cat Table
    final DataPath catTable = datastore.getDataPath(TABLE_CATEGORY_NAME);
    bytleTables.put(TABLE_CATEGORY_NAME, catTable);
    catTable.createRelationDef()
      .addColumn(COLUMN_CATEGORY_ID, Types.INTEGER)
      .addColumn(COLUMN_CATEGORY_DESC_NAME, Types.VARCHAR,30)
      .addColumn(COLUMN_CATEGORY_LOAD_TIMESTAMP, Types.TIMESTAMP)
      .setPrimaryKey(COLUMN_CATEGORY_ID)
      .addUniqueKey(COLUMN_CATEGORY_DESC_NAME);


    // Dim timeTable
    final DataPath timeTable = datastore.getDataPath(TABLE_DATE_NAME);
    bytleTables.put(TABLE_DATE_NAME, timeTable);
    timeTable.createRelationDef()
      .addColumn(COLUMN_DATE_ID, Types.DATE)
      .addColumn(COLUMN_DATE_NAME, Types.CHAR,10)
      .addColumn(COLUMN_MONTH_ID, Types.VARCHAR, 6)
      .addColumn(COLUMN_MONTH_NUMBER, Types.INTEGER)
      .addColumn(COLUMN_MONTH_NAME, Types.VARCHAR, 20)
      .addColumn(COLUMN_MONTH_DESC, Types.VARCHAR, 20)
      .addColumn(COLUMN_MONTH_DESC_SHORT, Types.VARCHAR, 10)
      .addColumn(COLUMN_YEAR_NUMBER, Types.VARCHAR, 4)
      .addUniqueKey(COLUMN_DATE_NAME)
      .setPrimaryKey(COLUMN_DATE_ID);


    // Fact Table
    final DataPath factTable = datastore.getDataPath(TABLE_FACT_NAME);
    bytleTables.put(TABLE_FACT_NAME, factTable);
    factTable.createRelationDef()
      .addColumn(COLUMN_FACT_ID, Types.INTEGER)
      .addColumn(COLUMN_DATE_ID, Types.DATE)
      .addColumn(COLUMN_CATEGORY_ID, Types.INTEGER)
      .addColumn(COLUMN_SALES_QTY, Types.REAL)
      .addColumn(COLUMN_SALES_PRICE, Types.NUMERIC, 20, 2)
      .setPrimaryKey(COLUMN_FACT_ID)
      .addForeignKey(timeTable.getOrCreateRelationDef().getPrimaryKey(), COLUMN_DATE_ID)
      .addForeignKey(catTable.getOrCreateRelationDef().getPrimaryKey(), COLUMN_CATEGORY_ID);


  }


  @Override
  public List<DataPath> getAndCreateDataPaths() {
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
  public void dropAllAndCreateDataPaths() {
    dropAll();
    getAndCreateDataPaths();
  }

  public List<DataPath> getDataPaths() {
    return new ArrayList<>(bytleTables.values());
  }

  public DataPath getCategoryTable(){
    return getDataPath(TABLE_CATEGORY_NAME);
  }

  public DataPath getFactTable(){
    return getDataPath(TABLE_FACT_NAME);
  }

  /**
   * Just a utility function to drop all
   */
  public void dropAll() {
    Tabulars.dropIfExists(Tabulars.getChildren(datastore.getCurrentDataPath()));
  }

  public DataPath getDateTable() {
    return getDataPath(TABLE_DATE_NAME);
  }
}
