package net.bytle.db.model;


import net.bytle.db.database.Database;
import net.bytle.db.engine.Queries;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * Created by gerard on 01-02-2016.
 * A class that contains a table data structure definition
 * <p>
 * A table can be:
 * * "TABLE",
 * * "VIEW"
 * <p>
 * A table definition may be created:
 * * manually
 * * or through the metadata of the driver
 * * or through the metadata of a result set
 */
public class TableDef extends RelationDefAbs implements ISqlRelation {


    private final RelationMeta meta;
    private PrimaryKeyDef primaryKeyDef;

    /**
     * The identity string is for now the name of the foreign key
     * TODO ? but it would be better to implement on the column names
     * because not all foreign keys have a name (for instance Sqlite)
     */
    private HashMap<String, ForeignKeyDef> foreignKeys = new HashMap<>();


    private Set<UniqueKeyDef> uniqueKeys = new HashSet<>();

    // Not used but we keep it because there is some doc
    // on the setter
    private String tableType;


    public TableDef(Database database, String tableName) {

        this.name = tableName;
        this.schema = database.getCurrentSchema();
        meta = new RelationMeta(this);

    }


    @Override
    public List<ColumnDef> getColumnDefs() {
        return meta.getColumnDefs();
    }

    @Override
    public ColumnDef getColumnDef(String columnName) {
        return meta.getColumnDef(columnName);
    }


    public TableDef addPrimaryKey(PrimaryKeyDef primaryKeyDef) {
        this.primaryKeyDef = primaryKeyDef;
        return this;
    }


    public PrimaryKeyDef getPrimaryKey() {
        if (primaryKeyDef == null) {
            primaryKeyDef = new PrimaryKeyDef(this);
            this.addPrimaryKey(primaryKeyDef);
        }
        return primaryKeyDef;
    }


    public List<ForeignKeyDef> getForeignKeys() {

        return new ArrayList<>(foreignKeys.values());

    }


    /**
     * Return the foreign Key for this primary key and this set of columns
     * If the key is already defined
     *
     * @param columnDefs    the foreign columns on the table
     * @param primaryKeyDef the foreign primary key
     * @return
     */
    private ForeignKeyDef getForeignKeyOf(PrimaryKeyDef primaryKeyDef, List<ColumnDef> columnDefs) {

        for (ForeignKeyDef foreignKeyDef : getForeignKeys()) {
            if (foreignKeyDef.getForeignPrimaryKey().equals(primaryKeyDef)) {
                if (foreignKeyDef.getChildColumns().equals(columnDefs)) {
                    return foreignKeyDef;
                }
            }
        }

        final String fkName = this.getName() + "_fk" + foreignKeys.size();
        ForeignKeyDef foreignKeyDef = new ForeignKeyDef(this)
                .setName(fkName)
                .setForeignPrimaryKey(primaryKeyDef)
                .addColumns(columnDefs);

        this.foreignKeys.put(fkName, foreignKeyDef);
        return foreignKeyDef;

    }


    /**
     * @return the foreign key that reference this table
     */
    public List<ForeignKeyDef> getExternalForeignKeys() {

        List<ForeignKeyDef> externalForeignKey = new ArrayList<>();
        for (TableDef tableDef : schema.getTables()) {
            if (tableDef.equals(this)) {
                continue;
            }
            for (ForeignKeyDef foreignKeyDef : tableDef.getForeignKeys()) {
                if (foreignKeyDef.getForeignPrimaryKey().getTableDef().equals(this)) {
                    externalForeignKey.add(foreignKeyDef);
                }
            }
        }
        return externalForeignKey;
    }


    /**
     * Shortcut function to create a primary key from a column name
     *
     * @param columnNames - the primary key column name(s)
     * @return tableDef for chaining init
     */
    public TableDef setPrimaryKey(String... columnNames) {
        this.getPrimaryKey().addColumn(getColumns(columnNames));
        return this;
    }

    public List<UniqueKeyDef> getUniqueKeys() {
        return new ArrayList(uniqueKeys);
    }

    public TableDef addForeignKey(ForeignKeyDef foreignKeyDef) {
        this.foreignKeys.put(foreignKeyDef.getName(), foreignKeyDef);
        return this;
    }


    /**
     * The JDBC table type
     * <p>
     * "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"
     * <p>
     * Constant are also available inside this class
     *
     * @param tableType
     * @return
     */
    public TableDef JdbcType(String tableType) {
        this.tableType = tableType;
        return this;
    }

    /**
     * Get and Create function
     *
     * @param columnDefs
     * @return
     */
    public UniqueKeyDef getOrCreateUniqueKey(ColumnDef... columnDefs) {

        UniqueKeyDef uniqueKeyDefToReturn = null;
        for (UniqueKeyDef uniqueKeyDef : uniqueKeys) {

            if (uniqueKeyDef.getColumns().equals(Arrays.asList(columnDefs))) {
                uniqueKeyDefToReturn = uniqueKeyDef;
            }

        }
        if (uniqueKeyDefToReturn == null) {
            uniqueKeyDefToReturn = new UniqueKeyDef(this)
                    .addColumns(Arrays.asList(columnDefs));
            uniqueKeys.add(uniqueKeyDefToReturn);
        }
        return uniqueKeyDefToReturn;


    }

