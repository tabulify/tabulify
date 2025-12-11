package com.tabulify.excel;

import org.apache.poi.openxml4j.opc.PackageAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class ExcelResultSetTest {


  private final ExcelResultSet fileResultSet;


  // The Parameters are passed and initialized via this constructor
  public ExcelResultSetTest() {
    try {
      URL file = ExcelResultSetTest.class.getResource("/excel/testDataSet.xlsx");
      assert file != null;
      Path pathObj = Paths.get(file.toURI());
      ExcelSheet excelDataPath = ExcelSheet.config(pathObj, PackageAccess.READ)
        .setHeaderId(1)
        .build();
      this.fileResultSet = new ExcelResultSet(excelDataPath);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  public void ExcelDataSetSizeIsCorrect() {

    Assertions.assertEquals(6, fileResultSet.size(), "The data set size must be correct");

  }

  @Test
  public void ExcelDataSetGetColumnNameStringWithGoodIndex() throws SQLException {

    Assertions.assertEquals("Personal deliverable", fileResultSet.getMetaData().getColumnName(1), "The column named 'Personal deliverable' is the second one");

  }

  @Test
  public void ExcelDataSetColumnNameNumericWithGoodIndex() throws SQLException {

    Assertions.assertEquals("20", fileResultSet.getMetaData().getColumnName(5));

  }

  @Test()
  public void ExcelDataSetColumnNameWithBigIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getMetaData().getColumnName(1000000),
      "This column name doesn't exist"
    );

  }

  @Test
  public void ExcelDataSetColumnNameWithZeroIndex() {
    // The first column starts to 1
    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getMetaData().getColumnName(0),
      "The column index begins with 1 and not 0"
    );
  }

  @Test()
  public void ExcelDataSetColumnNameWithNegativeIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getMetaData().getColumnName(-1),
      "Negative column index is a non-sense"
    );

  }

  @Test()
  public void ExcelDataSetColumnTypeString() throws SQLException {

    Assertions.assertEquals(Types.VARCHAR, fileResultSet.getMetaData().getColumnType(1), "The first column type must be String");

  }

  @Test()
  public void ExcelDataSetColumnTypeDate() throws SQLException {

    Assertions.assertEquals(Types.TIMESTAMP, fileResultSet.getMetaData().getColumnType(2), "The second column type must be a Date");

  }

  @Test()
  public void ExcelDataSetColumnTypeBoolean() throws SQLException {

    Assertions.assertEquals(Types.BOOLEAN, fileResultSet.getMetaData().getColumnType(4), "The fourth column must be a boolean");

  }

  @Test()
  public void ExcelDataSetColumnTypeDouble() throws SQLException {

    Assertions.assertEquals(Types.DOUBLE, fileResultSet.getMetaData().getColumnType(5), "The fifth column must be a numeric");

  }

  @Test()
  public void ExcelDataSetColumnTypeBadBigIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getMetaData().getColumnType(10000),
      "Bad big index that mus return an data set Exception"
    );

  }

  @Test()
  public void ExcelDataSetColumnTypeBad0Index() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getMetaData().getColumnType(0),
      "The column 0 doesn't exist"
    );

  }

  @Test()
  public void ExcelDataSetColumnTypeBadNegativeIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getMetaData().getColumnType(-1),
      "An negative column doesn't exist"
    );

  }

  @Test()
  public void ExcelDataSetColumnColumnCount() throws SQLException {

    Assertions.assertEquals(5, fileResultSet.getMetaData().getColumnCount(), "There is 5 columns");

  }


  @Test()
  public void ExcelDataSetBeforeFirst() {
    fileResultSet.absolute(7);
    fileResultSet.beforeFirst();
    Assertions.assertEquals(0, fileResultSet.getRow(), "Before first must return the row number 0");
  }

  @Test()
  public void ExcelDataSetAfterLast() {
    fileResultSet.afterLast();
    Assertions.assertEquals(7, fileResultSet.getRow(), "After last must return the row number 7");
  }

  @Test()
  public void ExcelDataSetNextReturnTrue() {
    fileResultSet.beforeFirst();
    Assertions.assertTrue(fileResultSet.next(), "Next must return true after beforeFirst");
  }

  @Test()
  public void ExcelDataSetNextGoodRowNum() {
    fileResultSet.beforeFirst();
    fileResultSet.next();
    Assertions.assertEquals(1, fileResultSet.getRow(), "Next must return true after beforeFirst");
  }

  @Test()
  public void ExcelDataSetNextReturnFalse() {
    fileResultSet.last();
    Assertions.assertFalse(fileResultSet.next(), "Next must return false after last");
  }

  @Test()
  public void ExcelDataSetLastRowNum() {
    fileResultSet.last();
    Assertions.assertEquals(6, fileResultSet.getRow(), "Last must go to the last record (ie 6)");
  }

  @Test()
  public void ExcelDataSetLastReturnTrue() {
    Assertions.assertTrue(fileResultSet.last(), "Last must return True");
  }

  @Test()
  public void ExcelDataSetAbsoluteGood() {
    fileResultSet.absolute(1);
    Assertions.assertEquals(1, fileResultSet.getRow(), "Absolute 1 must send you to the first row");
  }

  @Test()
  public void ExcelDataSetAbsoluteNegativeBadReturnFalse() {
    // There is only 6 rows
    Assertions.assertFalse(fileResultSet.absolute(-10000), "Big Negative Absolute number must return false");
  }

  @Test()
  public void ExcelDataSetAbsoluteNegativeBadRowNum() {
    // There is only 6 rows
    fileResultSet.absolute(-10000);
    Assertions.assertEquals(0, fileResultSet.getRow(), "Big Negative Absolute number must place the cursor to 0");
  }

  @Test()
  public void ExcelDataSetAbsolutePositiveBadReturnFalse() {
    // There is only 6 rows
    Assertions.assertFalse(fileResultSet.absolute(10000), "Big Positive Absolute number must return false");
  }

  @Test()
  public void ExcelDataSetAbsolutePositiveBadRowNum() {
    // There is only 6 rows
    fileResultSet.absolute(10000);
    Assertions.assertEquals(7, fileResultSet.getRow(), "When the index column is above the data set size, it must go one above the last one");
  }

  @Test()
  public void ExcelDataSetAbsoluteNegativeGoodRowNum() {
    // There is 6 rows, -2 means that we are on the 5
    fileResultSet.absolute(-2);
    Assertions.assertEquals(5, fileResultSet.getRow(), "A negative number lower than the data set size must start from the end");
  }

  @Test()
  public void ExcelDataSetAbsoluteNegativeGoodReturnTrue() {
    // There is 6 rows, -2 means that we are om the 5
    Assertions.assertTrue(fileResultSet.absolute(-2), "A good negative absolute must return True");
  }

  @Test()
  public void ExcelDataSetAbsolutePositiveGoodRowNum() {
    // There is 6 rows, -2 means that we are om the 5
    fileResultSet.absolute(2);
    Assertions.assertEquals(2, fileResultSet.getRow(), "The row number must be 2");
  }

  @Test()
  public void ExcelDataSetAbsolutePositiveGoodReturnTrue() {
    // There is 6 rows, -2 means that we are om the 5
    Assertions.assertTrue(fileResultSet.absolute(2));
  }

  @Test()
  public void ExcelDataSetAbsoluteZeroGoodRowNum() {
    fileResultSet.absolute(0);
    Assertions.assertEquals(0, fileResultSet.getRow(), "must be 0");
  }

  @Test()
  public void ExcelDataSetAbsoluteUnGoodRowNum() {
    fileResultSet.absolute(1);
    Assertions.assertTrue(fileResultSet.isFirst(), "must be first");
  }

  @Test()
  public void ExcelDataSetAbsoluteMinOneGoodRowNum() {
    fileResultSet.absolute(-1);
    Assertions.assertEquals(6, fileResultSet.getRow(), "Absolute -1, Get row must be");
    Assertions.assertTrue(fileResultSet.isLast(), "isLast must be true");
  }

  @Test()
  public void ExcelDataSetGetDateGood() throws SQLException {

    fileResultSet.absolute(3);
    Calendar cal = Calendar.getInstance();
    cal.set(2014, Calendar.MAY, 28, 14, 8, 3);
    // We get 808 milliseconds ???, then to 0
    cal.set(Calendar.MILLISECOND, 0);
    Date expected = new Date(cal.getTimeInMillis());
    Date actual = fileResultSet.getDate(2);
    Assertions.assertEquals(expected, actual, "Date are the same");

  }

  @Test()
  public void ExcelDataSetGetDateNull() throws SQLException {

    fileResultSet.absolute(1);
    Assertions.assertNull(fileResultSet.getDate(2), "Date must be null");
  }

  @Test()
  public void ExcelDataSetGetDateBadCellType() {

    fileResultSet.absolute(2);
    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getDate(1),
      "This cell as a string data type. An exception must be thrown."
    );

  }

  @Test()
  public void ExcelDataSetGetDateBigIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getDate(6),
      "This cell index is too big. An exception must be thrown."
    );

  }

  @Test()
  public void ExcelDataSetGetDateNegativeIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getDate(-1),
      "This cell index is negative. An exception must be thrown."
    );

  }

  @Test()
  public void ExcelDataSetGetDateZeroIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getDate(0),
      "This cell index is 0. An exception must be thrown."
    );

  }

  @Test()
  public void ExcelDataSetGetDateFromBadRowNum() {

    fileResultSet.beforeFirst();
    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getDate(2),
      "The cursor is not on a valid row in the data set. An exception must be thrown."
    );

  }

  @Test()
  public void ExcelDataSetGetDoubleGood() throws SQLException {

    this.fileResultSet.beforeFirst();
    this.fileResultSet.next();
    Assertions.assertEquals((Double) 4.0, fileResultSet.getNumeric(5), "The number must be 4");

  }

  @Test()
  public void ExcelDataSetGetDoubleNull() throws SQLException {

    this.fileResultSet.absolute(4);
    Double numeric = fileResultSet.getNumeric(5);
    Assertions.assertNull(numeric, "The number must be null");

  }

  @Test()
  public void ExcelDataSetGetDoubleBigIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getNumeric(6),
      "This cell index is 6 for 5 columns. An exception must be thrown."
    );

  }

  @Test()
  public void ExcelDataSetGetDoubleZeroIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getNumeric(0),
      "This cell index is 0. An exception must be thrown."
    );

  }

  @Test()
  public void ExcelDataSetGetDoubleNegativeIndex() {

    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getNumeric(-1),
      "This cell index is -1. An exception must be thrown."
    );

  }


  @Test()
  public void ExcelDataSetGetStringGood() throws SQLException {

    fileResultSet.absolute(1);
    Assertions.assertEquals("01", fileResultSet.getString(1), "This must be 01");

  }

  @Test()
  public void ExcelDataSetGetStringNull() throws SQLException {

    fileResultSet.absolute(4);
    Assertions.assertEquals("", fileResultSet.getString(1), "The value must be empty string");

  }

  @Test()
  public void ExcelDataSetGetStringBigIndex() {

    fileResultSet.absolute(1);
    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getString(6),
      "Too big column index must be throw an exception"
    );

  }

  @Test()
  public void ExcelDataSetGetStringNegativeIndex() {

    fileResultSet.absolute(1);
    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getString(-2),
      "Negative index must throw an exception"
    );

  }

  @Test()
  public void ExcelDataSetGetStringZeroIndex() {

    fileResultSet.absolute(1);
    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getString(0),
      "Zero index must throw an exception"
    );

  }

  @Test()
  public void ExcelDataSetGetStringBadRow() {

    fileResultSet.absolute(0);
    Assertions.assertThrows(
      SQLException.class,
      () -> fileResultSet.getString(1),
      "Reading a value on a no Row must throw an exception"
    );

  }

  @Test()
  public void ExcelDataSetNextLoopString() throws SQLException {

    fileResultSet.beforeFirst();
    StringBuilder firstColumn = new StringBuilder();
    while (fileResultSet.next()) {
      firstColumn.append(fileResultSet.getString(1));
    }
    Assertions.assertEquals("0102030506", firstColumn.toString(), "After the loop we must have");

  }

  @Test()
  public void ExcelDataSetNextLoopNumeric() throws SQLException {

    fileResultSet.beforeFirst();
    Double total = 0.0;
    while (fileResultSet.next()) {
      Double numeric = fileResultSet.getNumeric(5);
      if (null == numeric) {
        continue;
      }
      total = total + numeric;
    }
    Assertions.assertEquals((Double) 32.0, total, "After the loop we must have 32");

  }

  @Test()
  public void ExcelDataIsLastReturnTrue() {

    fileResultSet.absolute(6);
    Assertions.assertTrue(fileResultSet.isLast(), "IsLast must be true");

  }

  @Test()
  public void ExcelDataIsLastBigIndexReturnFalse() {

    fileResultSet.absolute(8);
    Assertions.assertFalse(fileResultSet.isLast(), "IsLast must be false");

  }

  @Test()
  public void ExcelDataIsLastNormalIndexReturnFalse() {

    fileResultSet.absolute(4);
    Assertions.assertFalse(fileResultSet.isLast(), "IsLast must be false");

  }

  @Test()
  public void ExcelDataIsAfterLastReturnTrue() {

    fileResultSet.absolute(6);
    fileResultSet.next();
    Assertions.assertTrue(fileResultSet.isAfterLast(), "IsAfterLast must be true");

  }

  @Test()
  public void ExcelDataIsAfterLastReturnFalse() {

    fileResultSet.absolute(6);
    Assertions.assertFalse(fileResultSet.isAfterLast(), "IsAfterLast must be false");

  }

  @Test()
  public void ExcelDataRelativeZero() {

    fileResultSet.absolute(3);
    fileResultSet.relative(0);
    Assertions.assertEquals(3, fileResultSet.getRow(), "Relative(0) Must do nothing");

  }

  @Test()
  public void ExcelDataRelativeZeroReturnTrue() {

    fileResultSet.absolute(3);
    Assertions.assertTrue(fileResultSet.relative(0), "Must return true as it's on a row");

  }

  @Test()
  public void ExcelDataRelativeZeroReturnFalse() {

    fileResultSet.beforeFirst();
    Assertions.assertFalse(fileResultSet.relative(0), "Must return false as it's not on a row");

  }

  @Test()
  public void ExcelDataRelativeNegativeNumInDataSet() {

    fileResultSet.absolute(5);
    fileResultSet.relative(-1);
    Assertions.assertEquals(4, fileResultSet.getRow(), "5-1: 4th row");

  }

  @Test()
  public void ExcelDataRelativeNegativeNumOutDataSet() {

    fileResultSet.absolute(5);
    fileResultSet.relative(-7);
    Assertions.assertEquals(0, fileResultSet.getRow(), "5-7 = 0 outside the data set");

  }

  @Test()
  public void ExcelDataRelativeNegativeNumReturnTrue() {

    fileResultSet.absolute(3);
    Assertions.assertTrue(fileResultSet.relative(-1), "It must be still on a Row");

  }

  @Test()
  public void ExcelDataRelativeNegativeNumReturnFalse() {

    fileResultSet.absolute(3);
    Assertions.assertFalse(fileResultSet.relative(-4), "It must be not on a Row");

  }

  @Test()
  public void ExcelDataRelativePositiveNumInDataSet() {

    fileResultSet.beforeFirst();
    fileResultSet.relative(1);
    Assertions.assertEquals(1, fileResultSet.getRow(), "1: 1th row");

  }

  @Test()
  public void ExcelDataRelativePositiveNumOutDataSet() {

    fileResultSet.beforeFirst();
    fileResultSet.relative(8);
    Assertions.assertEquals(7, fileResultSet.getRow(), "8: 8th row is outside the set, the row num must be after last");

  }

  @Test()
  public void ExcelDataRelativePositiveNumReturnTrue() {

    fileResultSet.absolute(5);
    Assertions.assertTrue(fileResultSet.relative(1), "Must be on the last row");

  }

  @Test()
  public void ExcelDataRelativePositiveNumReturnFalse() {

    Assertions.assertFalse(fileResultSet.relative(7), "Relative 7 must be outside the data set");

  }

  @Test()
  public void ExcelDataSetPreviousReturnTrue() {
    fileResultSet.last();
    Assertions.assertTrue(fileResultSet.previous(), "Previous must return true");
  }

  @Test()
  public void ExcelDataSetPreviousGoodRowNum() {
    fileResultSet.last();
    fileResultSet.previous();
    Assertions.assertEquals(5, fileResultSet.getRow(), "Previous must be on the 5th row");
  }

  @Test()
  public void ExcelDataSetPreviousReturnFalse() {
    fileResultSet.first();
    Assertions.assertFalse(fileResultSet.previous(), "Previous must return false after last");
  }

  @Test()
  public void ExcelDataSetPreviousLoopString() throws SQLException {

    fileResultSet.afterLast();
    StringBuilder firstColumn = new StringBuilder();
    while (fileResultSet.previous()) {
      firstColumn.append(fileResultSet.getString(1));
    }
    Assertions.assertEquals("0605030201", firstColumn.toString(), "After the loop we must have");

  }

  @Test()
  public void ExcelDataSetPreviousLoopNumeric() throws SQLException {

    fileResultSet.afterLast();
    Double total = 0.0;
    while (fileResultSet.previous()) {
      Double numeric = fileResultSet.getNumeric(5);
      if (numeric == null) {
        continue;
      }
      total = total + numeric;
    }
    Assertions.assertEquals((Double) 32.0, total, "After the loop we must have 32");

  }

  @Test()
  public void ExcelDataSetGetColumnTypes() throws SQLException {

    List<Integer> columnTypes = new ArrayList<>();
    for (int i = 0; i < fileResultSet.getMetaData().getColumnCount(); i++) {
      columnTypes.add(fileResultSet.getMetaData().getColumnType(i + 1));
    }
    Integer[] arrayColumnTypes = {Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR, Types.BOOLEAN, Types.DOUBLE};
    List<Integer> expectedColumnTypes = Arrays.asList(arrayColumnTypes);

    Assertions.assertEquals(expectedColumnTypes, columnTypes, "The two arrays must be equals");

  }

  @Test()
  public void ExcelDataSetGetColumnCount() throws SQLException {


    Assertions.assertEquals(5, fileResultSet.getMetaData().getColumnCount(), "The column count must be of size 5");

  }

