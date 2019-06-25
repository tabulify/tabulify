package net.bytle.db.model;

import java.sql.DatabaseMetaData;
import java.util.*;

public class RelationMeta {

    private Map<String, ColumnDef> columnDefByName = new HashMap<>();

    private RelationDef relationDef;

    public RelationMeta(RelationDef relationDef) {
        this.relationDef = relationDef;
    }

    /**
     * Return the columns by position
     *
     * @return
     */
    List<ColumnDef> getColumnDefs() {

        List<ColumnDef> columnDefs = new ArrayList<>(columnDefByName.values());
        columnDefs.sort(
                (Comparator.comparing(ColumnDef::getColumnPosition))
        );
        return columnDefs;

    }


    /**
     * @param columnName
     * @return the column or null if not found
     */
    public ColumnDef getColumnDef(String columnName) {
        return columnDefByName.get(columnName);
    }

    /**
     * @param columnName
     * @return the actual column or a new created column object if not found
     */
    public ColumnDef getColumnOf(String columnName) {


        ColumnDef columnDef = getColumnDef(columnName);
        if (columnDef == null) {

            // This assert is to catch when object are passed
            // to string function, the length is bigger than the assertion and make it fails
            assert columnName.length() < 100;
            columnDef = new ColumnDef(relationDef, columnName);
            columnDef.setColumnPosition(columnDefByName.size() + 1);
            columnDefByName.put(columnName, columnDef);
        }
        return columnDef;

    }


    public RelationMeta addColumn(String columnName) {
        getColumnOf(columnName);
        return this;
    }

    public RelationMeta addColumn(String columnName, int typeCode) {

        getColumnOf(columnName)
                .typeCode(typeCode);

        return this;

    }

    public RelationMeta addColumn(String columnName, String typeName) {

        getColumnOf(columnName)
                .typeName(typeName);

        return this;

    }

    public RelationMeta addColumn(String columnName, int typeCode, int precision) {

        getColumnOf(columnName)
                .typeCode(typeCode)
                .precision(precision);

        return this;

    }

    public RelationMeta addColumn(String columnName, int typeCode, int precision, int scale) {

        getColumnOf(columnName)
                .typeCode(typeCode)
                .precision(precision)
                .scale(scale);

        return this;

    }

    public RelationMeta addColumn(String columnName, int typeCode, boolean nullable) {

        int columnNullable;
        if (nullable) {
            columnNullable = DatabaseMetaData.columnNullable;
        } else {
            columnNullable = DatabaseMetaData.columnNoNulls;
        }
        getColumnOf(columnName)
                .typeCode(typeCode)
                .setNullable(columnNullable);

        return this;

    }

    public RelationMeta addColumn(String columnName, int typeCode, int precision, boolean nullable) {

        int columnNullable;
        if (nullable) {
            columnNullable = DatabaseMetaData.columnNullable;
        } else {
            columnNullable = DatabaseMetaData.columnNoNulls;
        }
        getColumnOf(columnName)
                .typeCode(typeCode)
                .setNullable(columnNullable)
                .precision(precision);

        return this;

    }

    /**
     * @param columnNames
     * @return an array of columns
     * The columns must exist otherwise you get a exception
     */
    ColumnDef[] getColumns(String... columnNames) {
        List<ColumnDef> columnDefs = new ArrayList<>();
        for (String columnName : columnNames) {
            final ColumnDef column = getColumnDef(columnName);
            if (column != null) {
                columnDefs.add(column);
            } else {
                throw new RuntimeException("The column (" + columnName + ") was not found for the table (" + this + ")");
            }
        }
        return columnDefs.toArray(new ColumnDef[columnDefs.size()]);
    }

    public ColumnDef getColumnDef(Integer columnIndex) {
        return getColumnDefs().get(columnIndex);
    }

    @Override
    public String toString() {
        return relationDef.getFullyQualifiedName();
    }
}
