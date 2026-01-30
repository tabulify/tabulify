package com.tabulify.template.flow;

import com.tabulify.Tabular;
import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.DefinePipelineStep;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.fs.FsConnection;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.uri.DataUriNode;
import com.tabulify.template.JsonTemplate;
import com.tabulify.template.TextTemplate;
import com.tabulify.template.TextTemplateEngine;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Test
 */
public class TemplatePipelineStepStreamTest {

    private Tabular tabular;
    private FsConnection resourceDataStore;

    @BeforeEach
    public void setUp() {
        tabular = Tabular.tabularWithoutConfigurationFile();
        resourceDataStore = tabular.createRuntimeConnectionForResources(TemplatePipelineStepStreamTest.class, "templating");
    }

    @AfterEach
    public void tearDown() {
        tabular.close();
        tabular = null;
    }

    @Test
    public void enrichTargetTest() throws SelectException {


        DataPath data = tabular.getAndCreateRandomMemoryDataPath()
                .setLogicalName("fileAttributes")
                .createRelationDef()
                .addColumn("welcome")
                .getDataPath();
        String valueInserted = "Hello World !";
        data.getInsertStream()
                .insert(valueInserted)
                .close();


        List<DataPath> dataPaths = Pipeline.builder(tabular)
                .addStep(
                        DefinePipelineStep.builder()
                                .addDataPath(data)
                )
                .addStep(
                        TemplatePipelineStep.builder()
                                .setGranularity(Granularity.RECORD)
                                .setTemplateEngine(TemplateEngine.THYMELEAF)
                                .addTemplateSelector(DataUriNode.createFromConnectionAndPath(
                                        resourceDataStore,
                                        "templates/thym*.html"))
                                .addTemplateModelVariable("records", DataUriNode.createFromConnectionAndPath(
                                        resourceDataStore,
                                        "data/table.csv")
                                )
                                .setOutput(StepOutputArgument.TARGETS)
                                .setTargetType(TemplateTargetType.ENRICHED_INPUT)
                                .setTargetColumnName("html")
                )
                .build()
                .execute()
                .getDownStreamDataPaths();
        Assertions.assertEquals(1, dataPaths.size());
        DataPath next = dataPaths.iterator().next();
        Assertions.assertEquals(2, next.getRelationDef().getColumnsSize());
        try (SelectStream selectStream = next.getSelectStream()) {
            selectStream.next();
            String firstValue = selectStream.getString(1);
            Assertions.assertEquals(valueInserted, firstValue);
            String templateValue = selectStream.getString(2);
            Assertions.assertEquals(EXPECTED, templateValue);
        }
        Tabulars.print(next);

    }

    public static String EXPECTED = "<!DOCTYPE html>\n" +
            "\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n" +
            "\n" +
            "<head>\n" +
            "  <title>Good Thymes Virtual Grocery</title>\n" +
            "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "\n" +
            "<p>Hello World !</p>\n" +
            "\n" +
            "<h1>Product list</h1>\n" +
            "\n" +
            "<table>\n" +
            "  <tr>\n" +
            "    <th>NAME</th>\n" +
            "    <th>PRICE</th>\n" +
            "    <th>IN STOCK</th>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td>Fresh Sweet Basil</td>\n" +
            "    <td>4.99</td>\n" +
            "    <td>yes</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td>Italian Tomato</td>\n" +
            "    <td>1.25</td>\n" +
            "    <td>no</td>\n" +
            "  </tr>\n" +
            "</table>\n" +
            "\n" +
            "</body>\n" +
            "\n" +
            "</html>\n";

    @Test
    public void thymeleafModelVariableYamlPipelineTest() throws SelectException {
        DataPath data = tabular.getAndCreateRandomMemoryDataPath()
                .setLogicalName("fileAttributes")
                .createRelationDef()
                .addColumn("welcome")
                .getDataPath();
        data.getInsertStream()
                .insert("Hello World !")
                .close();

        DataUriNode tableSelector = DataUriNode.builder()
                .setConnection(resourceDataStore)
                .setPath("data/table.csv")
                .build();

        List<DataPath> dataPaths = Pipeline.builder(tabular)
                .addStep(
                        DefinePipelineStep.builder()
                                .addDataPath(data)
                )
                .addStep(
                        TemplatePipelineStep.builder()
                                .setTemplateEngine(TemplateEngine.THYMELEAF)
                                .addTemplateSelector(
                                        DataUriNode.builder()
                                                .setConnection(resourceDataStore)
                                                .setPath("templates/thym*.html")
                                                .build()
                                )
                                .addTemplateModelVariable("records", tableSelector)
                )
                .build()
                .execute()
                .getDownStreamDataPaths();
        Assertions.assertEquals(1, dataPaths.size());
        try (SelectStream selectStream = dataPaths.iterator().next().getSelectStream()) {
            selectStream.next();
            String actual = selectStream.getString(1);
            Assertions.assertEquals(EXPECTED, actual);
        }


    }


