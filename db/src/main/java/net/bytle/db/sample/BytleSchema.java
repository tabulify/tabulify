package net.bytle.db.sample;

import net.bytle.db.Tabular;
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
     * The time table and columns name
     */
    static public final String TABLE_TIME_NAME = "D_TIME";
    static public final String COLUMN_DATE_ID = "DATE_ID";
    static public final String COLUMN_MONTH_ID = "MONTH_ID";
    static public final String COLUMN_MONTH_NAME = "MONTH_NAME";
    static public final String COLUMN_MONTH_DESC = "MONTH_DESC";
    static public final String COLUMN_MONTH_DESC_SHORT = "MONTH_DESC_SHORT";
    static public final String COLUMN_MONTH_NUMBER = "MONTH_NUMBER";
    static public final String COLUMN_YEAR_NUMBER = "YEAR_NUMBER";

    /**
     * A category table
     */
    static public final String TABLE_CATEGORY_NAME = "D_CATEGORY";
    static public final String COLUMN_CATEGORY_DESC_NAME = "CAT_DESC";
    static public final String COLUMN_CATEGORY_ID = "CAT_ID";
    static public final String COLUMN_CATEGORY_LOAD_DATE = "LOAD_DATE";
    /**
     * The fact table and columns name
     */
    static public final String TABLE_FACT_NAME = "F_SALES";
    static public final String COLUMN_FACT_ID = "ID";
    static public final String COLUMN_SALES_QTY = "SALES_QTY";
    static public final String COLUMN_SALES_PRICE = "SALES_PRICE";

    static final List<String> tables = Arrays.asList(
            TABLE_FACT_NAME
            , TABLE_CATEGORY_NAME
            , TABLE_TIME_NAME
    );

    private Map<String, DataPath> bytleTables = new HashMap<>();


    BytleSchema() {

        buildTables();

    }

    public static BytleSchema of() {
      return new BytleSchema();
    }


    void buildTables() {
        Tabular tabular = Tabular.tabular();
        // Dim Cat Table
        final DataPath catTable = tabular.getDataPath(TABLE_CATEGORY_NAME);
        bytleTables.put(TABLE_CATEGORY_NAME, catTable);
        catTable.getDataDef()
                .addColumn(COLUMN_CATEGORY_ID, Types.INTEGER)
                .addColumn(COLUMN_CATEGORY_DESC_NAME, Types.VARCHAR)
                .addColumn(COLUMN_CATEGORY_LOAD_DATE, Types.DATE)
                .setPrimaryKey(COLUMN_CATEGORY_ID)
                .addUniqueKey(COLUMN_CATEGORY_DESC_NAME);


        // Dim timeTable
        final DataPath timeTable = tabular.getDataPath(TABLE_TIME_NAME);
        bytleTables.put(TABLE_TIME_NAME, timeTable);
        timeTable.getDataDef()
                .addColumn(COLUMN_DATE_ID, Types.DATE)
                .addColumn(COLUMN_MONTH_ID, Types.VARCHAR, 6)
                .addColumn(COLUMN_MONTH_NUMBER, Types.INTEGER)
                .addColumn(COLUMN_MONTH_NAME, Types.VARCHAR, 20)
                .addColumn(COLUMN_MONTH_DESC, Types.VARCHAR, 20)
                .addColumn(COLUMN_MONTH_DESC_SHORT, Types.VARCHAR, 10)
                .addColumn(COLUMN_YEAR_NUMBER, Types.VARCHAR, 4)
                .setPrimaryKey(COLUMN_DATE_ID);


        // Fact Table
        final DataPath factTable = tabular.getDataPath(TABLE_FACT_NAME);
        bytleTables.put(TABLE_FACT_NAME, factTable);
        factTable.getDataDef()
                .addColumn(COLUMN_FACT_ID, Types.INTEGER)
                .addColumn(COLUMN_DATE_ID, Types.DATE)
                .addColumn(COLUMN_CATEGORY_ID, Types.INTEGER)
                .addColumn(COLUMN_SALES_QTY, Types.DOUBLE)
                .addColumn(COLUMN_SALES_PRICE, Types.DOUBLE, 50, 2)
                .setPrimaryKey(COLUMN_FACT_ID)
                .addForeignKey(timeTable.getDataDef().getPrimaryKey(), COLUMN_DATE_ID)
                .addForeignKey(catTable.getDataDef().getPrimaryKey(), COLUMN_CATEGORY_ID);


    }


    @Override
    public List<DataPath> getAndCreateDataPaths() {
      ArrayList<DataPath> dataPaths = new ArrayList<>(bytleTables.values());
      Tabulars.createIfNotExist(dataPaths);
      return dataPaths;
    }

    @Override
    public DataPath getAndCreateDataPath(String tableName) {
      DataPath dataPath = bytleTables.get(tableName);
      Tabulars.create(dataPath);
      return dataPath;
    }

    @Override
    public List<DataPath> getAndCreateDataPaths(String... tableNames) {
        return Arrays.stream(tableNames).map(name -> bytleTables.get(name)).collect(Collectors.toList());
    }


}
