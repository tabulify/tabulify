package com.tabulify.model;

import com.tabulify.spi.DataPath;
import net.bytle.exception.CastException;
import net.bytle.type.Arrayss;
import net.bytle.type.Casts;
import net.bytle.type.MapKeyIndependent;
import net.bytle.type.Maps;

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class that contains a column data structure definition
 * <p>
 * A column represents a vertical arrangement of cells within a table
 *
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#columns">Web tabular model Columns</a>
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#columns">Web tabular metadata Columns</a>
 */
public class ColumnDefBase implements ColumnDef {


  private static final Set<Integer> allowedNullableValues = new HashSet<>();
  private final SqlDataType sqlDataType;

  /**
   * The data type of the value
   * It's not final because it may be
   * modified for instance if a data generator
   * is attached to the column in the data generator
   * module
   */
  protected Class<?> clazz;

  protected Map<String, Object> properties = new MapKeyIndependent<>();

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
  private Boolean isAutoincrement = false;
  private Boolean isGeneratedColumn = false;
  private final RelationDef relationDef;

  /**
   * The column position starts at 1
   */
  private int columnPosition;
  private String fullyQualifiedName;

  /* Precision = Length for string, Precision =  Precision for Fix Number */
  /**
   * JDBC returns a integer
   */
  private Integer precision;
  /* Only needed for number */
  private Integer scale;

  private String comment;

  @Override
  public Boolean isGeneratedColumn() {
    return isGeneratedColumn;
  }

  /**
   * Only called by the function of of a TableDef
   * To construct a column use TableDef.of
   */
  public ColumnDefBase(RelationDef relationDef, String columnName, Class<?> clazz, SqlDataType sqlDataType) {

    assert sqlDataType != null : "The sql data type cannot be null";
    assert relationDef != null : "The data def cannot be null";
    assert columnName != null : "The column name cannot be null";
    assert clazz != null : "The class cannot be null for the column " + columnName + " with the data type (" + sqlDataType + ") on the data path " + relationDef.getDataPath() + " for the column (" + columnName + ")";
    assert clazz == sqlDataType.getSqlClass() : "For the column (" + columnName + "), the classes given are different, argument class (" + clazz.getSimpleName() + ") vs type class (" + sqlDataType.getSqlClass().getSimpleName() + "). They should not be different.";

    this.relationDef = relationDef;
    this.columnName = columnName;
    this.sqlDataType = sqlDataType;
    this.clazz = clazz;


  }

  /**
   * @return one of
   * DatabaseMetaData.columnNullable,
   * DatabaseMetaData.columnNoNulls,
   * DatabaseMetaData.columnNullableUnknown
   */
  @Override
  public Boolean isNullable() {
    return
      nullable == DatabaseMetaData.columnNullable
        ||
        nullable == DatabaseMetaData.columnNullableUnknown;
  }

  @Override
  public Boolean isAutoincrement() {
    return isAutoincrement;
  }


  @Override
  public String getColumnName() {
    return columnName;
  }

  @Override
  public Integer getPrecision() {
    return precision;
  }

  @Override
  public Integer getScale() {
    return scale;
  }

  @Override
  public RelationDef getRelationDef() {
    return relationDef;
  }

  @Override
  public SqlDataType getDataType() {

    return this.sqlDataType;

  }

  @Override
  public ColumnDef setColumnPosition(int columnPosition) {
    this.columnPosition = columnPosition;
    return this;
  }

  @Override
  public Integer getColumnPosition() {
    return columnPosition;
  }

  @Override
  public ColumnDef setNullable(int nullable) {

    if (!allowedNullableValues.contains(nullable)) {
      throw new RuntimeException("The value (" + nullable + ") is unknown");
    } else {
      this.nullable = nullable;
    }
    return this;

  }

  @Override
  public ColumnDef setNullable(Boolean nullable) {

    if (nullable != null) {
      setNullable(nullable ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls);
    }
    return this;

  }

  @Override
  public String getFullyQualifiedName() {
    if (fullyQualifiedName == null) {
      DataPath path = relationDef.getDataPath();
      fullyQualifiedName = getColumnName() + "@(" + this.getRelationDef().getDataPath() + ")";
    }
    return fullyQualifiedName;
  }

  @Override
  public int compareTo(ColumnDef o) {
    return this.getColumnPosition().compareTo(o.getColumnPosition());
  }


  @SuppressWarnings("rawtypes")
  @Override
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
   * @param isAutoincrement YES, NO or '' Empty string: not known
   * @return the object for chaining
   */
  @Override
  public ColumnDef setIsAutoincrement(String isAutoincrement) {
    this.isAutoincrement = isAutoincrement.equals("YES");
    return this;
  }

  /**
   * What is this ? derived column ?
   *
   * @param isGeneratedColumn
   * @return
   */
  @Override
  public ColumnDef setIsGeneratedColumn(String isGeneratedColumn) {
    this.isGeneratedColumn = isGeneratedColumn != null && isGeneratedColumn.equals("YES");
    return this;
  }

