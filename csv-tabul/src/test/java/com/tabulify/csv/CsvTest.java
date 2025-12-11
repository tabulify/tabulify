package com.tabulify.csv;

import com.tabulify.Tabular;
import com.tabulify.diff.DataPathDiff;
import com.tabulify.diff.DataPathDiffResult;
import com.tabulify.fs.FsDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.uri.DataUriBuilder;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NoColumnException;
import com.tabulify.fs.Fs;
import com.tabulify.type.KeyNormalizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

public class CsvTest extends CsvBase {


  @Test
  public void csvNameTest() {

    String name = "comment.csv";
    CsvDataPath csvDataPath = (CsvDataPath) resourceDataStore.getDataPath(name);
    Assertions.assertEquals(name, csvDataPath.getName(), "Name");

  }

  @Test
  public void csvInsertStreamTest() throws SelectException {

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
    FsDataPath path = tabular.getTempFile("InsertStreamTest", ".csv");
    CsvDataPath csvDataPath = (CsvDataPath) new CsvDataPath(path)
      .setHeaderRowId(1)
      .getOrCreateRelationDef()
      .copyDataDef(dataPath)
      .getDataPath();
    Assertions.assertFalse(Tabulars.exists(csvDataPath), "The csv file (" + csvDataPath + ") does not exist ");
    Tabulars.create(csvDataPath);

    // Assertion before insertion
    Assertions.assertTrue(Tabulars.exists(csvDataPath), "The csv file does exist");
    Assertions.assertEquals(0L, (long) csvDataPath.getCount(), "The csv file has no row");

    // Insertion
    try (
      SelectStream selectStream = dataPath.getSelectStream();
      InsertStream insertStream = csvDataPath.getInsertStream()
    ) {
      while (selectStream.next()) {
        insertStream.insert(selectStream.getObjects());
      }
    }
    // Assertion after
    Assertions.assertEquals(2L, (long) csvDataPath.getCount(), "The csv file (" + csvDataPath + ") has rows");

  }

  @Test
  public void csvCopyTest() {

    DataPath source = tabular.getTempFile("csv_test", "copy.csv")
      .getOrCreateRelationDef().addColumn("first")
      .addColumn("second")
      .getDataPath();
    Tabulars.create(source);
    try (
      InsertStream insertStream = source.getInsertStream()
    ) {
      insertStream.insert("1", "foo");
      insertStream.insert("2", "bar");
    }

    Path path = Fs.getTempFilePath("test", ".csv");
    DataPath targetCsv = tabular.getDataPath(path);
    Assertions.assertEquals(CsvDataPath.class, targetCsv.getClass(), "This is an instance of the Csv Data Path");

    Tabulars.copy(source, targetCsv);
    Assertions.assertEquals(2L, (long) targetCsv.getCount(), "The csv file (" + targetCsv + ") has rows");
    Assertions.assertEquals(true, Tabulars.exists(source), "The source file (" + source + ") exist ");


  }

  @Test
  public void csvMoveTest() {

    DataPath source = tabular.getTempFile("csv_test", "move.csv")
      .getOrCreateRelationDef().addColumn("first")
      .addColumn("second")
      .getDataPath();
    Tabulars.create(source);
    try (
      InsertStream insertStream = source.getInsertStream()
    ) {
      insertStream.insert("1", "foo");
      insertStream.insert("2", "bar");
    }

    Path path = Fs.getTempFilePath("testMove", ".csv");
    DataPath targetCsv = tabular.getDataPath(path);
    Tabulars.dropIfExists(targetCsv);
    Assertions.assertEquals(CsvDataPath.class, targetCsv.getClass(), "This is an instance of the Csv Data Path");

    Tabulars.move(source, targetCsv);
    Assertions.assertEquals(2L, (long) targetCsv.getCount(), "The csv file (" + targetCsv + ") has rows");
    Assertions.assertEquals(false, Tabulars.exists(source), "The source file (" + source + ") does not exist ");

  }

