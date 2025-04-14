package com.tabulify.excel;

import com.tabulify.model.RelationDef;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Logger;


/**
 * A Sql result set implementation
 * First tabulify cursor implementation
 * See <a href="https://poi.apache.org/components/spreadsheet/quick-guide.html">...</a>
 */
public class ExcelResultSet implements ResultSet {

  private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());
  private final Sheet sheet;
  private final ExcelSheet excelSheet;


  // Excel Poi Row Num
  // As zero is the first row num, null means that there are no data in the sheet
  private Integer firstPhysicalRowNum = null;
  private boolean dataSetIsEmpty = true;

  // The lastPhysicalRowNum of the data Set
  // It's used for instance to insert a row at the end ...
  // null if there is no row, no header
  private Integer lastPhysicalRowNum;
  // The last columnNum is defined by the first row size
  // may be null
  private Short lastColumnNum;


  // The logical location of the row
  // 0 is before
  // 1 is the first row
  // ....
  private int logicalRowIndex = 0;


  private ResultSetMetaData resultSetMetaData;


  /**
   * Read a new sourceResultSet
   * <p>
   * The format is chosen from the extension sourceResultSet
   * * xlsx: Excel
   * * csv: CSV
   *
   * @param excelSheet - path to the excel file
   */
  public ExcelResultSet(ExcelSheet excelSheet) {

    this.excelSheet = excelSheet;
    this.sheet = this.excelSheet.getSheet();

    // The last row num with data
    this.lastPhysicalRowNum = getLastRowNum();

    this.firstPhysicalRowNum = excelSheet.getHeaderRowId();

    if (this.firstPhysicalRowNum <= this.lastPhysicalRowNum) {
      this.dataSetIsEmpty = false;
    } else {
      this.dataSetIsEmpty = true;
    }
    // The last ColumnNum with data
    this.lastColumnNum = getLastColNum();

    // Metadata Building
    if (excelSheet.getHeaderRowId() != 0) {
      Row headerRow = sheet.getRow(this.getExcelRowIndex(excelSheet.getHeaderRowId()));
      int columnIndex = 0;
      headerNames = new TreeBidiMap<>();
      for (Cell cell : headerRow) {
        if (cell.getCellType() != CellType.STRING) {
          throw new IllegalArgumentException("The cell (" + cell.getRowIndex() + "," + cell.getColumnIndex() + ") with the value (" + ExcelSheets.getCellValue(cell, String.class) + ") can be an header as it is not of STRING type but of type (" + getCellTypeName(cell.getCellType()) + ")");
        } else {
          columnIndex++;
          headerNames.put(cell.getStringCellValue(), columnIndex);
        }
      }
    }

    // DataType Building
    // The data type comes from the first row of data
    // We need to read it
    // rowId is zero based so getRow(headerRowId) get the next row
    int nextRow = excelSheet.getHeaderRowId();
    Row firstRowWithData = sheet.getRow(nextRow);
    if (firstRowWithData != null) {

      int columnIndex = 0;
      for (Cell cell : firstRowWithData) {

        columnIndex++;
        headerCells.put(columnIndex, cell);

      }
    }
    // empty sheet, data type may be given manually
    // throw new IllegalArgumentException("The first row of data must be present in order to determine the data type of each column automatically. The row (" + (nextRow + 1) + ") does not exist");



  }


  /**
   * Moves the cursor forward one row from its current position.
   * A <code>DataSet</code> cursor is initially positioned
   * before the first row; the first call to the method
   * <code>next</code> makes the first row the current row; the
   * second call makes the second row the current row, and so on.
   * <p/>
   * When a call to the <code>next</code> method returns <code>false</code>,
   * the cursor is positioned after the last row. Any
   * invocation of a <code>DataSet</code> method which requires a
   * current row will result in a <code>DataSetException</code> being thrown.
   *
   * @return <code>true</code> if the new current row is valid;
   * <code>false</code> if there are no more rows
   */

  public boolean next() {
    if (this.logicalRowIndex > this.lastPhysicalRowNum) {
      return false;
    } else {
      this.logicalRowIndex++;
      return isCursorInDataSet();
    }
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>DataSet</code> object as
   * a <code>String</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws Exception if the columnIndex is not valid;
   *                   if a access error occurs or this method is
   *                   called on a closed result set
   */

  public String getString(int columnIndex) {

    return cast(null, columnIndex, String.class);

  }

  /* This function check if the cell has valid coordinates.
   * It is used by all cell operations such as read and updates
   */
  private void validateCell(int rowIndex, int columnIndex) {
    validateRowIndex(rowIndex);
    validateColumnIndex(columnIndex);
  }

  /**
   * Validate the Row index
   *
   * @param rowIndex
   */
  private void validateRowIndex(int rowIndex) {
    if (rowIndex < 1) {
      throw new RuntimeException("The row index (" + rowIndex + ") cannot be below 1");
    } else if (rowIndex > size()) {
      throw new RuntimeException("The row index (" + rowIndex + ") can not be greater than the number of row (" + size() + ")");
    }
  }

  /**
   * Validate the columnIndex
   *
   * @param columnIndex
   */
  private void validateColumnIndex(int columnIndex) {
    if (columnIndex < 1) {
      throw new RuntimeException("The column index (" + columnIndex + ") must not be negative or null");
    } else if (columnIndex > this.lastColumnNum) {
      throw new RuntimeException("The column index (" + columnIndex + ") can not be greater than the last one (" + this.lastColumnNum + ")");
    }
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>DataSet</code> object as
   * a <code>double</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is <code>NULL</code> (ie BLANK type in Excel), the
   * value returned is <code>0</code>
   * @throws Exception if the columnIndex is not valid;
   *                   if an access error occurs or this method is
   *                   called on a closed result set
   */

  public Double getNumeric(int columnIndex) {


    return this.cast(null, columnIndex, Double.class);

  }


  public boolean getBoolean(int columnIndex) {


    return cast(null, columnIndex, Boolean.class);

  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>byte</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public byte getByte(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>short</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public short getShort(int columnIndex) throws SQLException {
    return 0;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>int</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public int getInt(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>long</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public long getLong(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>float</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public float getFloat(int columnIndex) throws SQLException {
    return 0;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>double</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public double getDouble(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.BigDecimal</code> in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param scale       the number of digits to the right of the decimal point
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs or this method is
   *                                         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @deprecated Use {@code getBigDecimal(int columnIndex)}
   * or {@code getBigDecimal(String columnLabel)}
   */
  @Deprecated
  @Override
  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>byte</code> array in the Java programming language.
   * The bytes represent the raw values returned by the driver.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public byte[] getBytes(int columnIndex) throws SQLException {
    return new byte[0];
  }


  public boolean getBoolean(String columnLabel) {
    return getBoolean(getColumnIndex(columnLabel));
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>byte</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public byte getByte(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>short</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public short getShort(String columnLabel) throws SQLException {
    return 0;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>int</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public int getInt(String columnLabel) throws SQLException {
    return 0;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>long</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public long getLong(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>float</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public float getFloat(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>double</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public double getDouble(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.math.BigDecimal</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param scale       the number of digits to the right of the decimal point
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs or this method is
   *                                         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @deprecated Use {@code getBigDecimal(int columnIndex)}
   * or {@code getBigDecimal(String columnLabel)}
   */
  @Deprecated
  @Override
  public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
    throw new UnsupportedOperationException("Deprecated");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>byte</code> array in the Java programming language.
   * The bytes represent the raw values returned by the driver.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public byte[] getBytes(String columnLabel) throws SQLException {
    return new byte[0];
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>DataSet</code> object as
   * a <code>Date</code> object in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is <code>NULL</code> (ie the type BLANK in Excel), the
   * value returned is <code>null</code>
   * @throws Exception if the columnIndex is not valid;
   *                   if a database access error occurs or this method is
   *                   called on a closed result set
   */

  public Date getDate(int columnIndex) {


    return cast(null, columnIndex, Date.class);

  }

  // Return the physique coordinates from a logical one
  private int getPhysicalRowIndex(int logicalRowIndex) {
    return logicalRowIndex + this.firstPhysicalRowNum;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Time</code> object in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public Time getTime(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Timestamp</code> object in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public Timestamp getTimestamp(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a stream of ASCII characters. The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large <code>LONGVARCHAR</code> values.
   * The JDBC driver will
   * do any necessary conversion from the database format into ASCII.
   * <p>
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream.  Also, a
   * stream may return <code>0</code> when the method
   * <code>InputStream.available</code>
   * is called whether there is data available or not.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a Java input stream that delivers the database column value
   * as a stream of one-byte ASCII characters;
   * if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public InputStream getAsciiStream(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * as a stream of two-byte 3 characters. The first byte is
   * the high byte; the second byte is the low byte.
   * <p>
   * The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large <code>LONGVARCHAR</code>values.  The
   * JDBC driver will do any necessary conversion from the database
   * format into Unicode.
   * <p>
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream.
   * Also, a stream may return <code>0</code> when the method
   * <code>InputStream.available</code>
   * is called, whether there is data available or not.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a Java input stream that delivers the database column value
   * as a stream of two-byte Unicode characters;
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code>
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs or this method is
   *                                         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @deprecated use <code>getCharacterStream</code> in place of
   * <code>getUnicodeStream</code>
   */
  @Deprecated
  @Override
  public InputStream getUnicodeStream(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Deprecated");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a  stream of
   * uninterpreted bytes. The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large <code>LONGVARBINARY</code> values.
   * <p>
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream.  Also, a
   * stream may return <code>0</code> when the method
   * <code>InputStream.available</code>
   * is called whether there is data available or not.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a Java input stream that delivers the database column value
   * as a stream of uninterpreted bytes;
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code>
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public InputStream getBinaryStream(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>DataSet</code> object as
   * a <code>String</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified
   * @return the column value; if the value is  <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws Exception if the columnLabel is not valid;
   *                   if an access error occurs or this method is
   *                   called on a closed result set
   */

  public String getString(String columnLabel) {
    return getString(getColumnIndex(columnLabel));
  }


  private int getCursorType() {
    return TYPE_SCROLL_SENSITIVE;
  }


  public InputStream getBinary(int i) {
    throw new UnsupportedOperationException("An sourceResultSet sourceResultSet cannot store a sourceResultSet");
    // We can assume that it's a path or URI for instance to implement it
  }


  public InputStream getBinary(String columnLabel) {
    return getBinary(getColumnIndex(columnLabel));
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>DataSet</code> object as
   * a <code>double</code> in the Java programming language.
   *
   * @param columnLabel the label for the column specified.
   * @return the column value; if the value is <code>NULL</code> (ie BLANK type in Excel), the
   * value returned is <code>0</code>
   * @throws Exception if the columnLabel is not valid;
   *                   if a access error occurs or this method is
   *                   called on a closed result set
   */

  public double getNumeric(String columnLabel) {
    return getNumeric(getColumnIndex(columnLabel));
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>DataSet</code> object as
   * a <code>java.sql.Date</code> object in the Java programming language.
   *
   * @param columnLabel the label for the column specified
   * @return the column value; if the value is <code>NULL</code> (ie BLANCK type in Excel), the
   * value returned is <code>null</code>
   * @throws Exception if the columnLabel is not valid;
   *                   if a database access error occurs or this method is
   *                   called on a closed result set
   */

  public Date getDate(String columnLabel) {
    return getDate(getColumnIndex(columnLabel));
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Time</code> object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public Time getTime(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.sql.Timestamp</code> object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public Timestamp getTimestamp(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a stream of
   * ASCII characters. The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large <code>LONGVARCHAR</code> values.
   * The JDBC driver will
   * do any necessary conversion from the database format into ASCII.
   * <p>
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream. Also, a
   * stream may return <code>0</code> when the method <code>available</code>
   * is called whether there is data available or not.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a Java input stream that delivers the database column value
   * as a stream of one-byte ASCII characters.
   * If the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code>.
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public InputStream getAsciiStream(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a stream of two-byte
   * Unicode characters. The first byte is the high byte; the second
   * byte is the low byte.
   * <p>
   * The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large <code>LONGVARCHAR</code> values.
   * The JDBC technology-enabled driver will
   * do any necessary conversion from the database format into Unicode.
   * <p>
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream.
   * Also, a stream may return <code>0</code> when the method
   * <code>InputStream.available</code> is called, whether there
   * is data available or not.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a Java input stream that delivers the database column value
   * as a stream of two-byte Unicode characters.
   * If the value is SQL <code>NULL</code>, the value returned
   * is <code>null</code>.
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs or this method is
   *                                         called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @deprecated use <code>getCharacterStream</code> instead
   */
  @Deprecated
  @Override
  public InputStream getUnicodeStream(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Deprecated");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a stream of uninterpreted
   * <code>byte</code>s.
   * The value can then be read in chunks from the
   * stream. This method is particularly
   * suitable for retrieving large <code>LONGVARBINARY</code>
   * values.
   * <p>
   * <P><B>Note:</B> All the data in the returned stream must be
   * read prior to getting the value of any other column. The next
   * call to a getter method implicitly closes the stream. Also, a
   * stream may return <code>0</code> when the method <code>available</code>
   * is called whether there is data available or not.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a Java input stream that delivers the database column value
   * as a stream of uninterpreted bytes;
   * if the value is SQL <code>NULL</code>, the result is <code>null</code>
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public InputStream getBinaryStream(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the first warning reported by calls on this
   * <code>ResultSet</code> object.
   * Subsequent warnings on this <code>ResultSet</code> object
   * will be chained to the <code>SQLWarning</code> object that
   * this method returns.
   * <p>
   * <P>The warning chain is automatically cleared each time a new
   * row is read.  This method may not be called on a <code>ResultSet</code>
   * object that has been closed; doing so will cause an
   * <code>SQLException</code> to be thrown.
   * <p>
   * <B>Note:</B> This warning chain only covers warnings caused
   * by <code>ResultSet</code> methods.  Any warning caused by
   * <code>Statement</code> methods
   * (such as reading OUT parameters) will be chained on the
   * <code>Statement</code> object.
   *
   * @return the first <code>SQLWarning</code> object reported or
   * <code>null</code> if there are none
   * @throws SQLException if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public SQLWarning getWarnings() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Clears all warnings reported on this <code>ResultSet</code> object.
   * After this method is called, the method <code>getWarnings</code>
   * returns <code>null</code> until a new warning is
   * reported for this <code>ResultSet</code> object.
   *
   * @throws SQLException if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public void clearWarnings() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the name of the SQL cursor used by this <code>ResultSet</code>
   * object.
   * <p>
   * <P>In SQL, a result table is retrieved through a cursor that is
   * named. The current row of a result set can be updated or deleted
   * using a positioned update/delete statement that references the
   * cursor name. To insure that the cursor has the proper isolation
   * level to support update, the cursor's <code>SELECT</code> statement
   * should be of the form <code>SELECT FOR UPDATE</code>. If
   * <code>FOR UPDATE</code> is omitted, the positioned updates may fail.
   * <p>
   * <P>The JDBC API supports this SQL feature by providing the name of the
   * SQL cursor used by a <code>ResultSet</code> object.
   * The current row of a <code>ResultSet</code> object
   * is also the current row of this SQL cursor.
   *
   * @return the SQL name for this <code>ResultSet</code> object's cursor
   * @throws SQLException                    if a database access error occurs or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   */
  @Override
  public String getCursorName() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the  number, types and properties of
   * this <code>ResultSet</code> object's columns.
   *
   * @return the description of this <code>ResultSet</code> object's columns
   * @throws SQLException if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return new ExcelMeta();
  }

  /**
   * <p>Gets the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>Object</code> in the Java programming language.
   * <p>
   * <p>This method will return the value of the given column as a
   * Java object.  The type of the Java object will be the default
   * Java object type corresponding to the column's SQL type,
   * following the mapping for built-in types specified in the JDBC
   * specification. If the value is an SQL <code>NULL</code>,
   * the driver returns a Java <code>null</code>.
   * <p>
   * <p>This method may also be used to read database-specific
   * abstract data types.
   * <p>
   * In the JDBC 2.0 API, the behavior of method
   * <code>getObject</code> is extended to materialize
   * data of SQL user-defined types.
   * <p>
   * If <code>Connection.getTypeMap</code> does not throw a
   * <code>SQLFeatureNotSupportedException</code>,
   * then when a column contains a structured or distinct value,
   * the behavior of this method is as
   * if it were a call to: <code>getObject(columnIndex,
   * this.getStatement().getConnection().getTypeMap())</code>.
   * <p>
   * If <code>Connection.getTypeMap</code> does throw a
   * <code>SQLFeatureNotSupportedException</code>,
   * then structured values are not supported, and distinct values
   * are mapped to the default Java class as determined by the
   * underlying SQL type of the DISTINCT type.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>java.lang.Object</code> holding the column value
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public Object getObject(int columnIndex) throws SQLException {
    validateCell(logicalRowIndex, columnIndex);
    int physicalRowIndex = getPhysicalRowIndex(logicalRowIndex);
    int sheetRowIndex = this.getExcelRowIndex(physicalRowIndex);
    int sheetColumnIndex = this.getExcelColumnIndex(columnIndex);
    return this.sheet.getRow(sheetRowIndex).getCell(sheetColumnIndex);
  }

  /**
   * <p>Gets the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>Object</code> in the Java programming language.
   * <p>
   * <p>This method will return the value of the given column as a
   * Java object.  The type of the Java object will be the default
   * Java object type corresponding to the column's SQL type,
   * following the mapping for built-in types specified in the JDBC
   * specification. If the value is an SQL <code>NULL</code>,
   * the driver returns a Java <code>null</code>.
   * <p>
   * This method may also be used to read database-specific
   * abstract data types.
   * <p>
   * In the JDBC 2.0 API, the behavior of the method
   * <code>getObject</code> is extended to materialize
   * data of SQL user-defined types.  When a column contains
   * a structured or distinct value, the behavior of this method is as
   * if it were a call to: <code>getObject(columnIndex,
   * this.getStatement().getConnection().getTypeMap())</code>.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>java.lang.Object</code> holding the column value
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public Object getObject(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Maps the given <code>ResultSet</code> column label to its
   * <code>ResultSet</code> column index.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column index of the given column name
   * @throws SQLException if the <code>ResultSet</code> object
   *                      does not contain a column labeled <code>columnLabel</code>, a database access error occurs
   *                      or this method is called on a closed result set
   */
  @Override
  public int findColumn(String columnLabel) throws SQLException {
    return 0;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.io.Reader</code> object.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>java.io.Reader</code> object that contains the column
   * value; if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   * @since 1.2
   */
  @Override
  public Reader getCharacterStream(int columnIndex) throws SQLException {
    return null;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.io.Reader</code> object.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>java.io.Reader</code> object that contains the column
   * value; if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   * @since 1.2
   */
  @Override
  public Reader getCharacterStream(String columnLabel) throws SQLException {
    return null;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.math.BigDecimal</code> with full precision.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   * @since 1.2
   */
  @Override
  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    return null;
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.math.BigDecimal</code> with full precision.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs or this method is
   *                      called on a closed result set
   * @since 1.2
   */
  @Override
  public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    return null;
  }


  /**
   * Retrieves whether the cursor is before the first row in
   * this <code>DataSet</code> object.
   *
   * @return <code>true</code> if the cursor is before the first row;
   * <code>false</code> if the cursor is at any other position or the
   * result set contains no rows
   * @throws Exception if a access error occurs or this method is
   *                   called on a closed data set
   */

  public boolean isBeforeFirst() {
    if (this.getRow() < this.firstPhysicalRowNum) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Retrieves whether the cursor is after the last row in
   * this <code>DataSet</code> object.
   * <p/>
   *
   * @return <code>true</code> if the cursor is after the last row;
   * <code>false</code> if the cursor is at any other position or the
   * result set contains no rows
   * @throws Exception if a database access error occurs or this method is
   *                   called on a closed result set
   */

  public boolean isAfterLast() {
    if (getRow() > this.lastPhysicalRowNum) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Retrieves whether the cursor is on the first row of
   * this <code>DataSet</code> object.
   * <p/>
   *
   * @return <code>true</code> if the cursor is on the first row;
   * <code>false</code> otherwise
   * @throws Exception if a access error occurs or this method is
   *                   called on a closed result set
   */

  public boolean isFirst() {
    if (this.getRow() == this.firstPhysicalRowNum) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Retrieves whether the cursor is on the last row of
   * this <code>DataSet</code> object.
   * <p/>
   * <strong>Note:</strong> Support for the <code>isLast</code> method
   * is optional for <code>DataSet</code>s with a result
   * set type of <code>TYPE_FORWARD_ONLY</code>
   *
   * @return <code>true</code> if the cursor is on the last row;
   * <code>false</code> otherwise
   * @throws Exception if a database access error occurs or this method is
   *                   called on a closed result set
   *                   // @throws FeatureNotSupportedException if the the method is not supported
   */

  public boolean isLast() {
    if (getRow() == size()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Moves the cursor to the front of
   * this <code>DataSet</code> object, just before the
   * first row. This method has no effect if the result set contains no rows.
   *
   * @throws Exception if a  access error occurs;
   *                   // @throws FeatureNotSupportedException if the driver does not support this method
   * @since 1.2
   */

  public void beforeFirst() {
    if (!this.dataSetIsEmpty) {
      this.logicalRowIndex = this.firstPhysicalRowNum - 1;
    }
  }

  /**
   * Moves the cursor to the first row in
   * this <code>DataSet</code> object.
   *
   * @return <code>true</code> if the cursor is on a valid row;
   * <code>false</code> if there are no rows in the result set
   * @throws Exception if a access error occurs
   *                   // @throws FeatureNotSupportedException if the JDBC driver does not support
   *                   this method
   */

  public boolean first() {
    if (!this.dataSetIsEmpty) {
      this.logicalRowIndex = this.firstPhysicalRowNum;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Retrieves the current row number.  The first row is number 1, the
   * second number 2, and so on.
   * <p>
   * <strong>Note:</strong>Support for the <code>getRow</code> method
   * is optional for <code>ResultSet</code>s with a result
   * set type of <code>TYPE_FORWARD_ONLY</code>
   *
   * @return the current row number; <code>0</code> if there is no current row
   * @throws SQLException                    if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public int getRow() {

    return this.logicalRowIndex;

  }

  /**
   * Moves the cursor to the given row number in
   * this <code>DataSet</code> object.
   * <p/>
   * <p>If the row number is positive, the cursor moves to
   * the given row number with respect to the
   * beginning of the result set.  The first row is row 1, the second
   * is row 2, and so on.
   * <p/>
   * <p>If the given row number is negative, the cursor moves to
   * an absolute row position with respect to
   * the end of the data set.  For example, calling the method
   * <code>absolute(-1)</code> positions the
   * cursor on the last row; calling the method <code>absolute(-2)</code>
   * moves the cursor to the next-to-last row, and so on.
   * <p/>
   * <p>If the row number specified is zero, the cursor is moved to
   * before the first row.
   * <p/>
   * <p>An attempt to position the cursor beyond the first/last row in
   * the result set leaves the cursor before the first row or after
   * the last row.
   * <p/>
   * <p><B>Note:</B> Calling <code>absolute(1)</code> is the same
   * as calling <code>first()</code>. Calling <code>absolute(-1)</code>
   * is the same as calling <code>last()</code>.
   *
   * @param row the number of the row to which the cursor should move.
   *            A value of zero indicates that the cursor will be positioned
   *            before the first row; a positive number indicates the row number
   *            counting from the beginning of the result set; a negative number
   *            indicates the row number counting from the end of the result set
   * @return <code>true</code> if the cursor is moved to a position in this
   * <code>DataSet</code> object;
   * <code>false</code> if the cursor is before the first row or after the
   * last row
   * @throws Exception if a database access error
   *                   occurs; this method is called on a closed result set
   *                   or the result set type is <code>TYPE_FORWARD_ONLY</code>
   *                   //@throws FeatureNotSupportedException if the driver does not support this method
   */

  public boolean absolute(int row) {

    if (row < 0) {
      if (Math.abs(row) <= this.size()) {
        // row is negative
        // -1, you are on the last row
        this.logicalRowIndex = size() + 1 + row;
      } else {
        this.logicalRowIndex = 0;
      }
    } else if (row == 0) {
      this.logicalRowIndex = 0;
    } else if (row > 0) {
      if (row <= this.size()) {
        this.logicalRowIndex = row;
      } else {
        this.logicalRowIndex = this.size() + 1;
      }
    }

    return isCursorInDataSet();
  }

  private boolean isCursorInDataSet() {
    Boolean cursorInDataSet = null;
    try {
      validateRowIndex(this.logicalRowIndex);
      cursorInDataSet = true;
    } catch (Exception e) {
      //TODO: Hum hum can we not have here a live exception
      //to be sure to get this exception
      cursorInDataSet = false;
    }
    return cursorInDataSet;
  }

  /**
   * Moves the cursor a relative number of rows, either positive or negative.
   * Attempting to move beyond the first/last row in the
   * result set positions the cursor before/after the
   * the first/last row. Calling <code>relative(0)</code> is valid, but does
   * not change the cursor position.
   * <p/>
   * <p>Note: Calling the method <code>relative(1)</code>
   * is identical to calling the method <code>next()</code> and
   * calling the method <code>relative(-1)</code> is identical
   * to calling the method <code>previous()</code>.
   *
   * @param rows an <code>int</code> specifying the number of rows to
   *             move from the current row; a positive number moves the cursor
   *             forward; a negative number moves the cursor backward
   * @return <code>true</code> if the cursor is on a row;
   * <code>false</code> otherwise
   * @throws Exception if a access error occurs;
   *                   //@throws FeatureNotSupportedException if the driver does not support this method
   */

  public boolean relative(int rows) {

    if (rows > 0) {
      if (this.logicalRowIndex + rows > this.lastPhysicalRowNum) {
        this.logicalRowIndex = this.lastPhysicalRowNum + 1;
      } else {
        this.logicalRowIndex += rows;
      }
    } else if (rows < 0) {
      if (this.logicalRowIndex - Math.abs(rows) < this.firstPhysicalRowNum) {
        this.logicalRowIndex = this.firstPhysicalRowNum - 1;
      } else {
        this.logicalRowIndex -= Math.abs(rows);
      }
    }
    return isCursorInDataSet();

  }

  /**
   * Moves the cursor to the previous row in this
   * <code>DataSet</code> object.
   * <p/>
   * When a call to the <code>previous</code> method returns <code>false</code>,
   * the cursor is positioned before the first row.  Any invocation of a
   * <code>DataSet</code> method which requires a current row will result in a
   * <code>DataSetException</code> being thrown.
   * <p/>
   *
   * @return <code>true</code> if the cursor is now positioned on a valid row;
   * <code>false</code> if the cursor is positioned before the first row
   * @throws Exception if a access error occurs;
   */

  public boolean previous() {
    this.logicalRowIndex -= 1;
    return isCursorInDataSet();
  }

  /**
   * Gives a hint as to the direction in which the rows in this
   * <code>ResultSet</code> object will be processed.
   * The initial value is determined by the
   * <code>Statement</code> object
   * that produced this <code>ResultSet</code> object.
   * The fetch direction may be changed at any time.
   *
   * @param direction an <code>int</code> specifying the suggested
   *                  fetch direction; one of <code>ResultSet.FETCH_FORWARD</code>,
   *                  <code>ResultSet.FETCH_REVERSE</code>, or
   *                  <code>ResultSet.FETCH_UNKNOWN</code>
   * @throws SQLException if a database access error occurs; this
   *                      method is called on a closed result set or
   *                      the result set type is <code>TYPE_FORWARD_ONLY</code> and the fetch
   *                      direction is not <code>FETCH_FORWARD</code>
   * @see Statement#setFetchDirection
   * @see #getFetchDirection
   * @since 1.2
   */
  @Override
  public void setFetchDirection(int direction) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the fetch direction for this
   * <code>ResultSet</code> object.
   *
   * @return the current fetch direction for this <code>ResultSet</code> object
   * @throws SQLException if a database access error occurs
   *                      or this method is called on a closed result set
   * @see #setFetchDirection
   * @since 1.2
   */
  @Override
  public int getFetchDirection() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Gives the JDBC driver a hint as to the number of rows that should
   * be fetched from the database when more rows are needed for this
   * <code>ResultSet</code> object.
   * If the fetch size specified is zero, the JDBC driver
   * ignores the value and is free to make its own best guess as to what
   * the fetch size should be.  The default value is set by the
   * <code>Statement</code> object
   * that created the result set.  The fetch size may be changed at any time.
   *
   * @param rows the number of rows to fetch
   * @throws SQLException if a database access error occurs; this method
   *                      is called on a closed result set or the
   *                      condition {@code rows >= 0} is not satisfied
   * @see #getFetchSize
   * @since 1.2
   */
  @Override
  public void setFetchSize(int rows) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the fetch size for this
   * <code>ResultSet</code> object.
   *
   * @return the current fetch size for this <code>ResultSet</code> object
   * @throws SQLException if a database access error occurs
   *                      or this method is called on a closed result set
   * @see #setFetchSize
   * @since 1.2
   */
  @Override
  public int getFetchSize() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the type of this <code>ResultSet</code> object.
   * The type is determined by the <code>Statement</code> object
   * that created the result set.
   *
   * @return <code>ResultSet.TYPE_FORWARD_ONLY</code>,
   * <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>,
   * or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
   * @throws SQLException if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public int getType() throws SQLException {
    return ResultSet.TYPE_SCROLL_SENSITIVE;
  }

  /**
   * @return the number of rows in the set
   * @throws Exception
   */
  int size() {
    // this.sheet.getPhysicalNumberOfRows();
    return this.lastPhysicalRowNum - this.firstPhysicalRowNum + 1;
  }

  /**
   * Moves the cursor to the last row in
   * this <code>DataSet</code> object.
   *
   * @return <code>true</code> if the cursor is on a valid row;
   * <code>false</code> if there are no rows in the result set
   * @throws Exception if an error occurs;
   *                   //@throws FeatureNotSupportedException if the driver does not support this method
   * @since 1.2
   */

  public boolean last() {
    if (this.dataSetIsEmpty) {
      return false;
    } else {
      this.logicalRowIndex = this.lastPhysicalRowNum;
      return true;
    }
  }


  /**
   * Reports whether
   * the last column read had a value of SQL <code>NULL</code>.
   * Note that you must first call one of the getter methods
   * on a column to try to read its value and then call
   * the method <code>wasNull</code> to see if the value read was
   * SQL <code>NULL</code>.
   *
   * @return <code>true</code> if the last column value read was SQL
   * <code>NULL</code> and <code>false</code> otherwise
   * @throws SQLException if a database access error occurs or this method is
   *                      called on a closed result set
   */
  @Override
  public boolean wasNull() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  /**
   * Moves the cursor to the end of
   * this <code>DataSet</code> object, just after the
   * last row. This method has no effect if the result set contains no rows.
   *
   * @throws Exception if a access error occurs;
   *                   //@throws FeatureNotSupportedException if the implementation does not support this method
   */

  public void afterLast() {
    if (!this.dataSetIsEmpty) {
      this.logicalRowIndex = this.lastPhysicalRowNum + 1;
    }
  }


  //TODO: Must be in the update row to be able to add data
  //Old code that shows how to add data in the sheet
//    public void addRowData(List<DataPoint> rowData) throws DataSetException {
//
//        // Create Row
//        Row excelRow;
//        if (null == this.lastPhysicalRowNum) {
//            excelRow = this.sheet.createRow(this.firstPhysicalRowNum);
//            this.lastPhysicalRowNum = this.firstPhysicalRowNum;
//        } else {
//            excelRow = this.sheet.createRow(this.lastPhysicalRowNum + 1);
//            this.lastPhysicalRowNum++;
//        }
//
//        // Insert Data
//        Iterator<DataPoint> iterator = rowData.iterator();
//        int i = 0;
//        while (iterator.hasNext()) {
//            DataPoint datapoint = iterator.next();
//            Cell dataCell = excelRow.createCell(i);
//            i++;
//            if (datapoint.getDataType() == DataTypes.NUMERIC) {
//                if (!(datapoint.getNumeric() == null)) {
//                    dataCell.setCellValue(datapoint.getNumeric());
//                }
//            } else if (datapoint.getDataType() == DataTypes.STRING) {
//                if (!(datapoint.getString() == null)) {
//                    dataCell.setCellValue(datapoint.getString());
//                }
//            } else if (datapoint.getDataType() == DataTypes.BOOLEAN) {
//                if (!(datapoint.getBoolean() == null)) {
//                    dataCell.setCellValue(datapoint.getBoolean());
//                }
//            } else if (datapoint.getDataType() == DataTypes.DATETIME) {
//                if (!(datapoint.getDateTime() == null)) {
//                    dataCell.setCellValue(datapoint.getDateTime());
//                    CellStyle cellstyle = this.wb.createCellStyle();
//                    DataFormat df = this.wb.createDataFormat();
//                    cellstyle.setDataFormat(df.getFormat(this.defaultFormatString));
//                    dataCell.setCellStyle(cellstyle);
//                }
//            } else {
//                dataCell.setCellValue("The data type (" + datapoint.getDataType() + ":" + DataTypes.getTypeName(datapoint.getDataType()) + " is not supported in Excel");
//            }
//        }
//
//    }


  /**
   * Retrieves the concurrency mode of this <code>ResultSet</code> object.
   * The concurrency used is determined by the
   * <code>Statement</code> object that created the result set.
   *
   * @return the concurrency type, either
   * <code>ResultSet.CONCUR_READ_ONLY</code>
   * or <code>ResultSet.CONCUR_UPDATABLE</code>
   * @throws SQLException if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public int getConcurrency() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves whether the current row has been updated.  The value returned
   * depends on whether or not the result set can detect updates.
   * <p>
   * <strong>Note:</strong> Support for the <code>rowUpdated</code> method is optional with a result set
   * concurrency of <code>CONCUR_READ_ONLY</code>
   *
   * @return <code>true</code> if the current row is detected to
   * have been visibly updated by the owner or another; <code>false</code> otherwise
   * @throws SQLException                    if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @see DatabaseMetaData#updatesAreDetected
   * @since 1.2
   */
  @Override
  public boolean rowUpdated() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves whether the current row has had an insertion.
   * The value returned depends on whether or not this
   * <code>ResultSet</code> object can detect visible inserts.
   * <p>
   * <strong>Note:</strong> Support for the <code>rowInserted</code> method is optional with a result set
   * concurrency of <code>CONCUR_READ_ONLY</code>
   *
   * @return <code>true</code> if the current row is detected to
   * have been inserted; <code>false</code> otherwise
   * @throws SQLException                    if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @see DatabaseMetaData#insertsAreDetected
   * @since 1.2
   */
  @Override
  public boolean rowInserted() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves whether a row has been deleted.  A deleted row may leave
   * a visible "hole" in a result set.  This method can be used to
   * detect holes in a result set.  The value returned depends on whether
   * or not this <code>ResultSet</code> object can detect deletions.
   * <p>
   * <strong>Note:</strong> Support for the <code>rowDeleted</code> method is optional with a result set
   * concurrency of <code>CONCUR_READ_ONLY</code>
   *
   * @return <code>true</code> if the current row is detected to
   * have been deleted by the owner or another; <code>false</code> otherwise
   * @throws SQLException                    if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @see DatabaseMetaData#deletesAreDetected
   * @since 1.2
   */
  @Override
  public boolean rowDeleted() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>null</code> value.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code>
   * or <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateNull(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>boolean</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>byte</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateByte(int columnIndex, byte x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>short</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateShort(int columnIndex, short x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an <code>int</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateInt(int columnIndex, int x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>long</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateLong(int columnIndex, long x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>float</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateFloat(int columnIndex, float x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>double</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateDouble(int columnIndex, double x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.math.BigDecimal</code>
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>String</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateString(int columnIndex, String x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>byte</code> array value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Date</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Time</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateTime(int columnIndex, Time x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Timestamp</code>
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an <code>Object</code> value.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * If the second argument is an <code>InputStream</code> then the stream must contain
   * the number of bytes specified by scaleOrLength.  If the second argument is a
   * <code>Reader</code> then the reader must contain the number of characters specified
   * by scaleOrLength. If these conditions are not true the driver will generate a
   * <code>SQLException</code> when the statement is executed.
   *
   * @param columnIndex   the first column is 1, the second is 2, ...
   * @param x             the new column value
   * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> ,
   *                      this is the number of digits after the decimal point. For
   *                      Java Object types <code>InputStream</code> and <code>Reader</code>,
   *                      this is the length
   *                      of the data in the stream or reader.  For all other types,
   *                      this value will be ignored.
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an <code>Object</code> value.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateObject(int columnIndex, Object x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>null</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateNull(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>boolean</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBoolean(String columnLabel, boolean x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>byte</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateByte(String columnLabel, byte x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>short</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateShort(String columnLabel, short x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an <code>int</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateInt(String columnLabel, int x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>long</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateLong(String columnLabel, long x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>float </code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateFloat(String columnLabel, float x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>double</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateDouble(String columnLabel, double x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.BigDecimal</code>
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>String</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateString(String columnLabel, String x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a byte array value.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code>
   * or <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBytes(String columnLabel, byte[] x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Date</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateDate(String columnLabel, java.sql.Date x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Time</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateTime(String columnLabel, Time x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Timestamp</code>
   * value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      the <code>java.io.Reader</code> object containing
   *                    the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an <code>Object</code> value.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * If the second argument is an <code>InputStream</code> then the stream must contain
   * the number of bytes specified by scaleOrLength.  If the second argument is a
   * <code>Reader</code> then the reader must contain the number of characters specified
   * by scaleOrLength. If these conditions are not true the driver will generate a
   * <code>SQLException</code> when the statement is executed.
   *
   * @param columnLabel   the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x             the new column value
   * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> ,
   *                      this is the number of digits after the decimal point. For
   *                      Java Object types <code>InputStream</code> and <code>Reader</code>,
   *                      this is the length
   *                      of the data in the stream or reader.  For all other types,
   *                      this value will be ignored.
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an <code>Object</code> value.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateObject(String columnLabel, Object x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Inserts the contents of the insert row into this
   * <code>ResultSet</code> object and into the database.
   * The cursor must be on the insert row when this method is called.
   *
   * @throws SQLException                    if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>,
   *                                         this method is called on a closed result set,
   *                                         if this method is called when the cursor is not on the insert row,
   *                                         or if not all of non-nullable columns in
   *                                         the insert row have been given a non-null value
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void insertRow() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the underlying database with the new contents of the
   * current row of this <code>ResultSet</code> object.
   * This method cannot be called when the cursor is on the insert row.
   *
   * @throws SQLException                    if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>;
   *                                         this method is called on a closed result set or
   *                                         if this method is called when the cursor is on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void updateRow() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Deletes the current row from this <code>ResultSet</code> object
   * and from the underlying database.  This method cannot be called when
   * the cursor is on the insert row.
   *
   * @throws SQLException                    if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>;
   *                                         this method is called on a closed result set
   *                                         or if this method is called when the cursor is on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void deleteRow() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Refreshes the current row with its most recent value in
   * the database.  This method cannot be called when
   * the cursor is on the insert row.
   * <p>
   * <P>The <code>refreshRow</code> method provides a way for an
   * application to
   * explicitly tell the JDBC driver to refetch a row(s) from the
   * database.  An application may want to call <code>refreshRow</code> when
   * caching or prefetching is being done by the JDBC driver to
   * fetch the latest value of a row from the database.  The JDBC driver
   * may actually refresh multiple rows at once if the fetch size is
   * greater than one.
   * <p>
   * <P> All values are refetched subject to the transaction isolation
   * level and cursor sensitivity.  If <code>refreshRow</code> is called after
   * calling an updater method, but before calling
   * the method <code>updateRow</code>, then the
   * updates made to the row are lost.  Calling the method
   * <code>refreshRow</code> frequently will likely slow performance.
   *
   * @throws SQLException                    if a database access error
   *                                         occurs; this method is called on a closed result set;
   *                                         the result set type is <code>TYPE_FORWARD_ONLY</code> or if this
   *                                         method is called when the cursor is on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method or this method is not supported for the specified result
   *                                         set type and result set concurrency.
   * @since 1.2
   */
  @Override
  public void refreshRow() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Cancels the updates made to the current row in this
   * <code>ResultSet</code> object.
   * This method may be called after calling an
   * updater method(s) and before calling
   * the method <code>updateRow</code> to roll back
   * the updates made to a row.  If no updates have been made or
   * <code>updateRow</code> has already been called, this method has no
   * effect.
   *
   * @throws SQLException                    if a database access error
   *                                         occurs; this method is called on a closed result set;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or if this method is called when the cursor is
   *                                         on the insert row
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void cancelRowUpdates() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Moves the cursor to the insert row.  The current cursor position is
   * remembered while the cursor is positioned on the insert row.
   * <p>
   * The insert row is a special row associated with an updatable
   * result set.  It is essentially a buffer where a new row may
   * be constructed by calling the updater methods prior to
   * inserting the row into the result set.
   * <p>
   * Only the updater, getter,
   * and <code>insertRow</code> methods may be
   * called when the cursor is on the insert row.  All of the columns in
   * a result set must be given a value each time this method is
   * called before calling <code>insertRow</code>.
   * An updater method must be called before a
   * getter method can be called on a column value.
   *
   * @throws SQLException                    if a database access error occurs; this
   *                                         method is called on a closed result set
   *                                         or the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void moveToInsertRow() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Moves the cursor to the remembered cursor position, usually the
   * current row.  This method has no effect if the cursor is not on
   * the insert row.
   *
   * @throws SQLException                    if a database access error occurs; this
   *                                         method is called on a closed result set
   *                                         or the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public void moveToCurrentRow() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the <code>Statement</code> object that produced this
   * <code>ResultSet</code> object.
   * If the result set was generated some other way, such as by a
   * <code>DatabaseMetaData</code> method, this method  may return
   * <code>null</code>.
   *
   * @return the <code>Statement</code> object that produced
   * this <code>ResultSet</code> object or <code>null</code>
   * if the result set was produced some other way
   * @throws SQLException if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public Statement getStatement() throws SQLException {
    throw new RuntimeException("This Excel Result Set has no statement");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as an <code>Object</code>
   * in the Java programming language.
   * If the value is an SQL <code>NULL</code>,
   * the driver returns a Java <code>null</code>.
   * This method uses the given <code>Map</code> object
   * for the custom mapping of the
   * SQL structured or distinct type that is being retrieved.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param map         a <code>java.util.Map</code> object that contains the mapping
   *                    from SQL type names to classes in the Java programming language
   * @return an <code>Object</code> in the Java programming language
   * representing the SQL value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>Ref</code> object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>Ref</code> object representing an SQL <code>REF</code>
   * value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Ref getRef(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>Blob</code> object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>Blob</code> object representing the SQL
   * <code>BLOB</code> value in the specified column
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Blob getBlob(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>Clob</code> object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>Clob</code> object representing the SQL
   * <code>CLOB</code> value in the specified column
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Clob getClob(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as an <code>Array</code> object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return an <code>Array</code> object representing the SQL
   * <code>ARRAY</code> value in the specified column
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Array getArray(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as an <code>Object</code>
   * in the Java programming language.
   * If the value is an SQL <code>NULL</code>,
   * the driver returns a Java <code>null</code>.
   * This method uses the specified <code>Map</code> object for
   * custom mapping if appropriate.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param map         a <code>java.util.Map</code> object that contains the mapping
   *                    from SQL type names to classes in the Java programming language
   * @return an <code>Object</code> representing the SQL value in the
   * specified column
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>Ref</code> object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>Ref</code> object representing the SQL <code>REF</code>
   * value in the specified column
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Ref getRef(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>Blob</code> object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>Blob</code> object representing the SQL <code>BLOB</code>
   * value in the specified column
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Blob getBlob(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>Clob</code> object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>Clob</code> object representing the SQL <code>CLOB</code>
   * value in the specified column
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Clob getClob(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as an <code>Array</code> object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return an <code>Array</code> object representing the SQL <code>ARRAY</code> value in
   * the specified column
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.2
   */
  @Override
  public Array getArray(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the date if the underlying database does not store
   * timezone information.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param cal         the <code>java.util.Calendar</code> object
   *                    to use in constructing the date
   * @return the column value as a <code>java.sql.Date</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the date if the underlying database does not store
   * timezone information.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param cal         the <code>java.util.Calendar</code> object
   *                    to use in constructing the date
   * @return the column value as a <code>java.sql.Date</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public java.sql.Date getDate(String columnLabel, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the time if the underlying database does not store
   * timezone information.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param cal         the <code>java.util.Calendar</code> object
   *                    to use in constructing the time
   * @return the column value as a <code>java.sql.Time</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public Time getTime(int columnIndex, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the time if the underlying database does not store
   * timezone information.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param cal         the <code>java.util.Calendar</code> object
   *                    to use in constructing the time
   * @return the column value as a <code>java.sql.Time</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException if the columnLabel is not valid;
   *                      if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public Time getTime(String columnLabel, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the timestamp if the underlying database does not store
   * timezone information.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param cal         the <code>java.util.Calendar</code> object
   *                    to use in constructing the timestamp
   * @return the column value as a <code>java.sql.Timestamp</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException if the columnIndex is not valid;
   *                      if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
   * in the Java programming language.
   * This method uses the given calendar to construct an appropriate millisecond
   * value for the timestamp if the underlying database does not store
   * timezone information.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param cal         the <code>java.util.Calendar</code> object
   *                    to use in constructing the date
   * @return the column value as a <code>java.sql.Timestamp</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException if the columnLabel is not valid or
   *                      if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.2
   */
  @Override
  public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.net.URL</code>
   * object in the Java programming language.
   *
   * @param columnIndex the index of the column 1 is the first, 2 is the second,...
   * @return the column value as a <code>java.net.URL</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs; this method
   *                                         is called on a closed result set or if a URL is malformed
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public URL getURL(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>java.net.URL</code>
   * object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value as a <code>java.net.URL</code> object;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>null</code> in the Java programming language
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs; this method
   *                                         is called on a closed result set or if a URL is malformed
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public URL getURL(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Ref</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateRef(int columnIndex, Ref x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Ref</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateRef(String columnLabel, Ref x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Blob</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateBlob(int columnIndex, Blob x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Blob</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateBlob(String columnLabel, Blob x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Clob</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateClob(int columnIndex, Clob x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Clob</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateClob(String columnLabel, Clob x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Array</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateArray(int columnIndex, Array x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.Array</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.4
   */
  @Override
  public void updateArray(String columnLabel, Array x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row of this
   * <code>ResultSet</code> object as a <code>java.sql.RowId</code> object in the Java
   * programming language.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @return the column value; if the value is a SQL <code>NULL</code> the
   * value returned is <code>null</code>
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public RowId getRowId(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row of this
   * <code>ResultSet</code> object as a <code>java.sql.RowId</code> object in the Java
   * programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value ; if the value is a SQL <code>NULL</code> the
   * value returned is <code>null</code>
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public RowId getRowId(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>RowId</code> value. The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the <code>updateRow</code> or <code>insertRow</code> methods are called
   * to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param x           the column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>RowId</code> value. The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the <code>updateRow</code> or <code>insertRow</code> methods are called
   * to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the holdability of this <code>ResultSet</code> object
   *
   * @return either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
   * @throws SQLException if a database access error occurs
   *                      or this method is called on a closed result set
   * @since 1.6
   */
  @Override
  public int getHoldability() throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  /**
   * Updates the designated column with a <code>String</code> value.
   * It is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param nString     the value for the column to be updated
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNString(int columnIndex, String nString) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>String</code> value.
   * It is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param nString     the value for the column to be updated
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set;
   *                                         the result set concurrency is <CODE>CONCUR_READ_ONLY</code>
   *                                         or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNString(String columnLabel, String nString) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.NClob</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param nClob       the value for the column to be updated
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set;
   *                                         if a database access error occurs or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.NClob</code> value.
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param nClob       the value for the column to be updated
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set;
   *                                         if a database access error occurs or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>NClob</code> object
   * in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>NClob</code> object representing the SQL
   * <code>NCLOB</code> value in the specified column
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set
   *                                         or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public NClob getNClob(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a <code>NClob</code> object
   * in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>NClob</code> object representing the SQL <code>NCLOB</code>
   * value in the specified column
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set
   *                                         or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public NClob getNClob(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in  the current row of
   * this <code>ResultSet</code> as a
   * <code>java.sql.SQLXML</code> object in the Java programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in  the current row of
   * this <code>ResultSet</code> as a
   * <code>java.sql.SQLXML</code> object in the Java programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.SQLXML</code> value.
   * The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the <code>updateRow</code> or <code>insertRow</code> methods are called
   * to update the database.
   * <p>
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param xmlObject   the value for the column to be updated
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs; this method
   *                                         is called on a closed result set;
   *                                         the <code>java.xml.transform.Result</code>,
   *                                         <code>Writer</code> or <code>OutputStream</code> has not been closed
   *                                         for the <code>SQLXML</code> object;
   *                                         if there is an error processing the XML value or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>.  The <code>getCause</code> method
   *                                         of the exception may provide a more detailed exception, for example, if the
   *                                         stream does not contain valid XML.
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a <code>java.sql.SQLXML</code> value.
   * The updater
   * methods are used to update column values in the current row or the insert
   * row. The updater methods do not update the underlying database; instead
   * the <code>updateRow</code> or <code>insertRow</code> methods are called
   * to update the database.
   * <p>
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param xmlObject   the column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs; this method
   *                                         is called on a closed result set;
   *                                         the <code>java.xml.transform.Result</code>,
   *                                         <code>Writer</code> or <code>OutputStream</code> has not been closed
   *                                         for the <code>SQLXML</code> object;
   *                                         if there is an error processing the XML value or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>.  The <code>getCause</code> method
   *                                         of the exception may provide a more detailed exception, for example, if the
   *                                         stream does not contain valid XML.
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Java programming language.
   * It is intended for use when
   * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public String getNString(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Java programming language.
   * It is intended for use when
   * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>null</code>
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public String getNString(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.io.Reader</code> object.
   * It is intended for use when
   * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return a <code>java.io.Reader</code> object that contains the column
   * value; if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language.
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>java.io.Reader</code> object.
   * It is intended for use when
   * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return a <code>java.io.Reader</code> object that contains the column
   * value; if the value is SQL <code>NULL</code>, the value returned is
   * <code>null</code> in the Java programming language
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.   The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.  The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      the <code>java.io.Reader</code> object containing
   *                    the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an ascii stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a binary stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value, which will have
   * the specified number of bytes.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      the <code>java.io.Reader</code> object containing
   *                    the new column value
   * @param length      the length of the stream
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given input stream, which
   * will have the specified number of bytes.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param inputStream An object that contains the data to set the parameter
   *                    value to.
   * @param length      the number of bytes in the parameter data.
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given input stream, which
   * will have the specified number of bytes.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param inputStream An object that contains the data to set the parameter
   *                    value to.
   * @param length      the number of bytes in the parameter data.
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.Reader</code> object. The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param reader      An object that contains the data to set the parameter value to.
   * @param length      the number of characters in the parameter data.
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.Reader</code> object.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      An object that contains the data to set the parameter value to.
   * @param length      the number of characters in the parameter data.
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.Reader</code> object. The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param reader      An object that contains the data to set the parameter value to.
   * @param length      the number of characters in the parameter data.
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set,
   *                                         if a database access error occurs or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * object, which is the given number of characters long.
   * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.Reader</code> object. The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      An object that contains the data to set the parameter value to.
   * @param length      the number of characters in the parameter data.
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set;
   *                                         if a database access error occurs or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateNCharacterStream</code> which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * It is intended for use when
   * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
   * and <code>LONGNVARCHAR</code> columns.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateNCharacterStream</code> which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      the <code>java.io.Reader</code> object containing
   *                    the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an ascii stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateAsciiStream</code> which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a binary stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateBinaryStream</code> which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateCharacterStream</code> which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param x           the new column value
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with an ascii stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateAsciiStream</code> which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a binary stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateBinaryStream</code> which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param x           the new column value
   * @throws SQLException                    if the columnLabel is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column with a character stream value.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateCharacterStream</code> which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      the <code>java.io.Reader</code> object containing
   *                    the new column value
   * @throws SQLException                    if the columnLabel is not valid; if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given input stream. The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateBlob</code> which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param inputStream An object that contains the data to set the parameter
   *                    value to.
   * @throws SQLException                    if the columnIndex is not valid; if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given input stream. The data will be read from the stream
   * as needed until end-of-stream is reached.
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateBlob</code> which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param inputStream An object that contains the data to set the parameter
   *                    value to.
   * @throws SQLException                    if the columnLabel is not valid; if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * object.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateClob</code> which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param reader      An object that contains the data to set the parameter value to.
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * object.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateClob</code> which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      An object that contains the data to set the parameter value to.
   * @throws SQLException                    if the columnLabel is not valid; if a database access error occurs;
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   *                                         or this method is called on a closed result set
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * <p>
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateNClob</code> which takes a length parameter.
   *
   * @param columnIndex the first column is 1, the second 2, ...
   * @param reader      An object that contains the data to set the parameter value to.
   * @throws SQLException                    if the columnIndex is not valid;
   *                                         if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set,
   *                                         if a database access error occurs or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Updates the designated column using the given <code>Reader</code>
   * object.
   * The data will be read from the stream
   * as needed until end-of-stream is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   * <p>
   * <p>
   * The updater methods are used to update column values in the
   * current row or the insert row.  The updater methods do not
   * update the underlying database; instead the <code>updateRow</code> or
   * <code>insertRow</code> methods are called to update the database.
   * <p>
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>updateNClob</code> which takes a length parameter.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @param reader      An object that contains the data to set the parameter value to.
   * @throws SQLException                    if the columnLabel is not valid; if the driver does not support national
   *                                         character sets;  if the driver can detect that a data conversion
   *                                         error could occur; this method is called on a closed result set;
   *                                         if a database access error occurs or
   *                                         the result set concurrency is <code>CONCUR_READ_ONLY</code>
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.6
   */
  @Override
  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * <p>Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object and will convert from the
   * SQL type of the column to the requested Java data type, if the
   * conversion is supported. If the conversion is not
   * supported  or null is specified for the type, a
   * <code>SQLException</code> is thrown.
   * <p>
   * At a minimum, an implementation must support the conversions defined in
   * Appendix B, Table B-3 and conversion of appropriate user defined SQL
   * types to a Java type which implements {@code SQLData}, or {@code Struct}.
   * Additional conversions may be supported and are vendor defined.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @param type        Class representing the Java data type to convert the designated
   *                    column to.
   * @return an instance of {@code type} holding the column value
   * @throws SQLException                    if conversion is not supported, type is null or
   *                                         another error occurs. The getCause() method of the
   *                                         exception may provide a more detailed exception, for example, if
   *                                         a conversion error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.7
   */
  @Override
  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * <p>Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object and will convert from the
   * SQL type of the column to the requested Java data type, if the
   * conversion is supported. If the conversion is not
   * supported  or null is specified for the type, a
   * <code>SQLException</code> is thrown.
   * <p>
   * At a minimum, an implementation must support the conversions defined in
   * Appendix B, Table B-3 and conversion of appropriate user defined SQL
   * types to a Java type which implements {@code SQLData}, or {@code Struct}.
   * Additional conversions may be supported and are vendor defined.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.
   *                    If the SQL AS clause was not specified, then the label is the name
   *                    of the column
   * @param type        Class representing the Java data type to convert the designated
   *                    column to.
   * @return an instance of {@code type} holding the column value
   * @throws SQLException                    if conversion is not supported, type is null or
   *                                         another error occurs. The getCause() method of the
   *                                         exception may provide a more detailed exception, for example, if
   *                                         a conversion error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   *                                         this method
   * @since 1.7
   */
  @Override
  public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  // Header and column data type
  private TreeBidiMap<String, Integer> headerNames;
  private Map<Integer, Cell> headerCells = new HashMap<>();


  Integer getLastRowNum() {
    return this.sheet.getLastRowNum();
  }

  Short getLastColNum() {
    Row row = this.sheet.getRow(0);
    if (row == null) {
      return null;
    }
    return row.getLastCellNum();
  }

  /**
   * An utility to get a string representation of the Excel cell type
   *
   * @param cellType
   * @return
   */
  private String getCellTypeName(CellType cellType) {
    return cellType.toString();
  }


  /*
   * Return the Row Index of the Sheet
   * from the logical rowIndex
   */
  private int getExcelRowIndex(int rowIndex) {
    // The Excel Api begins the col and row to 0
    return rowIndex - 1;
  }

  /* Return the Column Index of the Sheet
   * @param columnIndex is the column index of the data set
   */
  private int getExcelColumnIndex(int columnIndex) {
    // The Excel Api begins the col and row to 0
    return columnIndex - 1;
  }

  /**
   * Get a string from a cell
   */
  public String getString(int rowIndex, int columnIndex) {


    return this.cast(rowIndex, columnIndex, String.class);

  }

  /**
   * Return the value of a cell
   *
   * @param rowIndex    - the logical row asked
   * @param columnIndex - the logical column asked
   * @param clazz       - the class to cast to
   * @param <T>         - the type
   * @return the value cast
   */
  private <T> T cast(Integer rowIndex, int columnIndex, Class<T> clazz) {


    return ExcelSheets.getCellValue(getCell(rowIndex, columnIndex), clazz);

  }


  public void close() {

    this.excelSheet.close();

  }

  // Not in the resultSetMetadata interface
  // Is used to go from the name to the index and vice versa
  // The logical way is to think in index number for row and col
  int getColumnIndex(String columnName) {
    if (headerNames == null) {
      throw new RuntimeException("This data set has no header defined and can then not search a column index from a column name");
    } else {
      Integer columnIndex = this.headerNames.get(columnName);
      if (columnIndex == null) {
        throw new RuntimeException("The header column label (" + columnName + ") does not exist");
      } else {
        return columnIndex;
      }
    }

  }

  public boolean isClosed() {
    return this.excelSheet.isClosed();
  }

  /**
   * Returns an object that implements the given interface to allow access to
   * non-standard methods, or standard methods not exposed by the proxy.
   * <p>
   * If the receiver implements the interface then the result is the receiver
   * or a proxy for the receiver. If the receiver is a wrapper
   * and the wrapped object implements the interface then the result is the
   * wrapped object or a proxy for the wrapped object. Otherwise return the
   * the result of calling <code>unwrap</code> recursively on the wrapped object
   * or a proxy for that result. If the receiver is not a
   * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
   *
   * @param iface A Class defining an interface that the result must implement.
   * @return an object that implements the interface. May be a proxy for the actual implementing object.
   * @since 1.6
   */
  @Override
  public <T> T unwrap(Class<T> iface) {
    return null;
  }

  /**
   * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
   * for an object that does. Returns false otherwise. If this implements the interface then return true,
   * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
   * object. If this does not implement the interface and is not a wrapper, return false.
   * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
   * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
   * returns true then calling <code>unwrap</code> with the same argument should succeed.
   *
   * @param iface a Class defining an interface.
   * @return true if this implements the interface or directly or indirectly wraps an object that does.
   * @throws SQLException if an error occurs while determining whether this is a wrapper
   *                      for an object with the given interface.
   * @since 1.6
   */
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  public TreeBidiMap<String, Integer> getHeaderNames() {
    return this.headerNames;
  }

  public Map<Integer, Cell> getColumnTypes() {
    return headerCells;
  }


  class ExcelMeta implements ResultSetMetaData {

    /**
     * Returns the number of columns in this <code>ResultSet</code> object.
     *
     * @return the number of columns
     */
    public int getColumnCount() {

      return headerNames.size();

    }

    /**
     * Indicates whether the designated column is automatically numbered.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     */

    public boolean isAutoIncrement(int column) throws SQLException {
      return false;
    }

    /**
     * Indicates whether a column's case matters.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     **/
    public boolean isCaseSensitive(int column) throws SQLException {
      return false;
    }

    /**
     * Indicates whether the designated column can be used in a where clause.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isSearchable(int column) throws SQLException {
      return false;
    }

    /**
     * Indicates whether the designated column is a cash value.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isCurrency(int column) throws SQLException {
      return false;
    }

    /**
     * Indicates the nullability of values in the designated column.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the nullability status of the given column; one of <code>columnNoNulls</code>,
     * <code>columnNullable</code> or <code>columnNullableUnknown</code>
     * @throws SQLException if a database access error occurs
     */
    public int isNullable(int column) throws SQLException {
      return 0;
    }

    /**
     * Indicates whether values in the designated column are signed numbers.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isSigned(int column) throws SQLException {
      return false;
    }

    /**
     * Indicates the designated column's normal maximum width in characters.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the normal maximum number of characters allowed as the width
     * of the designated column
     * @throws SQLException if a database access error occurs
     */
    public int getColumnDisplaySize(int column) throws SQLException {
      return 0;
    }

    /**
     * Gets the designated column's suggested title for use in printouts and
     * displays. The suggested title is usually specified by the SQL <code>AS</code>
     * clause.  If a SQL <code>AS</code> is not specified, the value returned from
     * <code>getColumnLabel</code> will be the same as the value returned by the
     * <code>getColumnName</code> method.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the suggested column title
     * @throws SQLException if a database access error occurs
     **/
    public String getColumnLabel(int column) throws SQLException {
      return null;
    }

    /**
     * Get the designated column's name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return column name
     * @throws SQLException if a database access error occurs
     */
    public String getColumnName(int column) throws SQLException {

      if (headerNames == null) {
        return String.valueOf(column);
      } else {
        String columnName = headerNames.inverseBidiMap().get(column);
        if (columnName == null) {
          throw new SQLException("The column index (" + column + ") doesn't exist");
        }
        return columnName;
      }

    }


    /**
     * Get the designated column's table's schema.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return schema name or "" if not applicable
     * @throws SQLException if a database access error occurs
     */
    public String getSchemaName(int column) throws SQLException {
      return null;
    }

    /**
     * Get the designated column's specified column size.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. 0 is returned for data types where the
     * column size is not applicable.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return precision
     * @throws SQLException if a database access error occurs
     */
    public int getPrecision(int column) throws SQLException {
      return 0;
    }

    /**
     * Gets the designated column's number of digits to right of the decimal point.
     * 0 is returned for data types where the scale is not applicable.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return scale
     * @throws SQLException if a database access error occurs
     */
    public int getScale(int column) throws SQLException {
      return 0;
    }

    /**
     * Gets the designated column's table name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return table name or "" if not applicable
     * @throws SQLException if a database access error occurs
     */
    public String getTableName(int column) throws SQLException {
      return null;
    }

    /**
     * Gets the designated column's table's catalog name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the name of the catalog for the table in which the given column
     * appears or "" if not applicable
     * @throws SQLException if a database access error occurs
     */
    public String getCatalogName(int column) throws SQLException {
      return null;
    }

    /**
     * Retrieves the designated column's SQL type.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return SQL type from java.sql.Types
     * @throws SQLException if a database access error occurs
     * @see Types
     */
    public int getColumnType(int column) throws SQLException {
      if (column < 1) {
        throw new SQLException("The column index can not be 0 or less. The column index passed was (" + column + ")");
      }
      if (column > headerCells.size()) {
        throw new SQLException("The column index can not be greater than the number of column (" + headerCells.size() + "). The column index passed was (" + column + ")");
      }
      return ExcelSheets.toSqlType(headerCells.get(column));
    }


    /**
     * Retrieves the designated column's database-specific type name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return type name used by the database. If the column type is
     * a user-defined type, then a fully-qualified type name is returned.
     * @throws SQLException if a database access error occurs
     */
    public String getColumnTypeName(int column) throws SQLException {
      throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Indicates whether the designated column is definitely not writable.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isReadOnly(int column) throws SQLException {
      return false;
    }

    /**
     * Indicates whether it is possible for a write on the designated column to succeed.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isWritable(int column) throws SQLException {
      return false;
    }

    /**
     * Indicates whether a write on the designated column will definitely succeed.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean isDefinitelyWritable(int column) throws SQLException {
      return false;
    }

    /**
     * <p>Returns the fully-qualified name of the Java class whose instances
     * are manufactured if the method <code>ResultSet.getObject</code>
     * is called to retrieve a value
     * from the column.  <code>ResultSet.getObject</code> may return a subclass of the
     * class returned by this method.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the fully-qualified name of the class in the Java programming
     * language that would be used by the method
     * <code>ResultSet.getObject</code> to retrieve the value in the specified
     * column. This is the class name used for custom mapping.
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    public String getColumnClassName(int column) throws SQLException {
      return null;
    }

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy.
     * <p>
     * If the receiver implements the interface then the result is the receiver
     * or a proxy for the receiver. If the receiver is a wrapper
     * and the wrapped object implements the interface then the result is the
     * wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling <code>unwrap</code> recursively on the wrapped object
     * or a proxy for that result. If the receiver is not a
     * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws SQLException If no object found that implements the interface
     * @since 1.6
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      return null;
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
     * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
     * returns true then calling <code>unwrap</code> with the same argument should succeed.
     *
     * @param iface a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws SQLException if an error occurs while determining whether this is a wrapper
     *                      for an object with the given interface.
     * @since 1.6
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
    }


  }

  private Cell getCell(Integer rowIndex, int columnIndex) {
    if (rowIndex == null) {
      rowIndex = logicalRowIndex;
    }

    validateCell(logicalRowIndex, columnIndex);
    int physicalRowIndex = getPhysicalRowIndex(rowIndex);

    // Mapping of logical location with the sourceResultSet sheet location
    int sheetRowIndex = this.getExcelRowIndex(physicalRowIndex);
    int sheetColumnIndex = this.getExcelColumnIndex(columnIndex);

    return this.sheet.getRow(sheetRowIndex).getCell(sheetColumnIndex);

  }
}