    /**
     * Shortcut to add two columns as primary key
     *
     * @param columnName1 - The first column name of the primary key
     * @param columnName2 - The second column name of the primary key
     * @return - the table Def for a chaining initialization
     */
    public TableDef setPrimaryKey(String columnName1, String columnName2) {

        this.getPrimaryKey()
                .addColumn(meta.getColumnOf(columnName1))
                .addColumn(meta.getColumnOf(columnName2));

        return this;

    }

    public TableDef setPrimaryKey(String columnName1, String columnName2, String columnName3) {

        this.getPrimaryKey()
                .addColumn(this.getColumnDef(columnName1))
                .addColumn(this.getColumnDef(columnName2))
                .addColumn(this.getColumnDef(columnName3));

        return this;
    }

    /**
     * Add a unique key
     *
     * @param columnNames
     * @return the table def for chaining initialization
     */
    public TableDef addUniqueKey(String... columnNames) {
        getOrCreateUniqueKey(getColumns(columnNames));
        return this;
    }

    /**
     * @param columnNames
     * @return an array of columns
     * The columns must exist otherwise you get a exception
     */
    private ColumnDef[] getColumns(String... columnNames) {

        return meta.getColumns(columnNames);
    }

    /**
     * Add a foreign key
     *
     * @param primaryKeyDef
     * @param columnNames
     * @return the table for initialization chaining
     */
    public TableDef addForeignKey(PrimaryKeyDef primaryKeyDef, String... columnNames) {
        try {
            getForeignKeyOf(primaryKeyDef, Arrays.asList(getColumns(columnNames)));
        } catch (Exception e) {
            throw new RuntimeException("A problem occurs when trying to add a foreign to the table (" + this + ") towards the table (" + primaryKeyDef.getTableDef() + "). See the message below.", e);
        }
        return this;
    }

    /**
     * A short cut of @{link {@link #addForeignKey(ForeignKeyDef)}}
     * that add for you the foreign key
     *
     * @param table
     * @param columnNames
     * @return the tableDef for chaining initialization
     */
    public TableDef addForeignKey(TableDef table, String... columnNames) {
        return addForeignKey(table.getPrimaryKey(), columnNames);
    }


    public TableDef addPrimaryKey(List<String> primaryKeyColumns) {
        this.getPrimaryKey().addColumn(primaryKeyColumns);
        return this;
    }


    /**
     * @param columnIndex
     * @return a columnDef by index starting at 0
     */
    public ColumnDef getColumnDef(Integer columnIndex) {

        return getColumnDefs().get(columnIndex);
    }

    @Override
    public ColumnDef getColumnOf(String columnName) {
        return meta.getColumnOf(columnName);
    }


    /**
     * TODO: TableDef should only have metadata, move this
     * The generation of a SQL must not be inside
     *
     * @return
     */
    @Override
    public String getQuery() {
        /**
         * {@link DatabaseMetaData#getIdentifierQuoteString()}
         */
        return "select * from " + getFullyQualifiedName();
    }


    public TableDef setSchema(SchemaDef schemaDef) {
        this.schema = schemaDef;
        return this;
    }

    public TableDef addColumn(String columnName) {
        meta.addColumn(columnName);
        return this;
    }

    public TableDef addColumn(String columnName, int type) {
        meta.addColumn(columnName, type);
        return this;
    }

    public TableDef addColumn(String columnName, int type, int precision) {
        meta.addColumn(columnName, type, precision);
        return this;
    }

    public TableDef addColumn(String columnName, int type, boolean nullable) {
        meta.addColumn(columnName, type, nullable);
        return this;
    }

    public TableDef addColumn(String columnName, int type, int precision, int scale) {
        meta.addColumn(columnName, type, precision, scale);
        return this;
    }

    public TableDef addColumn(String columnName, int type, int precision, boolean nullable) {
        meta.addColumn(columnName, type, precision, nullable);
        return this;
    }

    @Override
    public ResultSet getResultSet() {

        return Queries.getResultSet(this);

    }


    public TableDef setDatabase(Database database) {
        this.schema = database.getCurrentSchema();
        return this;
    }

    public void deleteForeignKey(ForeignKeyDef foreignKeyDef) {

        foreignKeyDef = foreignKeys.remove(foreignKeyDef.getName());
        if (foreignKeyDef == null) {

            throw new RuntimeException("The foreign key (" + foreignKeyDef.getName() + ") does not belong to the table (" + this + ") and could not be removed");
        }

    }

    public List<TableDef> getForeignTables() {
        List<TableDef> tableDefs = new ArrayList<>();
        for (ForeignKeyDef foreignKeyDef:getForeignKeys()){
            tableDefs.add(foreignKeyDef.getForeignPrimaryKey().getTableDef());
        }
        return tableDefs;
    }
}
