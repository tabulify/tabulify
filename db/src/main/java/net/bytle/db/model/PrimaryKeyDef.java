package net.bytle.db.model;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

public class PrimaryKeyDef {

    private final TableDef tableDef;
    private String name;
    private List<ColumnDef> columnDefs = new ArrayList<>();

    public String getName() {
        return name;
    }

    /**
     * Use {@link #PrimaryKeyDef(TableDef)}
     */
    PrimaryKeyDef(){

        tableDef = null;
    }

    PrimaryKeyDef(TableDef tableDef) {
        this.tableDef = tableDef;
    }

    public TableDef getTableDef() {
        return tableDef;
    }

    public PrimaryKeyDef name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Add a column to the primary key
     * if the column is already present, this function will have no effect
     *
     * @param columnDefs
     */
    public PrimaryKeyDef addColumn(ColumnDef... columnDefs) {

        for (ColumnDef columnDef : columnDefs) {

            if (!this.columnDefs.contains(columnDef)) {

                this.columnDefs.add(columnDef);
                // The primary key generally cannot be null
                columnDef.setNullable(DatabaseMetaData.columnNoNulls);

            }

        }

        return this;

    }

    public List<ColumnDef> getColumns() {
        return this.columnDefs;
    }


    /**
     * An alias function that call @{link {@link #addColumn(ColumnDef...)}}
     *
     * @param columnNames
     * @return
     */
    public PrimaryKeyDef addColumn(List<String> columnNames) {

        for (String columnName : columnNames) {
            this.addColumn(tableDef.getColumnDef(columnName));
        }
        return this;

    }
}