  @Test
  public void csvInsertTest() {

    DataPath source = tabular.getTempFile("csv_source", "insert.csv")
      .getOrCreateRelationDef()
      .addColumn("first")
      .addColumn("second")
      .getDataPath();
    Tabulars.create(source);
    try (
      InsertStream insertStream = source.getInsertStream()
    ) {
      insertStream.insert("1", "foo");
      insertStream.insert("2", "bar");
    }

    Path path = Fs.getTempFilePath("csv_target", ".csv");
    DataPath targetCsv = tabular.getDataPath(path)
      .getOrCreateRelationDef()
      .mergeDataDef(source)
      .getDataPath();
    Assertions.assertEquals(CsvDataPath.class, targetCsv.getClass(), "This is an instance of the Csv Data Path");
    Tabulars.create(targetCsv);
    // don't create otherwise we get the headers and the insert will add the headers 2 times
    Tabulars.insert(source, targetCsv);
    Assertions.assertEquals(2L, (long) targetCsv.getCount(), "The csv file (" + targetCsv + ") has rows");
    Assertions.assertEquals(true, Tabulars.exists(source), "The source file (" + source + ") exists ");
    Tabulars.insert(source, targetCsv);
    Assertions.assertEquals(4L, (long) targetCsv.getCount(), "The csv file (" + targetCsv + ") has 2 rows more");

  }