    @Test
    public void textTemplateTest() throws SelectException {


        DataPath data = tabular.getAndCreateRandomMemoryDataPath()
                .createRelationDef()
                .addColumn("id")
                .addColumn("name")
                .addColumn("1")
                .getDataPath();
        try (InsertStream insertStream = data.getInsertStream()) {
            insertStream
                    .insert("1", "Don", "new")
                    .insert("2", "Daniel", "happy")
                    .insert("3", "Arnold", "great");
        }


        String message = "Dear ${name}, we are \n" +
                "really luck to count you our $id, customers\n" +
                "All the best for this $1, year";
        TextTemplate textTemplateEngine = TextTemplateEngine.getOrCreate().compile(message
        );
        try (SelectStream selectStream = data.getSelectStream()) {
            while (selectStream.next()) {
                Map<String, Object> values = new HashMap<>();
                for (ColumnDef<?> columnDef : data.getOrCreateRelationDef().getColumnDefs()) {
                    values.put(columnDef.getColumnName(), selectStream.getString(columnDef.getColumnPosition()));
                }
                String result = textTemplateEngine.applyVariables(values).getResult();
                String expected = message
                        .replace("${name}", values.get("name").toString())
                        .replace("$id", values.get("id").toString())
                        .replace("$1", values.get("1").toString());
                Assertions.assertEquals(expected, result);
                textTemplateEngine.resetResult();
            }
        }


    }

    @Test
    public void textInlineTemplateYamlPipelineTest() throws URISyntaxException {

        Path path = Paths.get(Objects.requireNonNull(TemplatePipelineStepStreamTest.class.getResource("/templating/flow/text-inline-template--pipeline.yml")).toURI());
        Pipeline.createFromYamlPath(tabular, path)
                .execute();

    }

    /**
     * Test all arguments
     */
    @Test
    public void htmlEmailTemplateYamlPipelineTest() throws URISyntaxException {

        Path path = Paths.get(Objects.requireNonNull(TemplatePipelineStepStreamTest.class.getResource("/templating/flow/html-email-template--pipeline.yml")).toURI());
        Pipeline.createFromYamlPath(tabular, path)
                .execute();

    }

    @Test
    public void jsonTemplateTest() throws SelectException {

        String json = "{\n" +
                "    \"Before${version}After\": {\n" +
                "        \"${script}\": {\n" +
                "            \"file\": \"${file}-myfile\",\n" +
                "            \"integrity\": \"${integrity}\"\n" +
                "        }\n" +
                "    }\n" +
                "}";


        JsonTemplate jsonTemplate = JsonTemplate.compile(json);


        DataPath data = tabular.getAndCreateRandomMemoryDataPath()
                .createRelationDef()
                .addColumn("version")
                .addColumn("script")
                .addColumn("file")
                .addColumn("integrity")
                .getDataPath();
        try (InsertStream insertStream = data.getInsertStream()) {
            insertStream
                    .insert("4.4.1", "bootstrap.16col", "bootstrap.16col.min.css", "sha384-7/QJWEBUuqkcuwRA+9t03pPgZlWIijurW5dMHOroOhOnuvLJNs/+ia7CiFz7Sws2")
                    .insert("4.5.0", "bootstrap.16col", "bootstrap.16col.min.css", "sha384-qHUrb/0aQsXITU+/99hyrzzX8Sq1M5nNhf1u/bUHJUR+2J6K3oYJ4qQnB/5kZnMY")
                    .insert("4.5.0", "bootswatch.cerulean", "bootstrap.cerulean.min.css", "sha384-qHUrb/0aQsXITU+/99hyrzzX8Sq1M5nNhf1u/bUHJUR+2J6K3oYJ4qQnB/5kZnMY");
        }


        try (SelectStream selectStream = data.getSelectStream()) {
            while (selectStream.next()) {
                Map<String, Object> values = new HashMap<>();
                for (ColumnDef<?> columnDef : data.getOrCreateRelationDef().getColumnDefs()) {
                    values.put(columnDef.getColumnName(), selectStream.getString(columnDef.getColumnPosition()));
                }
                jsonTemplate.applyVariables(values);
            }
        }
        System.out.println(jsonTemplate.getResult());


    }

