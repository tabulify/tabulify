package net.bytle.db.model;

import net.bytle.cli.CliLog;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by gerard on 01-02-2016.
 * A class that contains a column data structure definition
 * <p>
 * It's generally construct form a resultSetMetadata object
 * but it can also be construct programmatically (when you want to load data after parsing a file for instance)
 */
public class ColumnDef implements Comparable<ColumnDef> {

    public static final int DEFAULT_PRECISION = 50;
    private static Set<Integer> allowedNullableValues = new HashSet<>();

    static {
        allowedNullableValues.add(DatabaseMetaData.columnNoNulls);
        allowedNullableValues.add(DatabaseMetaData.columnNullable);
        allowedNullableValues.add(DatabaseMetaData.columnNullableUnknown);
    }

    /* Mandatory */
    private final String columnName;

    private int nullable = DatabaseMetaData.columnNullable;
    private String isAutoincrement;
    private String isGeneratedColumn;
    private RelationDef relationDef;
    private int columnPosition;
    private String fullyQualifiedName;

    // Default type code is given in the getter function
    private DataType dataType;
    private Integer typeCode;
    private String typeName; // Db such as sqlite doesn't have the notion of typcode
    /* Precision = Length for string, Precision =  Precision for Fix Number */
    private Integer precision;
    /* Only needed for number */
    private Integer scale;

    //
    private Class clazz;


    public String getIsGeneratedColumn() {
        return isGeneratedColumn;
    }

    /**
     * Only called by the function get of a TableDef
     * To construct a column use TableDef.get
     *
     * @param relationDef
     */
    ColumnDef(RelationDef relationDef, String columnName) {

        this.relationDef = relationDef;
        this.columnName = columnName;

    }

    /**
     * @return one of
     * DatabaseMetaData.columnNullable,
     * DatabaseMetaData.columnNoNulls,
     * DatabaseMetaData.columnNullableUnknown
     */
    public int getNullable() {
        return nullable;
    }

    public String getIsAutoincrement() {
        if (isAutoincrement == null) {
            return "";
        } else {
            return isAutoincrement;
        }
    }

    // A datatype constructor
    private DataType getDataTypeOf(Integer typeCode, Class clazz) {

        DataType dataType;
        DataTypeJdbc dataTypeJdbc;

        if (typeCode == null && clazz == null) {

            // No data type defined, default to VARCHAR
            typeCode = Types.VARCHAR;

        } else {

            // If the developer gave only the java data type (class)
            if (typeCode != null) {
                typeCode = this.typeCode;
            } else {
                dataTypeJdbc = DataTypesJdbc.ofClass(clazz);
                typeCode = dataTypeJdbc.getTypeCode();
            }

        }

        // Trying to retrieve it from the cache
        dataType = this.getRelationDef().getDatabase().getDataType(typeCode);

        // If the data type is not known
        if (dataType == null) {

            throw new RuntimeException("No DataType could be found for the type code (" + CliLog.toStringNullSafe(typeCode) + " and the class (" + CliLog.toStringNullSafe(clazz) + ")");

        }
        return dataType;

    }


    public String getColumnName() {
        return columnName;
    }

    public Integer getPrecision() {
        if (precision == null) {
            return DEFAULT_PRECISION;
        } else {
            return precision;
        }
    }

    public Integer getScale() {
        if (scale != null) {
            return scale;
        } else {
            return null;
        }
    }

    public RelationDef getRelationDef() {
        return relationDef;
    }

    public DataType getDataType() {

        // The typecode of the clazz may be modified between two calls of datatype
        // It happens for instance with test
        return getDataTypeOf(typeCode, clazz);

    }

    public void setColumnPosition(int columnPosition) {
        this.columnPosition = columnPosition;
    }

    public Integer getColumnPosition() {
        return columnPosition;
    }

    public ColumnDef setNullable(int nullable) {

        if (!allowedNullableValues.contains(nullable)) {
            throw new RuntimeException("The value (" + nullable + ") is unknown");
        } else {
            this.nullable = nullable;
        }
        return this;

    }

    public String getFullyQualifiedName() {
        if (fullyQualifiedName == null) {
            fullyQualifiedName = relationDef.getFullyQualifiedName() + "." + columnName;
        }
        return fullyQualifiedName;
    }

    @Override
    public int compareTo(ColumnDef o) {
        return this.getColumnPosition().compareTo(o.getColumnPosition());
    }

    public ColumnDef typeCode(Integer typeCode) {
        if (typeCode != null) {
            this.typeCode = typeCode;
        }
        return this;
    }

    public ColumnDef precision(Integer precision) {
        if (precision != null) {
            this.precision = precision;
            if (this.scale != null) {
                if (this.scale > this.precision) {
                    throw new RuntimeException("Scale (" + this.scale + ") cannot be greater than precision (" + this.precision + ").");
                }
            }
        }
        return this;
    }

    /**
     * Same value than the JDBC metadata
     * YES
     * NO
     * '' Empty string: not known
     *
     * @param is_autoincrement
     * @return
     */
    public ColumnDef isAutoincrement(String is_autoincrement) {
        this.isAutoincrement = is_autoincrement;
        return this;
    }

    /**
     * What is this ? derived column ?
     *
     * @param is_generatedcolumn
     * @return
     */
    public ColumnDef isGeneratedColumn(String is_generatedcolumn) {
        this.isGeneratedColumn = is_generatedcolumn;
        return this;
    }

    public ColumnDef scale(Integer scale) {

        if (this.scale != null) {
            if (this.precision != null) {
                if (this.scale > this.precision) {
                    throw new RuntimeException("Scale (" + this.scale + ") cannot be greater than precision (" + this.precision + ").");
                }
            }
            this.scale = scale;
        }
        return this;
    }

    public ColumnDef clazz(Class clazz) {
        if (clazz != null) {
            this.clazz = clazz;
        }
        return this;
    }


    @Override
    public String toString() {
        return "ColumnDef{" +
                "columnName='" + columnName + '\'' +
                ", columnType=" + dataType +
                ", precision=" + precision +
                ", scale=" + scale +
                ", nullable=" + nullable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColumnDef columnDef = (ColumnDef) o;

        if (!relationDef.equals(columnDef.relationDef)) return false;
        return getFullyQualifiedName().equals(columnDef.getFullyQualifiedName());
    }

    @Override
    public int hashCode() {
        int result = relationDef.hashCode();
        result = 31 * result + getFullyQualifiedName().hashCode();
        return result;
    }

    /**
     * TODO: not yet implemented
     *
     * @return
     */
    public Object getDefault() {
        return "";
    }

    /**
     * TODO: not yet implemented
     *
     * @return
     */
    public String getDescription() {
        return "";
    }

    public ColumnDef typeName(String typeName) {
        if (typeName != null) {
            this.typeName = typeName;
        }
        return this;
    }
}