  @Test
  public void csvReadTest() throws NoColumnException {


    CsvDataPath csvDataPath = resourceDataStore.getDataPath("test.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setQuoteCharacter('"');


    Assertions.assertEquals(23, csvDataPath.getOrCreateRelationDef().getColumnsSize(), "Count Columns");
    final String email_address = "Email Address";
    Assertions.assertEquals(email_address, csvDataPath.getOrCreateRelationDef().getColumnDef(1).getColumnName(), "ColumnName by index");
    Assertions.assertEquals(email_address, csvDataPath.getOrCreateRelationDef().getColumnDef(email_address).getColumnName(), "ColumnName by index");


  }

  @Test
  public void csvStreamTest() {

    CsvDataPath fsDataPath = resourceDataStore.getDataPath("test.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setEndOfRecords("\r\n");

    // Size
    Assertions.assertEquals(16L, (long) fsDataPath.getCount(), "Count");

    // First line
    try (
      SelectStream csvSelectStream = fsDataPath.getSelectStream()
    ) {
      csvSelectStream.beforeFirst();
      csvSelectStream.next();
      Assertions.assertEquals("lol@yahoo.com", csvSelectStream.getString(1), "First Row Value");
    }


  }

  @Test
  public void csvDelimiterTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("delimiter.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setQuoteCharacter('"')
      .setEndOfRecords("\r\n") // Needed for cross-platform test
      .setDelimiterCharacter(';');

    Assertions.assertEquals(3, csvDataPath.getOrCreateRelationDef().getColumnsSize(), "The number of columns is the good one");
    // Size
    Assertions.assertEquals(1, (long) csvDataPath.getCount(), "Count");

    // First line
    try (
      SelectStream csvSelectStream = csvDataPath.getSelectStream()
    ) {
      csvSelectStream.beforeFirst();
      csvSelectStream.next();
      Assertions.assertEquals("1 fake street", csvSelectStream.getString(3), "First Row Value");
    }

  }

  @Test
  public void csvHeaderTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("header.csv", CsvDataPath.class)
      .setHeaderRowId(5)
      .setEndOfRecords("\r\n") // Needed for cross platform test
      .setQuoteCharacter('"');

    Assertions.assertEquals(3, csvDataPath.getOrCreateRelationDef().getColumnsSize(), "Count Columns");
    Assertions.assertEquals("First Name", csvDataPath.getOrCreateRelationDef().getColumnDef(1).getColumnName(), "The first column name is the good one");
    Assertions.assertEquals("Last Name", csvDataPath.getOrCreateRelationDef().getColumnDef(2).getColumnName(), "The second column name is the good one");
    Assertions.assertEquals("Address", csvDataPath.getOrCreateRelationDef().getColumnDef(3).getColumnName(), "The third column name is the good one");
    // Size
    Assertions.assertEquals(1, (long) csvDataPath.getCount(), "Count");

    // First line
    try (
      SelectStream csvSelectStream = csvDataPath.getSelectStream()
    ) {
      csvSelectStream.beforeFirst();
      csvSelectStream.next();
      Assertions.assertEquals("1 fake street", csvSelectStream.getString(3), "First Row Value");
    }

  }

  /**
   * The second header is empty
   */
  @Test
  public void csvHeaderEmptyNameTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("header-empty-name.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setEndOfRecords("\r\n") // Needed for cross platform test
      .setQuoteCharacter('"');

    Assertions.assertEquals(3, csvDataPath.getOrCreateRelationDef().getColumnsSize(), "Count Columns");
    Assertions.assertEquals("first header", csvDataPath.getOrCreateRelationDef().getColumnDef(1).getColumnName(), "The first column name is the good one");
    Assertions.assertEquals("col2", csvDataPath.getOrCreateRelationDef().getColumnDef(2).getColumnName(), "The second column name is the good one");
    Assertions.assertEquals("third", csvDataPath.getOrCreateRelationDef().getColumnDef(3).getColumnName(), "The third column name is the good one");
    // Size
    Assertions.assertEquals(1, (long) csvDataPath.getCount(), "Count");

    // First line
    try (
      SelectStream csvSelectStream = csvDataPath.getSelectStream()
    ) {
      csvSelectStream.beforeFirst();
      csvSelectStream.next();
      Assertions.assertEquals("1 fake street", csvSelectStream.getString(3), "3 columns data");
    }

  }

  @Test
  public void csvQuoteTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("quote.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setEndOfRecords("\r\n") // Needed for cross platform test
      .setDelimiterCharacter(',')
      .setQuoteCharacter('"');


    try (
      SelectStream csvSelectStream = csvDataPath.getSelectStream()
    ) {
      csvSelectStream.beforeFirst();
      csvSelectStream.next();
      Assertions.assertEquals("Foo\nline break test", csvSelectStream.getString(1), "First Cell Value: A line break in a quoted cell value");
      Assertions.assertEquals("\"Bar", csvSelectStream.getString(2), "Second Cell Value: A escaped double quote ");
      Assertions.assertEquals("1, fake street", csvSelectStream.getString(3), "Third Cell Value: ");
    }

  }

  /**
   * When non-strict, the quote will be removed
   */
  @Test
  public void csvQuoteAutoTest() {

    try (Tabular tabular =
           Tabular.builder()
             .cleanEnv(true)
             .setStrictExecution(false)
             .build()
    ) {

      CsvDataPath csvDataPath = tabular.createRuntimeConnectionForResources(CsvTest.class, CSV_RESOURCE_ROOT).getDataPath("quoteAuto.csv", CsvDataPath.class)
        .setHeaderRowId(0);


      try (
        SelectStream csvSelectStream = csvDataPath.getSelectStream()
      ) {
        csvSelectStream.next();
        Assertions.assertEquals("With Quote", csvSelectStream.getString(1), "First Cell Value");
        Assertions.assertEquals("Without Quote", csvSelectStream.getString(2), "Second Cell Value");
      }

    }
  }

  @Test
  public void csvNewLineTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("newline.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setEndOfRecords("\n");

    Assertions.assertEquals(16, (long) csvDataPath.getCount(), "Count");

  }

  @Test
  public void csvEscapeTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("escape.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setEndOfRecords("\r\n")
      .setEscapeCharacter('\\')
      .setQuoteCharacter('"');

    Assertions.assertEquals(1, (long) csvDataPath.getCount(), "Count");

    try (
      SelectStream csvSelectStream = csvDataPath.getSelectStream()
    ) {
      csvSelectStream.beforeFirst();
      csvSelectStream.next();
      Assertions.assertEquals(" Foo \"", csvSelectStream.getString(1), "First Cell Value: Escape test");
      Assertions.assertEquals("B\"ar", csvSelectStream.getString(2), "Second Cell Value: Escape test");
      Assertions.assertEquals("\"Hallo\"", csvSelectStream.getString(3), "Third Cell Value: Escape test");
    }


  }

  @Test
  public void csvCommentTest() {


    CsvDataPath csvDataPath = resourceDataStore.getDataPath("comment.csv", CsvDataPath.class)
      .setHeaderRowId(1)
      .setEndOfRecords("\r\n")
      .setCommentCharacter('#');

    Assertions.assertEquals(1, (long) csvDataPath.getCount(), "Count");

  }

  @Test
  public void csvNoHeaderTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("noheader.csv", CsvDataPath.class)
      .setHeaderRowId(0)
      .setEndOfRecords("\r\n");

    Assertions.assertEquals(3, csvDataPath.getOrCreateRelationDef().getColumnsSize(), "Count Columns");

    Assertions.assertEquals(2L, (long) csvDataPath.getCount(), "Count");


  }

  /**
   * Cell without data, no header
   */
  @Test
  public void csvNoHeaderNoDataTest() {

    /*
     * Without data test
     */
    CsvDataPath csvDataPath = resourceDataStore.getDataPath("noheader-nodata.csv", CsvDataPath.class)
      .setHeaderRowId(0);

    RelationDef orCreateRelationDef = csvDataPath.getOrCreateRelationDef();
    String[] columnNames = orCreateRelationDef.getColumnDefs()
      .stream()
      .map(ColumnDef::getColumnName)
      .toArray(String[]::new);
    String[] columnNamesExpected = {"col1", "col2", "col3"};
    Assertions.assertArrayEquals(columnNamesExpected, columnNames);
    Assertions.assertEquals(3, orCreateRelationDef.getColumnsSize(), "Count Columns");
    Assertions.assertEquals(2L, (long) csvDataPath.getCount(), "Count");


  }

