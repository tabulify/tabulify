package com.tabulify.yaml;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.tabulify.Tabular;
import com.tabulify.fs.FsConnection;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.SelectStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class YamlDataPathTest {


  private Tabular tabular;
  private FsConnection resourceDataStore;

  @BeforeEach
  void setUp() {
    if (tabular != null) {
      tabular.close();
    }
    tabular = Tabular.tabularWithCleanEnvironment();
    resourceDataStore = tabular.createRuntimeConnectionForResources(YamlDataPathTest.class, "yaml");
  }

  /**
   * A yaml extension is a yaml file
   */
  @Test
  public void yamlAsYamlTest() {
    DataPath dataPath = resourceDataStore.getDataPath("whatever.yaml");
    Assertions.assertEquals(YamlDataPath.class, dataPath.getClass());
  }

  @Test
  public void ymlAsJsonTest() {


    DataPath dataPath = resourceDataStore.getDataPath("spec2_28.yml");
    Assertions.assertEquals(YamlDataPath.class, dataPath.getClass());
    YamlDataPath yamlDataPath = (YamlDataPath) dataPath;

    yamlDataPath.setOutputStyle(YamlStyle.JSON);
    RelationDef dataDef = yamlDataPath.getOrCreateRelationDef();
    List<? extends ColumnDef<?>> columnDefs = dataDef.getColumnDefs();
    Assertions.assertEquals(1, columnDefs.size(), "The column number are the good one");
    Assertions.assertEquals(YamlDataPath.YAML_DEFAULT_HEADER_NAME, columnDefs.get(0).getColumnName());

    SelectStream stream = yamlDataPath.getSelectStream();
    int count = 0;
    String expected1 = "{\n" +
      "  \"Time\": \"2001-11-23 15:01:42 -5\",\n" +
      "  \"User\": \"ed\",\n" +
      "  \"Warning\": \"This is an error message for the log file\"\n" +
      "}\n";
    String expected2 = "{\n" +
      "  \"Time\": \"2001-11-23 15:02:31 -5\",\n" +
      "  \"User\": \"ed\",\n" +
      "  \"Warning\": \"A slightly different error message.\"\n" +
      "}\n";
    String expected3 = "{\n" +
      "  \"Date\": \"2001-11-23 15:03:17 -5\",\n" +
      "  \"User\": \"ed\",\n" +
      "  \"Fatal\": \"Unknown variable \\\"bar\\\"\",\n" +
      "  \"Stack\": [\n" +
      "    {\n" +
      "      \"file\": \"TopClass.py\",\n" +
      "      \"line\": \"23\",\n" +
      "      \"code\": \"x = MoreObject(\\\"345\\\\n\\\")\\n\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"file\": \"MoreClass.py\",\n" +
      "      \"line\": \"58\",\n" +
      "      \"code\": \"foo = bar\"\n" +
      "    }\n" +
      "  ]\n" +
      "  \n" +
      "}\n";
    Iterator<String> expectedIterator = Arrays.asList(expected1, expected2, expected3)
      .iterator();
    while (stream.next()) {
      count++;
      String yamlDocString = stream.getString(1);
      String yamlExpectedDocString = expectedIterator.next();
      Assertions.assertEquals(yamlExpectedDocString, yamlDocString, count + " expectation");
      // Valid json
      @SuppressWarnings("unchecked") LinkedTreeMap<String, Object> jsonMap = (LinkedTreeMap<String, Object>) (new Gson()).fromJson(yamlDocString, Object.class);
      Assertions.assertFalse(jsonMap.isEmpty());
    }
    Assertions.assertEquals(3, count);

  }

}
