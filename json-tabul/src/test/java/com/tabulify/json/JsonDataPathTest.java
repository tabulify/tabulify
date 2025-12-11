package com.tabulify.json;

import com.tabulify.Tabular;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.type.MediaTypes;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class JsonDataPathTest {

  private Tabular tabular;
  private FsConnection resourceDataStore;

  @BeforeEach
  public void setUp() throws Exception {
    tabular = Tabular.tabularWithoutConfigurationFile();
    resourceDataStore = tabular.createRuntimeConnectionForResources(JsonDataPathTest.class, "json");
  }

  @AfterEach
  public void tearDown() {
    tabular.close();
    tabular = null;
  }

  @Test
  public void jsonLTabularTest() {

    DataPath jsonDataPath =  resourceDataStore.getDataPath("file.jsonl");
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass());
    JsonDataPath jsonDataPath1 = (JsonDataPath) jsonDataPath;
    jsonDataPath1.setStructure(JsonStructure.PROPERTIES);
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass(), "The data path class is the good one");
    RelationDef dataDef = jsonDataPath.getOrCreateRelationDef();
    List<? extends ColumnDef<?>> columnDefs = dataDef.getColumnDefs();
    Assertions.assertEquals(6, columnDefs.size(), "The column number are the good one");
    List<String> columns = columnDefs.stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toList());
    List<String> expectedColumns = new ArrayList<>();
    expectedColumns.add("utm_medium");
    expectedColumns.add("created_at");
    expectedColumns.add("type");
    expectedColumns.add("tracking_id");
    expectedColumns.add("utm_campaign");
    expectedColumns.add("user_id");
    Assertions.assertEquals(expectedColumns, columns, "The column names are the good ones");
    Assertions.assertEquals(228L, (long) jsonDataPath.getCount(), "The number of rows are the good one");


  }

  @Test
  public void jsonLColumnTest() {

    DataPath jsonDataPath = resourceDataStore.getDataPath("file.jsonl");
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass());
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass(), "The data path class is the good one");
    RelationDef dataDef = jsonDataPath.getOrCreateRelationDef();
    List<? extends ColumnDef<?>> columnDefs = dataDef.getColumnDefs();
    Assertions.assertEquals(1, columnDefs.size(), "The column number are the good one");
    List<String> columns = columnDefs.stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toList());
    List<String> expectedColumns = new ArrayList<>();
    expectedColumns.add(JsonDataPath.JSON_DEFAULT_HEADER_NAME);
    Assertions.assertEquals(expectedColumns, columns, "The column names are the good ones");
    Assertions.assertEquals(228L, (long) jsonDataPath.getCount(), "The number of rows are the good one");

  }

  @Test
  public void jsonPropertiesTest() {

    DataPath jsonDataPath = resourceDataStore.getDataPath("file.json");
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass());
    JsonDataPath jsonDataPath1 = (JsonDataPath) jsonDataPath;
    jsonDataPath1.setStructure(JsonStructure.PROPERTIES);
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass(), "The data path class is the good one");
    RelationDef dataDef = jsonDataPath.getOrCreateRelationDef();
    List<? extends ColumnDef<?>> columnDefs = dataDef.getColumnDefs();
    Assertions.assertEquals(2, columnDefs.size(), "The column number are the good one");
    List<String> columns = columnDefs.stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toList());
    List<String> expectedColumns = new ArrayList<>();
    expectedColumns.add("id");
    expectedColumns.add("text");
    Assertions.assertEquals(expectedColumns, columns, "The column names are the good ones");
    Assertions.assertEquals(1, (long) jsonDataPath.getCount(), "The number of rows are the good one");


  }

  @Test
  public void print() {


    DataPath jsonDataPath = resourceDataStore.getDataPath("file.json")
      .setLogicalName("phones");
    Tabulars.print(jsonDataPath);

  }

  @Test
  public void jsonColumnTest() {

    DataPath jsonDataPath = resourceDataStore.getDataPath("file.json");
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass());
    JsonDataPath jsonDataPath1 = (JsonDataPath) jsonDataPath;
    jsonDataPath1.setStructure(JsonStructure.DOCUMENT);
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass(), "The data path class is the good one");
    RelationDef dataDef = jsonDataPath.getOrCreateRelationDef();
    List<? extends ColumnDef<?>> columnDefs = dataDef.getColumnDefs();
    Assertions.assertEquals(1, columnDefs.size(), "The column number are the good one");
    List<String> columns = columnDefs.stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toList());
    List<String> expectedColumns = new ArrayList<>();
    expectedColumns.add("json");
    Assertions.assertEquals(expectedColumns, columns, "The column names are the good ones");
    Assertions.assertEquals(1, (long) jsonDataPath.getCount(), "The number of rows are the good one");

  }

  /**
   * We test when we set properties
   */
  @Test
  public void jsonSetPropertiesTest() {


    DataPath jsonDataPath = resourceDataStore.getDataPath("file.json");
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass());
    JsonDataPath jsonDataPath1 = (JsonDataPath) jsonDataPath;
    jsonDataPath1.addAttribute(JsonDataAttributes.STRUCTURE, JsonStructure.PROPERTIES);
    Assertions.assertEquals(JsonDataPath.class, jsonDataPath.getClass(), "The data path class is the good one");
    RelationDef dataDef = jsonDataPath.getOrCreateRelationDef();
    List<? extends ColumnDef<?>> columnDefs = dataDef.getColumnDefs();
    Assertions.assertEquals(2, columnDefs.size(), "The column number are the good one");
    List<String> columns = columnDefs.stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toList());
    List<String> expectedColumns = new ArrayList<>();
    expectedColumns.add("id");
    expectedColumns.add("text");
    Assertions.assertEquals(expectedColumns, columns, "The column names are the good ones");
    Assertions.assertEquals(1, (long) jsonDataPath.getCount(), "The number of rows are the good one");

  }

  @Tag("remote")
  @Test
  public void httpJson() {

    DataUriNode dataUri = tabular.createDataUri("https://en.wikipedia.org/w/api.php?action=query&titles=SQL&format=json&prop=description|categories");
    List<DataPath> dataPaths = tabular.select(dataUri);
    Assertions.assertEquals(1, dataPaths.size());
    // This is a FsTextDataPath not yet a Json due to the niofs http implementation, need to be casted
    // DataPath dataPath = dataPaths.get(0);
    // Assert.assertEquals(dataPath.getClass().getSimpleName(), JsonDataPath.class.getSimpleName());

  }

  @Test
  public void selectWithMediaTypeTest() {

    String glob = "*.json*";

    DataUriNode dataUri = DataUriNode.builder()
      .setConnection(resourceDataStore)
      .setPath(glob)
      .build();
    List<DataPath> dataPaths = tabular.select(dataUri, MediaTypes.TEXT_PLAIN);
    Assertions.assertEquals(3, dataPaths.size(), "File selected");
    for (DataPath dataPath : dataPaths) {
      Assertions.assertEquals(FsTextDataPath.class, dataPath.getClass(), "With the css media type, the JSON file are returned as css");
    }

    dataPaths = tabular.select(
      DataUriNode.builder()
        .setConnection(resourceDataStore)
        .setPath(glob)
        .build()
    );
    for (DataPath dataPath : dataPaths) {
      Assertions.assertEquals(JsonDataPath.class, dataPath.getClass(), "Without the media type, the JSON file are returned as text");
    }

  }

}