  @Test
  public void csvColumnNameOneCharacterHeaderTest() {


    CsvDataPath csvDataPath = resourceDataStore.getDataPath("header-one-character.csv", CsvDataPath.class)
      .setDelimiterCharacter(';')
      .setHeaderRowId(1);
    Assertions.assertEquals(1L, (long) csvDataPath.getCount(), "Count");


  }

  @Test
  public void csvNoHeaderCommentFirstTest() {


    CsvDataPath csvDataPath = resourceDataStore.getDataPath("noheaderCommentFirst.csv", CsvDataPath.class)
      .setCommentCharacter('#')
      .setEndOfRecords("\r\n")
      .setHeaderRowId(0);

    Assertions.assertEquals(3, csvDataPath.getOrCreateRelationDef().getColumnsSize(), "Count Columns");
    Assertions.assertEquals(2L, (long) csvDataPath.getCount(), "Count");


  }

  /**
   * An empty csv file should return 0 records
   */
  @Test
  public void csvEmptyTest() {

    FsDataPath path = tabular.getTempFile("emptyTest", ".csv");
    DataPath targetCsv = CsvDataPath.createFrom(path)
      .setHeaderRowId(0);
    Tabulars.create(targetCsv);
    Assertions.assertEquals(0, (long) targetCsv.getCount(), "An empty Csv has 0 records");

    // With a header it should also have zero
    targetCsv = CsvDataPath.createFrom(path)
      .setHeaderRowId(1);
    Assertions.assertEquals(0, (long) targetCsv.getCount(), "An empty Csv has 0 records");

  }

  @Test
  public void FsTableSystemTest() {

    CsvDataPath dataPath = resourceDataStore.getDataPath("test.csv", CsvDataPath.class)
      .setHeaderRowId(1);

    Boolean b = Tabulars.exists(dataPath);
    Assertions.assertEquals(true, b, "Exist");
    CsvSelectStream selectStream = (CsvSelectStream) dataPath.getSelectStream();
    int rows = 0;
    while (selectStream.next()) {
      rows++;
    }
    Assertions.assertEquals(16, rows, ">0");
  }

  /**
   * As a CSV a text file, it should be able to return a DataDef
   * of one cell that contains a line
   */
  @Test
  public void noStructureTest() {

    /**
     * Without header
     */
    CsvDataPath dataPath = resourceDataStore.getDataPath("delimiter.csv", CsvDataPath.class)
      .setHeaderRowId(0)
      .setDelimiterCharacter(';');
    Assertions.assertEquals(2L, (long) dataPath.getCount(), "Size is the good one");
    Assertions.assertEquals("col1", dataPath.getOrCreateRelationDef().getColumnDef(1).getColumnName(), "Column name is the good one");

  }


  @Test
  public void emptyLineTest() {

    CsvDataPath csvDataPath = resourceDataStore.getDataPath("emptyLine.csv", CsvDataPath.class)
      .setIgnoreEmptyLine(true)
      .setQuoteCharacter('"');
    Assertions.assertEquals(1L, (long) csvDataPath.getCount(), "Size is the good one");

  }

