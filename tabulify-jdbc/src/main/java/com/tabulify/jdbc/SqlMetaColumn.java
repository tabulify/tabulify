package com.tabulify.jdbc;

import com.tabulify.model.ColumnDef;

import java.sql.DatabaseMetaData;

/**
 * Represents the column meta that we get from the database
 * This object is used to be able to path information
 * to {@link SqlConnectionProvider}
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
  private Boolean isGeneratedColumn;
  /**
   * The isAutoIncrement of the JDBC driver
   */
  private Boolean isAutoIncrement;
  /**
   * The COLUMN_SIZE of the JDBC driver (ie the size on the terminal display)
   * <p></p>
   * <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String-">JDBC definition</a>
   * For numeric data, this is the maximum precision.
   * For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation
   * For binary data, this is the length in bytes.
   * For the ROWID datatype, this is the length in bytes.
   * Null is returned for data types where the column size is not applicable.
   */
  private int columnSize;
  /**
   * The type code constant ({@link java.sql.Types}
   */
  private int typeCode;
  /**
   * The type name of the JDBC driver
   * It's handy when the type code constant is not the good one
   * or when retrieving the meta from the information schema
   * where the type code is not available.
   */
  private String typeName;
  /**
   * The DECIMAL_DIGITS of the JDBC driver
   * The number of fractional digits
   * (ie the precision, number of second for timestamp)
   */
  private Integer decimalDigits;
  /**
   * The NULLABLE value of the JDBC driver
   * See {@link ColumnDef#setNullable(Boolean)}
   */
  private Integer isNullable;
  /**
   * The position in the list of columns
   */
  private Integer position;
  /**
   * Note that the comment is known as remarks in jdbc
   */
  private String comment;


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

  public int getColumnSize() {
    return columnSize;
  }

  public int getDecimalDigits() {
    return decimalDigits;
  }

  public Boolean isAutoIncrement() {
    return isAutoIncrement;
  }

  public Boolean isGeneratedColumn() {
    return isGeneratedColumn;
  }

  public Integer isNullable() {
    return isNullable;
  }

  public SqlMetaColumn setIsGeneratedColumn(Boolean isGeneratedColumn) {
    this.isGeneratedColumn = isGeneratedColumn;
    return this;
  }

  public SqlMetaColumn setIsAutoIncrement(Boolean isAutoincrement) {
    this.isAutoIncrement = isAutoincrement;
    return this;
  }

  public SqlMetaColumn setColumnSize(int columnSize) {
    this.columnSize = columnSize;
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

  public SqlMetaColumn setDecimalDigits(int decimalDigits) {
    this.decimalDigits = decimalDigits;
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

  public SqlMetaColumn setComment(String columnComment) {
    this.comment = columnComment;
    return this;
  }

  public String getComment() {
    return this.comment;
  }

}
