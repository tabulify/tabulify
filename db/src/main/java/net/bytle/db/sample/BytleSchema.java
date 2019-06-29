package net.bytle.db.sample;

import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The test model of Bytle
 */
public class BytleSchema implements SchemaSample {

    /**
     * Name of the sample
     */
    public static final String SCHEMA_NAME = "BytleSchema";

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

    /**
     * The schema where the table will be stored
     */
    private final SchemaDef schemaDef;
    private Map<String, TableDef> bytleTables = new HashMap<>();

    /**
     * @param currentSchema
     */
    BytleSchema(SchemaDef currentSchema) {
        this.schemaDef = currentSchema;
        buildTables();
    }


    /**
     * @param database
     * @return @return a Bytle Sample Schema object with the current database schema
     */
    public static BytleSchema get(Database database) {
        return new BytleSchema(database.getCurrentSchema());
    }

    /**
     * @param schemaDef
     * @return @return a Bytle Sample Schema object in the schema def
     */
    public static BytleSchema get(SchemaDef schemaDef) {
        return new BytleSchema(schemaDef);
    }

    public static BytleSchema get() {
        return get(Databases.get().getSchema(SCHEMA_NAME));
    }

    void buildTables() {

        // Dim Cat Table
        final TableDef catTable = Tables.get(TABLE_CATEGORY_NAME)
                .addColumn(COLUMN_CATEGORY_ID, Types.INTEGER)
                .addColumn(COLUMN_CATEGORY_DESC_NAME, Types.VARCHAR)
                .addColumn(COLUMN_CATEGORY_LOAD_DATE, Types.DATE)
                .setPrimaryKey(COLUMN_CATEGORY_ID)
                .addUniqueKey(COLUMN_CATEGORY_DESC_NAME)
                .setSchema(schemaDef);
        bytleTables.put(TABLE_CATEGORY_NAME, catTable);


        // Dim timeTable
        final TableDef timeTable = Tables.get(TABLE_TIME_NAME)
                .addColumn(COLUMN_DATE_ID, Types.DATE)
                .addColumn(COLUMN_MONTH_ID, Types.VARCHAR, 6)
                .addColumn(COLUMN_MONTH_NUMBER, Types.INTEGER)
                .addColumn(COLUMN_MONTH_NAME, Types.VARCHAR, 20)
                .addColumn(COLUMN_MONTH_DESC, Types.VARCHAR, 20)
                .addColumn(COLUMN_MONTH_DESC_SHORT, Types.VARCHAR, 10)
                .addColumn(COLUMN_YEAR_NUMBER, Types.VARCHAR, 4)
                .setPrimaryKey(COLUMN_DATE_ID)
                .setSchema(schemaDef);
        bytleTables.put(TABLE_TIME_NAME, timeTable);


        // Fact Table
        bytleTables.put(TABLE_FACT_NAME, Tables.getTable(TABLE_FACT_NAME)
                .addColumn(COLUMN_FACT_ID, Types.INTEGER)
                .addColumn(COLUMN_DATE_ID, Types.DATE)
                .addColumn(COLUMN_CATEGORY_ID, Types.INTEGER)
                .addColumn(COLUMN_SALES_QTY, Types.DOUBLE)
                .addColumn(COLUMN_SALES_PRICE, Types.DOUBLE)
                .setPrimaryKey(COLUMN_FACT_ID)
                .addForeignKey(timeTable.getPrimaryKey(), COLUMN_DATE_ID)
                .addForeignKey(catTable.getPrimaryKey(), COLUMN_CATEGORY_ID)
                .setSchema(schemaDef)
        );


    }


    @Override
    public List<TableDef> getTables() {
        return new ArrayList<>(bytleTables.values());
    }

    @Override
    public TableDef getTable(String tableName) {
        return bytleTables.get(tableName);
    }

    @Override
    public List<TableDef> getTables(String... tableNames) {
        return Arrays.stream(tableNames).map(name -> bytleTables.get(name)).collect(Collectors.toList());
    }

}
