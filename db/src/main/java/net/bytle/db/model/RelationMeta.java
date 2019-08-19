package net.bytle.db.model;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.*;

/**
 * Relation Meta is pseudo class used to regroup
 * all operations on columns
 *
 * This is to be able to create setter that return the object that have create them
 * in order to chain the setting
 */
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
        addColumn(columnName, null,null,null,null,null);
        return this;
    }

    public RelationMeta addColumn(String columnName, int typeCode) {

        addColumn(columnName, typeCode,null,null,null,null);
        return this;

    }


    public RelationMeta addColumn(String columnName, int typeCode, int precision) {

        addColumn(columnName, typeCode,precision,null,null,null);
        return this;

    }

    public RelationMeta addColumn(String columnName, int typeCode, int precision, int scale) {

        addColumn(columnName, typeCode,precision,scale,null,null);
        return this;

    }

    public RelationMeta addColumn(String columnName, int typeCode, boolean nullable) {

        addColumn(columnName, typeCode,null,null,nullable,null);
        return this;

    }

    public RelationMeta addColumn(String columnName, int typeCode, int precision, boolean nullable) {

        addColumn(columnName, typeCode,precision,null,nullable,null);
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

    public RelationMeta addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable, String comment) {
        int columnNullable;

        if (nullable == null) {
            columnNullable = DatabaseMetaData.columnNullableUnknown;
        } else if (!nullable) {
            columnNullable = DatabaseMetaData.columnNullable;
        } else {
            columnNullable = DatabaseMetaData.columnNoNulls;
        }

        getColumnOf(columnName)
                .typeCode(type)
                .precision(precision)
                .scale(scale)
                .setNullable(columnNullable)
                .comment(comment);
        return this;
    }
}