    @Test
    public void jsonPipelineTest() throws SelectException {

        DataPath data = tabular.getAndCreateRandomMemoryDataPath()
                .setLogicalName("fileAttributes")
                .createRelationDef()
                .addColumn("version")
                .addColumn("script")
                .addColumn("file")
                .addColumn("integrity")
                .getDataPath();
        try (InsertStream insertStream = data.getInsertStream()) {
            insertStream
                    .insert("4.4.1", "bootstrap.16col", "bootstrap.16col.min.css", "sha384-7/QJWEBUuqkcuwRA+9t03pPgZlWIijurW5dMHOroOhOnuvLJNs/+ia7CiFz7Sws2")
                    .insert("4.5.0", "bootstrap.16col", "bootstrap.16col.min.css", "sha384-qHUrb/0aQsXITU+/99hyrzzX8Sq1M5nNhf1u/bUHJUR+2J6K3oYJ4qQnB/5kZnMY")
                    .insert("4.5.0", "bootswatch.cerulean", "bootstrap.cerulean.min.css", "sha384-qHUrb/0aQsXITU+/99hyrzzX8Sq1M5nNhf1u/bUHJUR+2J6K3oYJ4qQnB/5kZnMY");
        }

        List<DataPath> dataPaths = Pipeline.builder(tabular)
                .addStep(
                        DefinePipelineStep.builder()
                                .addDataPath(data)
                )
                .addStep(
                        TemplatePipelineStep.builder()
                                .addTemplateSelector(
                                        DataUriNode.builder()
                                                .setConnection(resourceDataStore)
                                                .setPath("templates/*.json")
                                                .build()
                                )
                )
                .build()
                .execute()
                .getDownStreamDataPaths();
        assertJsonDataPath(dataPaths);


    }

    @Test
    public void jsonYamlPipelineInlineTemplateTest() throws URISyntaxException, SelectException {

        Path path = Paths.get(Objects.requireNonNull(TemplatePipelineStepStreamTest.class.getResource("/templating/flow/json-inline-template--pipeline.yml")).toURI());
        List<DataPath> dataPaths = Pipeline.createFromYamlPath(tabular, path)
                .execute()
                .getDownStreamDataPaths();
        assertJsonDataPath(dataPaths);

    }

    @Test
    public void jsonYamlPipelineTemplateSelectorTest() throws URISyntaxException, SelectException {

        Path path = Paths.get(Objects.requireNonNull(TemplatePipelineStepStreamTest.class.getResource("/templating/flow/json-selectors-template--pipeline.yml")).toURI());
        List<DataPath> dataPaths = Pipeline.createFromYamlPath(tabular, path)
                .execute()
                .getDownStreamDataPaths();
        assertJsonDataPath(dataPaths);


    }

    /**
     * Utility
     */
    private void assertJsonDataPath(List<DataPath> dataPaths) throws SelectException {
        Assert.assertEquals(1, dataPaths.size());
        try (SelectStream dataPath = dataPaths.iterator().next().getSelectStream()) {
            dataPath.next();
            String actual = dataPath.getString(1);
            String expected = "{\n" +
                    "  \"Before4.4.1After\": {\n" +
                    "    \"bootstrap.16col\": {\n" +
                    "      \"file\": \"bootstrap.16col.min.css-myfile\",\n" +
                    "      \"integrity\": \"sha384-7/QJWEBUuqkcuwRA+9t03pPgZlWIijurW5dMHOroOhOnuvLJNs/+ia7CiFz7Sws2\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"Before4.5.0After\": {\n" +
                    "    \"bootstrap.16col\": {\n" +
                    "      \"file\": \"bootstrap.16col.min.css-myfile\",\n" +
                    "      \"integrity\": \"sha384-qHUrb/0aQsXITU+/99hyrzzX8Sq1M5nNhf1u/bUHJUR+2J6K3oYJ4qQnB/5kZnMY\"\n" +
                    "    },\n" +
                    "    \"bootswatch.cerulean\": {\n" +
                    "      \"file\": \"bootstrap.cerulean.min.css-myfile\",\n" +
                    "      \"integrity\": \"sha384-qHUrb/0aQsXITU+/99hyrzzX8Sq1M5nNhf1u/bUHJUR+2J6K3oYJ4qQnB/5kZnMY\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
            Assert.assertEquals(expected, actual);
        }
    }

}
