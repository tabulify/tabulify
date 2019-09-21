package net.bytle.db.model;

import net.bytle.db.DbLoggers;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * A foreign key is a constraint definition
 * but it's basically the definition of relation between two tables
 *   * from a column(s)
 *   * and the columns of the (foreign) primary key
 * This object should then not be created with the pair of columns that creates this relations ie
 *   * the intern columns
 *   * the (foreign) primary key
 */
public class ForeignKeyDef {

    // The list of column
    // Order is important
    private final List<ColumnDef> columnDefs;
    // The foreign primary key
    private final PrimaryKeyDef foreignPrimaryKey;

    // May be null via JBDC
    // See description
    // https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getImportedKeys(java.lang.String,%20java.lang.String,%20java.lang.String)
    private String name;

    private ForeignKeyDef(PrimaryKeyDef primaryKeyDef, List<ColumnDef> columnDefs) {

        // null check
        if (primaryKeyDef==null){
            final String msg = "The foreign primary key can not be null when creating a foreign key";
            DbLoggers.LOGGER_DB_ENGINE.severe(msg);
            throw new RuntimeException(msg);
        }
        if (columnDefs==null){
            throw new RuntimeException("columnDefs should not be null for the primary key ("+primaryKeyDef+")");
        }
        // Size check
        if (primaryKeyDef.getColumns().size()!=columnDefs.size()){
            final String msg = "The foreign primary key ("+primaryKeyDef+") has (" + primaryKeyDef.getColumns().size() + ") columns and we got only ("+columnDefs.size()+") columns for the definition ("+columnDefs+").";
            DbLoggers.LOGGER_DB_ENGINE.severe(msg);
            throw new RuntimeException(msg);
        }
        // Columns Table check
        List<RelationDef> relationDefs = columnDefs.stream().map(ColumnDef::getRelationDef).distinct().collect(Collectors.toList());
        if (relationDefs.size()!=1){
            final String msg = "The columns ("+columnDefs+") has no table or different tables ("+relationDefs+") and this is not possible for a foreign key definition.";
            DbLoggers.LOGGER_DB_ENGINE.severe(msg);
            throw new RuntimeException(msg);
        }
        // Finally
        this.foreignPrimaryKey = primaryKeyDef;
        this.columnDefs = columnDefs;

    }

    public static ForeignKeyDef of(PrimaryKeyDef primaryKeyDef, ColumnDef... columnDefs) {
        return of(primaryKeyDef,Arrays.asList(columnDefs));
    }

    public static ForeignKeyDef of(PrimaryKeyDef primaryKeyDef, List<ColumnDef> columnDefs) {
        return new ForeignKeyDef(primaryKeyDef,columnDefs);
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

//    /**
//     * Add a foreign key column (ie a column of the table) that references a (foreign) primary key
//     * The sequence is the next one in the list
//     * @param columnDef
//     * @return the foreignKey
//     */
//    public ForeignKeyDef addColumn(ColumnDef columnDef) {
//
//        this.foreignKeyColumnDefs.put(foreignKeyColumnDefs.size(),columnDef);
//        return this;
//
//    }
//
//    /**
//     * Add a foreign key column (ie a column of the table) that references a (foreign) primary key
//     * @param columnDef - the column
//     * @param colSeq - the sequence (given by the JDBC database metadata)
//     * @return the foreignKey
//     */
//    public ForeignKeyDef addColumn(ColumnDef columnDef, int colSeq) {
//
//        this.foreignKeyColumnDefs.put(colSeq, columnDef);
//        return this;
//
//    }
//
//    public ForeignKeyDef setForeignPrimaryKey(PrimaryKeyDef foreignPrimaryKey) {
//        this.foreignPrimaryKey = foreignPrimaryKey;
//        return this;
//    }


    public List<ColumnDef> getChildColumns() {
        return columnDefs;
    }

    public PrimaryKeyDef getForeignPrimaryKey() {

        return foreignPrimaryKey;
    }

    public ForeignKeyDef setName(String name) {
        this.name = name;
        return this;
    }


//    public ForeignKeyDef addColumns(List<ColumnDef> columnDefs) {
//        for (ColumnDef columnDef:columnDefs){
//            addColumn(columnDef);
//        }
//        return this;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKeyDef that = (ForeignKeyDef) o;
        return Objects.equals(foreignPrimaryKey.getTableDef(), that.foreignPrimaryKey.getTableDef());
    }

    @Override
    public int hashCode() {

        return Objects.hash(foreignPrimaryKey.getTableDef());

    }

//    /**
//     * Alias function to add a column by name
//     *
//     * @param columnName
//     * @return
//     */
//    public ForeignKeyDef addColumn(String columnName) {
//        return addColumn(this.tableDef.getColumnDef(columnName));
//    }

//    /**
//     * Alias function to set the foreign key via table name
//     * The table must be in the same schema
//     *
//     * @param tableName
//     * @return the foreignKey for chaining init
//     */
//    public ForeignKeyDef setForeignPrimaryKey(String tableName) {
//        return setForeignPrimaryKey(this.tableDef.getSchema().getTableOf(tableName).getPrimaryKey());
//    }

    @Override
    public String toString() {
        final PrimaryKeyDef foreignPrimaryKey = this.foreignPrimaryKey;
        return "Fk to " + foreignPrimaryKey.getTableDef().getName();
    }

    public RelationDef getTableDef() {
        return columnDefs.get(1).getRelationDef();

    }
}