  /**
   * Example of a dynamic generated cs
   */
  @Test
  void bashScriptedCsvManifestTest() throws CastException {


    DataPath dataPath = resourceDataStore.getDataPath("scripted--csv.yml");
    Assertions.assertEquals(CsvDataPath.class, dataPath.getClass());
    ColumnDef<?> id = dataPath.getRelationDef().getColumnDefSafe("id");
    Assertions.assertEquals(id.getDataType().getAnsiType(), SqlDataTypeAnsi.INTEGER);
    Assertions.assertTrue(Tabulars.exists(dataPath));
    // Visual Test
    // Tabulars.print(dataPath);
    Assertions.assertEquals("Id, Greeting", dataPath.getOrCreateRelationDef().getColumnDefs().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")));
    List<List<?>> expected = List.of(
      List.of(1, "Hello 1"),
      List.of(2, "Hello 2"),
      List.of(3, "Hello 3")
    );
    DataPathDiffResult diffResult = DataPathDiff.builder(tabular)
      .build()
      .diff(expected, dataPath);
    // Visual Test
    Tabulars.print(diffResult.getResultAccumulatorReport());
    Assertions.assertTrue(diffResult.areEquals());

  }

  /**
   * A csv and a data def manifest test
   */
  @Test
  public void dataDefFromKindManifestWithoutDataUriYamlReadFromCsv() throws NoColumnException, CastException {
    /**
     * The manifest datadef-kind--csv.yml should be used
     */
    DataUriStringNode dataUriStringNode = DataUriStringNode.createFromString("datadef-kind.csv@" + resourceDataStore.getName().toString());
    assertDataDef(dataUriStringNode);
  }

  /**
   * A csv and a data def manifest test
   */
  @Test
  public void dataDefFromKindManifestWithoutDataUriYamlReadFromYaml() throws NoColumnException, CastException {

    /**
     * The data uri is not present, the `datadef-kind.csv@md` should be used
     */
    String pathOrName = "datadef-kind--csv.yml";
    DataPath dataPath = resourceDataStore.getDataPath(pathOrName);
    Assertions.assertEquals("datadef-kind.csv", dataPath.getName());
    Assertions.assertEquals(true, Tabulars.exists(dataPath));

    /**
     * Data Def assertion
     */
    DataUriStringNode dataUriStringNode = DataUriStringNode.createFromString(pathOrName + "@" + resourceDataStore.getName().toString());
    assertDataDef(dataUriStringNode);

  }

  /**
   * A csv and a data def manifest test
   */
  @Test
  public void dataDefManifestYamlRead() throws NoColumnException, CastException {
    /**
     * The manifest datadef-only--data-def.yml should be used
     */
    DataUriStringNode dataUriStringNode = DataUriStringNode.createFromString("datadef-only.csv@" + resourceDataStore.getName().toString());
    assertDataDef(dataUriStringNode);
  }

  /**
   * A full kind data def with data uri should work
   */
  @Test
  public void dataDefFullKindWithDataUriManifestYamlRead() throws NoColumnException, CastException {

    DataUriStringNode dataUriStringNode = DataUriStringNode.createFromString("datadef-with-data-uri--csv.yml@" + resourceDataStore.getName().toString());

    assertDataDef(dataUriStringNode);

  }

  private void assertDataDef(DataUriStringNode dataUriStringNode) throws NoColumnException {
    DataUriNode dataUri = DataUriBuilder
      .builder(tabular)
      .addConnection(resourceDataStore)
      .build()
      .apply(dataUriStringNode);


    List<DataPath> dataPaths = tabular.select(dataUri);
    Assertions.assertEquals(1, dataPaths.size(), "One was found");
    DataPath dataPath = dataPaths.get(0);
    Assertions.assertEquals(CsvDataPath.class, dataPath.getClass());
    CsvDataPath csvDataPath = (CsvDataPath) dataPath;
    /*
     * Data Def of the headers
     */
    Assertions.assertEquals('^', csvDataPath.getCommentCharacter(), "Comment Character");
    Assertions.assertEquals(';', (char) csvDataPath.getDelimiterCharacter(), "Delimiter Character");
    Assertions.assertEquals('\\', (char) csvDataPath.getEscapeCharacter(), "Escape Character");
    Assertions.assertEquals(3, csvDataPath.getHeaderRowId(), "Header Row Id");
    Assertions.assertFalse(csvDataPath.isIgnoreEmptyLine(), "Ignore Empty Line");
    Assertions.assertEquals('\'', (char) csvDataPath.getQuoteCharacter(), "Quote Character");
    /*
     * Data Def of the columns
     */
    RelationDef relationDef = csvDataPath.getOrCreateRelationDef();
    Assertions.assertEquals(4, relationDef.getColumnsSize(), "Column Size");
    String integerColumnName = "Integer";
    ColumnDef<?> columnDef = relationDef.getColumnDef(integerColumnName);
    Assertions.assertNotNull(columnDef, "Column Number");
    Assertions.assertEquals(Types.INTEGER, columnDef.getDataType().getVendorTypeNumber(), "Column data type - the column existed already");
    Assertions.assertEquals(4, (int) columnDef.getColumnPosition(), "Column position - the position has stayed the same");
  }

  @Test
  public void createRelationDef() {
    CsvDataPath csvDataPath = (CsvDataPath) resourceDataStore.getDataPath("emptyLine.csv");
    Assertions.assertEquals(3, csvDataPath.getOrCreateRelationDef().getColumnsSize());
  }


}
