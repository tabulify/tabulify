package net.bytle.db.model;

import net.bytle.cli.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.Database;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.type.Maps;
import net.bytle.type.Strings;


import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gerard on 01-02-2016.
 * A class that contains a column data structure definition
 * <p>
 * It's generally construct form a resultSetMetadata object
 * but it can also be construct programmatically (when you want to load data after parsing a file for instance)
 */
public class ColumnDef<T> implements Comparable<ColumnDef> {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    public static final int DEFAULT_PRECISION = 50;
    private static Set<Integer> allowedNullableValues = new HashSet<>();
    private final Class<T> clazz;

    private HashMap<String, Object> properties = new HashMap<>();

    static {
        allowedNullableValues.add(DatabaseMetaData.columnNoNulls);
        allowedNullableValues.add(DatabaseMetaData.columnNullable);
        allowedNullableValues.add(DatabaseMetaData.columnNullableUnknown);
    }

    /**
     * Mandatory
     * Called also an Identifier in SQL
     * See {@link DatabaseMetaData#getIdentifierQuoteString()}
     */
    private final String columnName;

    private int nullable = DatabaseMetaData.columnNullable;
    private String isAutoincrement;
    private String isGeneratedColumn;
    private RelationDef relationDef;
    private int columnPosition;
    private String fullyQualifiedName;

    // Default type code is given in the getter function
    private Integer typeCode; // No typename please as we want to be able to maps type between database
    /* Precision = Length for string, Precision =  Precision for Fix Number */
    private Integer precision;
    /* Only needed for number */
    private Integer scale;

    private String comment;


    public String getIsGeneratedColumn() {
        return isGeneratedColumn;
    }

    /**
     * Only called by the function get of a TableDef
     * To construct a column use TableDef.get
     *
     * @param relationDef
     */
    ColumnDef(RelationDef relationDef, String columnName, Class<T> clazz) {

        this.relationDef = relationDef;
        this.columnName = columnName;
        this.clazz = clazz;

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

        DataType dataType = null;
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
        final Database database = this.getRelationDef().getDatabase();
        if (database != null) {
            dataType = database.getDataType(typeCode);
        } else {
            dataType = new DataType.DataTypeBuilder(typeCode)
                    .JdbcDataType(DataTypesJdbc.of(typeCode))
                    .build();
        }

        // If the data type is not known
        if (dataType == null) {

            throw new RuntimeException("No DataType could be found for the type code (" + Strings.toStringNullSafe(typeCode) + " and the class (" + Strings.toStringNullSafe(clazz) + ")");

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

    public ColumnDef setNullable(Boolean nullable) {

        assert nullable != null;
        setNullable(nullable ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls);
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

    @Override
    public String toString() {
        return getFullyQualifiedName() + " " + getDataType().getTypeName() + '(' + precision + "," + scale + ") " + (nullable == DatabaseMetaData.columnNullable ? "null" : "not null");
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

    public ColumnDef comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     *
     * @param key - a key
     * @return
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     *
     * @param key - a key
     * @return
     */
    public Object getPropertyCaseIndependently(String key) {
        return Maps.getPropertyCaseIndependent(properties,key);
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public ColumnDef addProperty(String key, Object value) {

        properties.put(key, value);
        return this;

    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getComment() {
        return this.comment;
    }

    public Class<T> getClazz() {
        return this.clazz;
    }




}
