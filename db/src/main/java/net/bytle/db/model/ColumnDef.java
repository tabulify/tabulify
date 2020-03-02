package net.bytle.db.model;

import net.bytle.db.DbLoggers;
import net.bytle.log.Log;

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
 * A column represents a vertical arrangement of cells within a table
 *
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#columns">Web tabular model Columns</a>
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#columns">Web tabular metadata Columns</a>
 */
public class ColumnDef<T> implements Comparable<ColumnDef<T>> {

  private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

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
  private Boolean isAutoincrement = false;
  private Boolean isGeneratedColumn = false;
  private RelationDef dataDef;
  private int columnPosition;
  private String fullyQualifiedName;

  // Default type code is given in the getter function
  private Integer typeCode; // No typename please as we want to be able to maps type between database
  /* Precision = Length for string, Precision =  Precision for Fix Number */
  /** JDBC returns a integer */
  private Integer precision;
  /* Only needed for number */
  private Integer scale;

  private String comment;

  public Boolean getIsGeneratedColumn() {
    return isGeneratedColumn;
  }

  /**
   * Only called by the function of of a TableDef
   * To construct a column use TableDef.of
   *
   * @param dataDef
   */
  public ColumnDef(RelationDef dataDef, String columnName, Class<T> clazz) {
    assert dataDef!=null: "The data def cannot be null";
    assert columnName!=null: "The column name cannot be null";
    assert clazz!=null: "The class cannot be null for the column "+columnName+" on the data path "+dataDef.getDataPath();

    this.dataDef = dataDef;
    this.columnName = columnName;

    // To point out where we write sqltype.getClass in place of getClazz
    if (clazz==SqlDataType.class){
      throw new RuntimeException("Bad class");
    }
    this.clazz = clazz;

  }

  /**
   * @return one of
   * DatabaseMetaData.columnNullable,
   * DatabaseMetaData.columnNoNulls,
   * DatabaseMetaData.columnNullableUnknown
   */
  public Boolean getNullable() {
    return nullable != DatabaseMetaData.columnNoNulls;
  }

  public Boolean getIsAutoincrement() {
    return isAutoincrement;
  }

  /**
   * @return the type code from the type code of from the class if it was not set
   */
  private Integer getTypeCode() {

    return getDataType().getTypeCode();

  }


  public String getColumnName() {
    return columnName;
  }

  public Integer getPrecision() {
    return precision;
  }

  public Integer getScale() {
    return scale;
  }

  public RelationDef getDataDef() {
    return dataDef;
  }

  public SqlDataType getDataType() {

    // The typecode of the clazz may be modified between two calls of datatype
    // It happens for instance with test
    // See also  getDataTypeOf(, clazz)
    if (this.typeCode == null && this.clazz == null) {

      // No data type defined, default to VARCHAR
      return this.dataDef.getDataPath().getDataStore().getSqlDataType(Types.VARCHAR);

    } else {

      // If the developer gave only the java data type (class)
      if (this.typeCode != null) {
        return this.dataDef.getDataPath().getDataStore().getSqlDataType(this.typeCode);
      } else {
        return this.dataDef.getDataPath().getDataStore().getSqlDataType(clazz);
      }
    }


  }

  public ColumnDef<T> setColumnPosition(int columnPosition) {
    this.columnPosition = columnPosition;
    return this;
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

    if ( nullable != null ) {
      setNullable(nullable ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls);
    }
    return this;

  }

  public String getFullyQualifiedName() {
    if (fullyQualifiedName == null) {
      fullyQualifiedName = dataDef.getDataPath() + "." + columnName;
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
    this.isAutoincrement = is_autoincrement.equals("YES");
    return this;
  }

  /**
   * What is this ? derived column ?
   *
   * @param is_generatedcolumn
   * @return
   */
  public ColumnDef isGeneratedColumn(String is_generatedcolumn) {
    this.isGeneratedColumn = is_generatedcolumn != null && is_generatedcolumn.equals("YES");
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
    return getFullyQualifiedName() + " " + getDataType().getTypeNames() + '(' + precision + "," + scale + ") " + (nullable == DatabaseMetaData.columnNullable ? "null" : "not null");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ColumnDef columnDef = (ColumnDef) o;

    if (!dataDef.equals(columnDef.dataDef)) return false;
    return getFullyQualifiedName().equals(columnDef.getFullyQualifiedName());
  }

  @Override
  public int hashCode() {
    int result = dataDef.hashCode();
    result = 31 * result + getFullyQualifiedName().hashCode();
    return result;
  }

  /**
   * TODO: not yet implemented
   *
   * @return the default value if any
   */
  public Object getDefault() {
    throw new RuntimeException("Not yet implemented");
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
   * @param key - a key (The get is case independent)
   * @return
   */
  public Object getProperty(String key) {
    return properties.get(key);
  }


  /**
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

  /**
   * If you got unchecked error when using this function,
   * use the function {@link net.bytle.type.Typess#safeCast(Object, Class)}
   *
   * @return the class of the data
   */
  public Class<T> getClazz() {
    return this.clazz;
  }


  /**
   *
   * @return the precision or the max for this data type
   */
  public Integer getPrecisionOrMax() {
    return precision ==null ? getDataType().getMaxPrecision() : precision;
  }
}