  @Override
  public ColumnDef scale(Integer scale) {

    if (scale != null) {
      if (this.precision != null) {
        if (scale > this.precision) {
          throw new RuntimeException("Scale (" + this.scale + ") cannot be greater than precision (" + this.precision + ").");
        }
      }
    }
    this.scale = scale;
    return this;
  }

  @Override
  public String toString() {
    return getFullyQualifiedName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ColumnDef columnDef = (ColumnDef) o;

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
   * @return the default value if any
   */
  @Override
  public Object getDefault() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * TODO: not yet implemented
   *
   * @return
   */
  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public ColumnDef setComment(String comment) {
    this.comment = comment;
    return this;
  }


  /**
   * @param names - a key (The get is case independent)
   * @return object for chaining
   */
  @Override
  public <T> T getVariable(Class<T> clazz, String name, String... names) {
    Object o = Maps.getPropertyCaseIndependent(properties, name);
    if (o == null) {
      return null;
    }
    switch (names.length) {
      case 0:
        try {
          return Casts.cast(o, clazz);
        } catch (CastException e) {
          throw new RuntimeException("Unable to cast the property (" + name + " from the column (" + this + ")", e);
        }
      case 1:
        String namespace = names[0];
        if (o instanceof Map) {
          /**
           * The below line should not give an error because it's a string, object
           * map and we know that it's already a map
           */
          Map<String, Object> oMap = Casts.castToNewMapSafe(o, String.class, Object.class);
          try {
            return Casts.cast(Maps.getPropertyCaseIndependent(oMap, namespace), clazz);
          } catch (CastException e) {
            throw new RuntimeException("Unable to cast the property (" + Arrayss.toJoinedString(".", Arrayss.concat(name, names)) + " from the column (" + this + ")", e);
          }
        } else {
          throw new RuntimeException("The namespace (" + namespace + ") property should be a map in order to get a sub property key of the column (" + this + "). Values:" + o + ", Type:" + o.getClass().getSimpleName());
        }
      default:
        throw new IllegalArgumentException("The retrieving of a property on more than 2 level is not yet supported. The names arguments length (" + Arrayss.toJoinedStringWithComma(names) + " should be less or equal to one value and it has " + names.length);
    }
  }

  /**
   * An utility function to return a map from a property
   * This function calls the function {@link #getVariable(Class, String, String...)}
   * and cast the return value to a map
   *
   * @param keyClazz   - the class of the key
   * @param valueClazz - the class of the value
   * @param name       - the first name of the path
   * @param names      - the path to the property
   * @param <K>
   * @param <V>
   * @return
   */
  @Override
  public <K, V> Map<K, V> getMapProperty(Class<K> keyClazz, Class<V> valueClazz, String name, String... names) {

    Object o = getVariable(Object.class, name, names);
    if (o == null) {
      return null;
    }
    try {
      return Casts.castToNewMap(o, keyClazz, valueClazz);
    } catch (CastException e) {
      throw new RuntimeException("Unable to cast the value (" + o + ") from the property (" + Arrayss.toJoinedString(".", Arrayss.concat(name, names)) + ") from the column (" + this + ") into a map because " + e.getMessage(), e);
    }

  }


  /**
   * @param key
   * @param value
   * @return
   */
  @Override
  public ColumnDef setVariable(String key, Object value) {

    properties.put(key, value);
    return this;

  }

  /**
   * @param name  - a name of the key space
   * @param names - the names of the key
   * @param value
   * @return
   */
  @Override
  public ColumnDef setVariable(Object value, String name, String... names) {
    switch (names.length) {
      case 0:
        return setVariable(name, value);
      case 1:
        Map<String, Object> mapProperty = getMapProperty(String.class, Object.class, name);
        if (mapProperty == null) {
          mapProperty = new HashMap<>();
          this.properties.put(name, mapProperty);
        }
        mapProperty.put(names[0], value);
        return this;
      default:
        throw new IllegalArgumentException("A property with more than one namespace is not yet supported");
    }

  }


  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }

  @Override
  public String getComment() {
    return this.comment;
  }

  /**
   * @return the sql class of the data
   */
  @Override
  public Class<?> getClazz() {
    return this.clazz;
  }


  /**
   * @return the precision or the max for this data type
   */
  @Override
  public Integer getPrecisionOrMax() {
    return precision == null ? getDataType().getMaxPrecision() : precision;
  }

  @Override
  public ColumnDef setAllPropertiesFrom(ColumnDef source) {
    this.properties.putAll(source.getProperties());
    return this;
  }

  @Override
  public ColumnDef setPrecision(Integer precision) {
    this.precision = precision;
    return this;
  }

  @Override
  public ColumnDef setScale(Integer scale) {
    this.scale = scale;
    return this;
  }
}
