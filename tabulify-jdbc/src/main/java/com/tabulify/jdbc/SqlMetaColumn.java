package com.tabulify.jdbc;

import com.tabulify.spi.DataPath;

/**
 * Represents the column meta that we get from the database
 * This object is used to be able to path information
 * to {@link SqlDataStoreProvider}
 * in order to correct them
 * before creating the columns
 *
 * By default, they are created with the {@link SqlDataSystem#getMetaColumns(SqlDataPath)}
 * that takes this information from the JDBC driver {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)}
 */
public class SqlMetaColumn {

  /**
   * The name of the column
   */
  private final String columnName;
  /**
   * The data path
   */
  private final DataPath dataPath;
  /**
   * The IsGeneratedColumn of the JDBC driver
   */
  private String isGeneratedColumn;
  /**
   * The isAutoIncrement of the JDBC driver
   */
  private String isAutoIncrement;
  /**
   * The COLUMN_SIZE of the JDBC driver (ie precision)
   */
  private Integer precision;
  /**
   * The type code constant ({@link java.sql.Types}
   */
  private Integer typeCode;
  /**
   * The type name of the JDBC driver
   * It's handy when the type code constant is not the good one
   */
  private String typeName;
  /**
   * The DECIMAL_DIGITS of the JDBC driver (the scale)
   */
  private Integer scale;
  /**
   * The NULLABLE value of the JDBC driver
   */
  private Integer isNullable;


  public SqlMetaColumn(DataPath dataPath, String columnName) {
    this.dataPath = dataPath;
    this.columnName = columnName;
  }

  public static SqlMetaColumn createOf(DataPath dataPath, String columnName) {
    return new SqlMetaColumn(dataPath, columnName);
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

  public String isAutoIncrement() {
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

  public SqlMetaColumn setIsAutoIncrement(String isAutoincrement) {
    this.isAutoIncrement = isAutoincrement;
      return this;
  }

  public SqlMetaColumn setPrecision(Integer precision) {
    this.precision = precision;
    return this;
  }

  public SqlMetaColumn setTypeCode(int typeCode) {
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

  public String getTypeName() {
    return typeName;
  }


}