//    @Test()
//    public void ExcelDataSetGetCurrentRowData()  {
//        this.fileResultSet.absolute(6);
//
//        List<DataPoint> rowData = fileResultSet.getCurrentRowData();
//
//        List<DataPoint> expectedRowData = new ArrayList<DataPoint>();
//
//        expectedRowData.add(new DataPoint("06"));
//
//        Calendar cal = Calendar.getInstance();
//        cal.set(2014, Calendar.APRIL, 27, 0, 0, 0);
//        // We get 808 milliseconds ???, then to 0
//        cal.set(Calendar.MILLISECOND, 0);
//        Timestamp date = new  Timestamp(cal.getTimeInMillis());
//
//        expectedRowData.add(new DataPoint(date));
//        expectedRowData.add(new DataPoint("JSch Integration"));
//        expectedRowData.add(new DataPoint(false));
//        expectedRowData.add(new DataPoint((Double) 9.0));
//
//        assertEquals("The list must be equals", true, rowData.equals(expectedRowData));
//
//    }

  @Test()
  public void ExcelDataSetGetColumnNames() throws SQLException {
    List<String> expectedHeaderNames = Arrays.asList("Personal deliverable", "Deadline", "Activity", "Boolean", "20");
    List<String> headersNames = new ArrayList<>();
    for (int i = 0; i < fileResultSet.getMetaData().getColumnCount(); i++) {
      headersNames.add(fileResultSet.getMetaData().getColumnName(i + 1));
    }
    Assertions.assertEquals(expectedHeaderNames, headersNames, "The headers names must be equals");
  }


}
