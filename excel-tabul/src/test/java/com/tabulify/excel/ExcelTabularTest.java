package com.tabulify.excel;

import com.tabulify.Tabular;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.gen.DataGenerator;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.sample.BytleSchema;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ExcelTabularTest {

  private Tabular tabular;
  private FsConnection resourceDataStore;

  @BeforeEach
  void setUp() {
    tabular = Tabular.tabularWithoutConfigurationFile();
    resourceDataStore = tabular.createRuntimeConnectionForResources(ExcelTabularTest.class, "excel");
  }

  @Test
  public void excelDataPathClass() {


    DataPath dataPath = resourceDataStore.getDataPath("testDataSet.xlsx");
    Assertions.assertEquals(ExcelDataPath.class, dataPath.getClass());

  }

  @Test
  public void printTest() {
    Path path = Paths.get("src", "test", "resources", "excel", "testDataSet.xlsx");

    ExcelDataPath excelDataPath = ((ExcelDataPath) resourceDataStore.getDataPath("testDataSet.xlsx"))
      .setHeaderId(1);
    Tabulars.print(excelDataPath);

  }


  @Test
  public void insertXlsxNoHeaderTest() throws SelectException {


    // Memory data path
    DataPath dataPath = tabular.getAndCreateRandomMemoryDataPath()
      .getOrCreateRelationDef().addColumn("first")
      .addColumn("second")
      .getDataPath();

    try (
      InsertStream insertStream = dataPath.getInsertStream()
    ) {
      insertStream.insert("1", "foo");
      insertStream.insert("2", "bar");
    }

    // Csv data path
    FsDataPath path = tabular.getTempFile("InsertStreamTest", ".xlsx");
    ExcelDataPath excelDataPath = (ExcelDataPath) new ExcelDataPath(path)
      .setHeaderId(0)
      .getOrCreateRelationDef()
      .copyDataDef(dataPath)
      .getDataPath();
    Assertions.assertFalse(Tabulars.exists(excelDataPath), "The excel file (" + excelDataPath + ") does not exist ");
    Tabulars.create(excelDataPath);

    // Assertion before insertion
    Assertions.assertTrue(Tabulars.exists(excelDataPath), "The excel file does exist");
    Assertions.assertEquals(0L, (long) excelDataPath.getCount(), "The excel file has no row");

    // Insertion
    try (
      SelectStream selectStream = dataPath.getSelectStream();
      InsertStream insertStream = excelDataPath.getInsertStream()
    ) {
      while (selectStream.next()) {
        insertStream.insert(selectStream.getObjects());
      }
    }
    // Assertion after
    Assertions.assertEquals(2L, (long) excelDataPath.getCount(), "The excel file (" + excelDataPath + ") has rows");


  }

  @Test
  public void insertXlsxHeaderTest() throws SelectException {


    // Memory data path
    String intHeader = "int";
    String dateHeader = "date";
    String timestampHeader = "timestamp";
    String booleanHeader = "boolean";
    DataPath dataPath = tabular.getAndCreateRandomMemoryDataPath()
      .getOrCreateRelationDef()
      .addColumn(intHeader, SqlDataTypeAnsi.INTEGER)
      .addColumn(dateHeader, SqlDataTypeAnsi.DATE)
      .addColumn(timestampHeader, SqlDataTypeAnsi.TIMESTAMP)
      .addColumn(booleanHeader, SqlDataTypeAnsi.BOOLEAN)
      .getDataPath();

    List<List<Object>> rows = Arrays.asList(
      Arrays.asList(1, new Date(), new Date(), true),
      Arrays.asList(2, new Date(), new Date(), false),
      Arrays.asList(null, null, null, null)
    );
    try (
      InsertStream insertStream = dataPath.getInsertStream()
    ) {
      for (List<Object> row : rows) {
        insertStream.insert(row);
      }
    }

    FsDataPath tempExcelFsPath = tabular.getTempFile("InsertStreamTest", ".xlsx");
    ExcelDataPath excelDataPath = (ExcelDataPath) new ExcelDataPath(tempExcelFsPath)
      .setHeaderId(1)
      .getOrCreateRelationDef()
      .copyDataDef(dataPath)
      .getDataPath();
    Assertions.assertFalse(Tabulars.exists(excelDataPath), "The excel file (" + excelDataPath + ") does not exist ");
    Tabulars.create(excelDataPath);

    // Assertion before insertion
    Assertions.assertTrue(Tabulars.exists(excelDataPath), "The excel file does exist");
    Assertions.assertEquals(0L, (long) excelDataPath.getCount(), "The excel file has no row");

    // Insertion
    try (
      SelectStream selectStream = dataPath.getSelectStream();
      InsertStream insertStream = excelDataPath.getInsertStream()
    ) {
      while (selectStream.next()) {
        insertStream.insert(selectStream.getObjects());
      }
    }

    Assertions.assertEquals(rows.size(), (long) excelDataPath.getCount(), "The excel file (" + excelDataPath + ") has rows");
    Assertions.assertTrue(Files.exists(tempExcelFsPath.getAbsoluteNioPath()), "The excel file was created");

    // Read it back header
    ExcelDataPath excelDataPathToReadWithHeader = new ExcelDataPath(tempExcelFsPath)
      .setHeaderId(1);
    Tabulars.print(excelDataPathToReadWithHeader);
    Assertions.assertEquals(rows.size(), (long) excelDataPathToReadWithHeader.getCount(), "The excel file (" + excelDataPath + ") has rows");
    // check the values
    try (
      SelectStream selectStream = excelDataPathToReadWithHeader.getSelectStream();
    ) {
      selectStream.next();
      List<?> firstRows = selectStream.getObjects();
      Assertions.assertEquals(4, firstRows.size());
      Assertions.assertEquals(Double.class, firstRows.get(0).getClass());
      Assertions.assertEquals(java.sql.Date.class, firstRows.get(1).getClass());
      Assertions.assertEquals(java.sql.Timestamp.class, firstRows.get(2).getClass());
      Assertions.assertEquals(Boolean.class, firstRows.get(3).getClass());
    }

    // Read it back without header to check the headers
    ExcelDataPath excelDataPathToReadWithoutHeader = new ExcelDataPath(tempExcelFsPath)
      .setHeaderId(0);
    Assertions.assertEquals(rows.size() + 1, (long) excelDataPathToReadWithoutHeader.getCount(), "The excel file (" + excelDataPath + ") has rows");
    Tabulars.print(excelDataPathToReadWithoutHeader);
    // Check the headers
    try (
      SelectStream selectStream = excelDataPathToReadWithoutHeader.getSelectStream()
    ) {
      selectStream.next();
      List<?> firstRows = selectStream.getObjects();
      Assertions.assertEquals(4, firstRows.size());
      Object actual = firstRows.get(0);
      Assertions.assertEquals(intHeader, actual);
      Assertions.assertEquals(String.class, actual.getClass());
      Assertions.assertEquals(dateHeader, firstRows.get(1));
      Assertions.assertEquals(timestampHeader, firstRows.get(2));
      Assertions.assertEquals(booleanHeader, firstRows.get(3));
      selectStream.next();
      List<?> secondRow = selectStream.getObjects();
      String expected = "1.0"; // a string because the data type comes from the header
      Assertions.assertEquals(expected, secondRow.get(0));
      Assertions.assertEquals(String.class, secondRow.get(0).getClass());
      String expected1 = "true"; // a string because the data type comes from the header row, not the first row of data
      Assertions.assertEquals(expected1, secondRow.get(3));
      Assertions.assertEquals(String.class, secondRow.get(3).getClass());
    }

  }

  @Test
  public void transferTest() {

    BytleSchema bytleSchema = BytleSchema.create(tabular.getMemoryConnection());
    long maxRowCount = 10L;
    DataPath sourceDataPath = bytleSchema.getDataPath(BytleSchema.TABLE_DATE_NAME);
    DataGenerator.create(tabular)
      .addDummyTransfer(sourceDataPath, maxRowCount)
      .build()
      .load()
      .throwErrorIfFail();
    FsDataPath tempFsDataPath = tabular.getTempFile("InsertStreamTest", ".xlsx");
    ExcelDataPath excelDataPath = new ExcelDataPath(tempFsDataPath);
    Tabulars.insert(sourceDataPath, excelDataPath);
    Assertions.assertEquals(maxRowCount, (long) excelDataPath.getCount(), "The excel file (" + excelDataPath + ") has rows");
    Tabulars.insert(sourceDataPath, excelDataPath);
    Assertions.assertEquals(2 * maxRowCount, (long) excelDataPath.getCount(), "The excel file (" + excelDataPath + ") has rows");
    Tabulars.print(excelDataPath);

  }

}
