package com.tabulify.model;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import net.bytle.exception.CastException;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;

import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.tabulify.conf.Origin.DEFAULT;

/**
 * A class that contains a column data structure definition
 * <p>
 * A column represents a vertical arrangement of cells within a table
 *
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#columns">Web tabular model Columns</a>
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#columns">Web tabular metadata Columns</a>
 */
public class ColumnDefBase<T> implements ColumnDef<T> {


  private final SqlDataType<T> sqlDataType;


  /**
   * Variables may be generated
   * so the identifier is a string name
   */
  protected Map<String, com.tabulify.conf.Attribute> variables = new MapKeyIndependent<>();


  /**
   * Mandatory
   * Called also an Identifier in SQL
   * See {@link DatabaseMetaData#getIdentifierQuoteString()}
   */
  private final KeyNormalizer columnName;

  private SqlDataTypeNullable nullable = SqlDataTypeNullable.NULLABLE;
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
   * 0 = no precision
   */
  private int precision;
  /**
   * Only needed for number
   * 0 = no precision
   * It could be a short as in <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">...</a> but there is no short literal. Because the impact is minim, we use an int
   */
  private int scale;

  private String comment;

  @Override
  public Boolean isGeneratedColumn() {
    return isGeneratedColumn;
  }

  /**
   * Only called by the function of of a TableDef
   * To construct a column use TableDef.of
   */
  public ColumnDefBase(RelationDef relationDef, String columnName, SqlDataType<T> sqlDataType) {

    assert sqlDataType != null : "The sql data type cannot be null";
    assert relationDef != null : "The data def cannot be null";
    assert columnName != null : "The column name cannot be null";


    this.relationDef = relationDef;
    try {
      this.columnName = KeyNormalizer.create(columnName);
    } catch (CastException e) {
      throw new IllegalArgumentException("The column name (" + columnName + ") is not a valid column name. Error: " + e.getMessage(), e);
    }
    this.sqlDataType = sqlDataType;

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
    return nullable.isNullable();
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
    return columnName.toString();
  }

  @Override
  public KeyNormalizer getColumnNameNormalized() {
    return columnName;
  }

  @Override
  public int getPrecision() {
    return precision;
  }

  @Override
  public int getScale() {
    return scale;
  }

  @Override
  public RelationDef getRelationDef() {
    return relationDef;
  }

  @Override
  public SqlDataType<T> getDataType() {

    return this.sqlDataType;

  }

  @Override
  public ColumnDef<T> setColumnPosition(int columnPosition) {
    this.columnPosition = columnPosition;
    return this;
  }

  @Override
  public int getColumnPosition() {
    return columnPosition;
  }

  @Override
  public ColumnDef<T> setNullable(SqlDataTypeNullable nullable) {

    this.nullable = nullable;

    return this;

  }

  @Override
  public ColumnDef<T> setNullable(Boolean nullable) {

    setNullable(SqlDataTypeNullable.cast(nullable));
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
    return Integer.compare(this.getColumnPosition(), o.getColumnPosition());
  }


  @Override
  public ColumnDef<T> setIsAutoincrement(Boolean isAutoincrement) {
    this.isAutoincrement = isAutoincrement;
    return this;
  }

  /**
   * What is this ? derived column ?
   */
  @Override
  public ColumnDef<T> setIsGeneratedColumn(Boolean isGeneratedColumn) {
    this.isGeneratedColumn = isGeneratedColumn;
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

    //noinspection unchecked
    ColumnDef<T> columnDef = (ColumnDef<T>) o;

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
  public T getDefault() {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  @Override
  public String getComment() {
    return this.comment;
  }

  @Override
  public ColumnDef<T> setComment(String comment) {
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
  public ColumnDef<T> setVariable(String key, Object value) {
    throw new RuntimeException("The property column value (" + key + ") is unexpected. The column (" + this.getClass().getSimpleName() + ") does not implement (or have) any extra columns.");
  }

  @Override
  public ColumnDef<T> setVariable(AttributeEnum key, Object value) {
    com.tabulify.conf.Attribute attribute;
    try {
      attribute = this.getRelationDef().getDataPath().getConnection().getTabular().getVault().createAttribute(key, value, DEFAULT);
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


  /**
   * @return the sql class of the data
   */
  @Override
  public Class<T> getClazz() {
    return this.sqlDataType.getValueClass();
  }


  /**
   * @return the precision or the max for this data type
   */
  @Override
  public int getPrecisionOrMax() {
    return precision == 0 ? getDataType().getMaxPrecision() : precision;
  }

  @Override
  public ColumnDef<T> setAllVariablesFrom(ColumnDef<?> source) {
    source.getVariables().forEach(v -> this.variables.put(v.getAttributeMetadata().toString(), v));
    return this;
  }

  @Override
  public ColumnDef<T> setPrecision(int precision) {
    this.precision = precision;
    // Add it in a builder pattern
    // precision is now always 0 so we don't know if it was set or not
    //    if (this.scale > this.precision) {
    //      throw new IllegalStateException("Scale (" + this.scale + ") cannot be greater than precision (" + this.precision + ").");
    //    }
    return this;
  }

  @Override
  public ColumnDef<T> setScale(int scale) {
    this.scale = scale;
    // Add it in a builder pattern
    // precision is now always 0 so we don't know if it was set or not
    //    if (scale > this.precision) {
    //      throw new RuntimeException("Scale (" + this.scale + ") cannot be greater than precision (" + this.precision + ") for the column (" + this + ")");
    //    }
    return this;
  }

  @Override
  public SqlDataTypeAnsi getAnsiType() {
    SqlDataTypeAnsi ansiType = getDataType().getAnsiType();
    if (ansiType == SqlDataTypeAnsi.BIT && (this.getPrecision() == 1 || this.getPrecision() == 0)) {
      return SqlDataTypeAnsi.BOOLEAN;
    }
    return ansiType;
  }

  /**
   * A utility class to add the default variables when a columnDef is build
   *
   * @param enumClass - the class that holds all enum attribute
   * @return the column for chaining
   */
  public ColumnDef<T> addVariablesFromEnumAttributeClass(Class<? extends AttributeEnum> enumClass) {
    Arrays.asList(enumClass.getEnumConstants()).forEach(c -> {
      com.tabulify.conf.Attribute attribute = com.tabulify.conf.Attribute.create(c, Origin.DEFAULT);
      this.variables.put(c.toString(), attribute);
    });
    return this;
  }


}
