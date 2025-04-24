package com.tabulify.jdbc;

import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;

import java.sql.DatabaseMetaData;

/**
 * Represents the column meta that we get from the database
 * This object is used to be able to path information
 * to {@link SqlDataStoreProvider}
 * in order to correct them
 * before creating the columns
 * <p>
 * By default, they are created with the {@link SqlDataSystem#getMetaColumns(SqlDataPath)}
 * that takes this information from the JDBC driver {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)}
 */
public class SqlMetaColumn {

  /**
   * The name of the column
   */
  private final String columnName;
  /**
   * The IsGeneratedColumn of the JDBC driver
   */
  private String isGeneratedColumn;
  /**
   * The isAutoIncrement of the JDBC driver
   */
  private Boolean isAutoIncrement;
  /**
   * The COLUMN_SIZE of the JDBC driver (ie precision)
   * For numeric data, this is the maximum precision.
   * For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation
   * For binary data, this is the length in bytes.
   * For the ROWID datatype, this is the length in bytes.
   * Null is returned for data types where the column size is not applicable.
   */
  private Integer precision;
  /**
   * The type code constant ({@link java.sql.Types}
   */
  private Integer typeCode;
  /**
   * The type name of the JDBC driver
   * It's handy when the type code constant is not the good one
   * or when retrieving the meta from the information schema
   * where the type code is not available.
   */
  private String typeName;
  /**
   * The DECIMAL_DIGITS of the JDBC driver (the scale)
   */
  private Integer scale;
  /**
   * The NULLABLE value of the JDBC driver
   * See {@link ColumnDef#setNullable(int)}
   */
  private Integer isNullable;
  /**
   * The position in the list of columns
   */
  private Integer position;


  public SqlMetaColumn(String columnName) {
    this.columnName = columnName;
  }

  public static SqlMetaColumn createOf(String columnName) {
    return new SqlMetaColumn(columnName);
  }

  public Integer getTypeCode() {
    return typeCode;
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

  public Boolean isAutoIncrement() {
    return isAutoIncrement;
  }

  public String isGeneratedColumn() {
    return isGeneratedColumn;
  }

  public int isNullable() {
    return isNullable;
  }

  public SqlMetaColumn setIsGeneratedColumn(String isGeneratedcolumn) {
    this.isGeneratedColumn = isGeneratedcolumn;
    return this;
  }

  public SqlMetaColumn setIsAutoIncrement(Boolean isAutoincrement) {
    this.isAutoIncrement = isAutoincrement;
    return this;
  }

  public SqlMetaColumn setPrecision(Integer precision) {
    this.precision = precision;
    return this;
  }

  public SqlMetaColumn setTypeCode(Integer typeCode) {
    this.typeCode = typeCode;
    return this;
  }

  public SqlMetaColumn setTypeName(String typeName) {
    this.typeName = typeName;
    return this;
  }

  public SqlMetaColumn setScale(Integer scale) {
    this.scale = scale;
    return this;
  }

  public SqlMetaColumn setIsNullable(Integer nullable) {
    this.isNullable = nullable;
    return this;
  }

  public SqlMetaColumn setIsNullable(Boolean nullable) {
    if (nullable == null) {
      this.isNullable = DatabaseMetaData.columnNullableUnknown;
      return this;
    }
    if (nullable) {
      this.isNullable = DatabaseMetaData.columnNullable;
    } else {
      this.isNullable = DatabaseMetaData.columnNoNulls;
    }
    return this;
  }

  public String getTypeName() {
    return typeName;
  }


  public SqlMetaColumn setPosition(Integer position) {
    this.position = position;
    return this;
  }
}
