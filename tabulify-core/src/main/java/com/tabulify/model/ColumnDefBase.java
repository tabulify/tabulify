package com.tabulify.model;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import net.bytle.type.MapKeyIndependent;

import java.sql.DatabaseMetaData;
import java.util.Arrays;
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

  /**
   * Variables may be generated
   * so the identifier is a string name
   */
  protected Map<String, com.tabulify.conf.Attribute> variables = new MapKeyIndependent<>();

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
  private Boolean isAutoincrement = null;
  private Boolean isGeneratedColumn = false;
  private final RelationDef relationDef;

  /**
   * The column position starts at 1
   */
  private int columnPosition;
  private String fullyQualifiedName;

  /**
   * Precision = Length for string,
   * Precision = Precision for Fix Number
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

    this.addVariablesFromEnumAttributeClass(ColumnAttribute.class);

  }

  /**
   * @return one of
   * {@link DatabaseMetaData#columnNullable},
   * {@link DatabaseMetaData#columnNoNulls},
   * {@link DatabaseMetaData#columnNullableUnknown}
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
    if (isAutoincrement == null) {
      return false;
    }
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
      fullyQualifiedName = getColumnName() + "@(" + this.getRelationDef().getDataPath() + ")";
    }
    return fullyQualifiedName;
  }

  @Override
  public int compareTo(ColumnDef o) {
    return this.getColumnPosition().compareTo(o.getColumnPosition());
  }


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


  @Override
  public ColumnDef setIsAutoincrement(Boolean isAutoincrement) {
    this.isAutoincrement = isAutoincrement;
    return this;
  }

  /**
   * What is this ? derived column ?
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

  public com.tabulify.conf.Attribute getVariable(AttributeEnum attribute) {
    return getVariable(attribute.toString());
  }

  public com.tabulify.conf.Attribute getVariable(String s) {
    return variables.get(s);
  }


  @Override
  public ColumnDef setVariable(String key, Object value) {
    throw new RuntimeException("The property column value (" + key + ") is unexpected. The column (" + this.getClass().getSimpleName() + ") does not implement (or have) any extra columns.");
  }

  @Override
  public ColumnDef setVariable(AttributeEnum key, Object value) {
    com.tabulify.conf.Attribute attribute;
    try {
      attribute = this.getRelationDef().getDataPath().getConnection().getTabular().createAttribute(key, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.variables.put(attribute.getAttributeMetadata().toString(), attribute);
    return this;
  }

//  /**
//   * @param name  - a name of the key space
//   * @param names - the names of the key
//   */
//  @Override
//  public ColumnDef setVariable(Object value, String name, String... names) {
//    switch (names.length) {
//      case 0:
//        return setVariable(name, value);
//      case 1:
//        Map<String, Object> mapProperty = getMapProperty(String.class, Object.class, name);
//        if (mapProperty == null) {
//          mapProperty = new HashMap<>();
//          this.attributes.put(name, mapProperty);
//        }
//        mapProperty.put(names[0], value);
//        return this;
//      default:
//        throw new IllegalArgumentException("A property with more than one namespace is not yet supported");
//    }
//
//  }


  @Override
  public Set<com.tabulify.conf.Attribute> getVariables() {
    return new HashSet<>(variables.values());
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
  public ColumnDef setAllVariablesFrom(ColumnDef source) {
    source.getVariables().forEach(v -> this.variables.put(v.getAttributeMetadata().toString(), v));
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

  /**
   * A utility class to add the default variables when a columnDef is build
   *
   * @param enumClass - the class that holds all enum attribute
   * @return the column for chaining
   */
  public ColumnDef addVariablesFromEnumAttributeClass(Class<? extends AttributeEnum> enumClass) {
    Arrays.asList(enumClass.getEnumConstants()).forEach(c -> {
      com.tabulify.conf.Attribute attribute = com.tabulify.conf.Attribute.create(c, Origin.DEFAULT);
      this.variables.put(c.toString(), attribute);
    });
    return this;
  }


}
