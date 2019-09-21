package net.bytle.db.model;

import net.bytle.db.DbLoggers;

import java.util.*;

/**
 *
 */
public class ForeignKeyDef {

    private final TableDef tableDef;
    private Map<Integer, ColumnDef> foreignKeyColumnDefs = new HashMap<>();

    private PrimaryKeyDef foreignPrimaryKey;

    // May be null via JBDC
    // See description
    // https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getImportedKeys(java.lang.String,%20java.lang.String,%20java.lang.String)
    private String name;

    public ForeignKeyDef(TableDef tableDef) {

        this.tableDef = tableDef;

    }

    /**
     * The name may be null
     * See
     * https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getImportedKeys(java.lang.String,%20java.lang.String,%20java.lang.String)
     *
     * @return the name of the fk
     */
    public String getName() {
        return name;
    }

    /**
     * Add a foreign key column (ie a column of the table) that references a (foreign) primary key
     * The sequence is the next one in the list
     * @param columnDef
     * @return the foreignKey
     */
    public ForeignKeyDef addColumn(ColumnDef columnDef) {

        this.foreignKeyColumnDefs.put(foreignKeyColumnDefs.size(),columnDef);
        return this;

    }

    /**
     * Add a foreign key column (ie a column of the table) that references a (foreign) primary key
     * @param columnDef - the column
     * @param colSeq - the sequence (given by the JDBC database metadata)
     * @return the foreignKey
     */
    public ForeignKeyDef addColumn(ColumnDef columnDef, int colSeq) {

        this.foreignKeyColumnDefs.put(colSeq, columnDef);
        return this;

    }

    public ForeignKeyDef setForeignPrimaryKey(PrimaryKeyDef foreignPrimaryKey) {
        this.foreignPrimaryKey = foreignPrimaryKey;
        return this;
    }

    public TableDef getTableDef() {
        return tableDef;
    }

    public List<ColumnDef> getChildColumns() {
        List<Integer> keys = new ArrayList(foreignKeyColumnDefs.keySet());
        Collections.sort(keys);
        List<ColumnDef> columnDefs = new ArrayList<>();
        for (Integer key:keys){
            columnDefs.add(foreignKeyColumnDefs.get(key));
        }
        return columnDefs;
    }

    public PrimaryKeyDef getForeignPrimaryKey() {
        if (foreignPrimaryKey==null){
            DbLoggers.LOGGER_DB_ENGINE.warning("The foreign primary key is null for the foreign key ("+this+")");
        }
        return foreignPrimaryKey;
    }

    public ForeignKeyDef setName(String name) {
        this.name = name;
        return this;
    }


    public ForeignKeyDef addColumns(List<ColumnDef> columnDefs) {
        for (ColumnDef columnDef:columnDefs){
            addColumn(columnDef);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKeyDef that = (ForeignKeyDef) o;
        return Objects.equals(tableDef, that.tableDef) &&
                Objects.equals(foreignPrimaryKey.getTableDef(), that.foreignPrimaryKey.getTableDef());
    }

    @Override
    public int hashCode() {

        return Objects.hash(tableDef, foreignPrimaryKey.getTableDef());

    }

    /**
     * Alias function to add a column by name
     *
     * @param columnName
     * @return
     */
    public ForeignKeyDef addColumn(String columnName) {
        return addColumn(this.tableDef.getColumnDef(columnName));
    }

    /**
     * Alias function to set the foreign key via table name
     * The table must be in the same schema
     *
     * @param tableName
     * @return the foreignKey for chaining init
     */
    public ForeignKeyDef setForeignPrimaryKey(String tableName) {
        return setForeignPrimaryKey(this.tableDef.getSchema().getTableOf(tableName).getPrimaryKey());
    }

    @Override
    public String toString() {
        return "Fk to " + foreignPrimaryKey.getTableDef().getName();
    }
}
