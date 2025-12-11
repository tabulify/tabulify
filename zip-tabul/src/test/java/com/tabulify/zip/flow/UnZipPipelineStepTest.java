package com.tabulify.zip.flow;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.engine.PipelineBuilder;
import com.tabulify.flow.engine.PipelineResult;
import com.tabulify.flow.operation.DefinePipelineStep;
import com.tabulify.flow.operation.PrintPipelineStep;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.flow.operation.StreamType;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.PrinterPrintFormat;
import com.tabulify.uri.DataUriNode;
import com.tabulify.zip.datapath.ArchiveDataPath;
import com.tabulify.glob.Glob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;


class UnZipPipelineStepTest {


  private Tabular tabular;

  @BeforeEach
  void setUp() {
    tabular = Tabular.tabularWithoutConfigurationFile();
  }

  @AfterEach
  void tearDown() {
    tabular.close();
    tabular = null;
  }

  /**
   * Test a target uri that delete the root
   */
  @Test
  public void unzipTargetTemplateWithoutRootTest() {

    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/archive.tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    PipelineResult execution = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.TARGETS)
          .setEntrySelector(Glob.createOf("zip/*"))
          .setTargetDataUri(
            DataUriNode
              .builder()
              .setConnection(tabular.getTmpConnection())
              .setPath("${entry_1}")
              .build()
          )
      )
      .addStep(PrintPipelineStep.builder())
      .build()
      .execute();

    List<DataPath> downStreamDataPaths = execution.getDownStreamDataPaths();
    Assertions.assertEquals(2, downStreamDataPaths.size());
    DataPath emptyDataPath = downStreamDataPaths.get(0);
    // No zip
    Assertions.assertEquals("empty.txt", emptyDataPath.getCompactPath());


  }

  @Test
  public void unzipEntrySelectorsTest() {

    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/archive.tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    PipelineResult execution = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.TARGETS)
          .setEntrySelector(Glob.createOf("*empty*"))
          .setTargetDataUri(
            DataUriNode
              .builder()
              .setConnection(tabular.getTmpConnection())
              .setPath("${entry_path}")
              .build()
          )
      )
      .addStep(PrintPipelineStep.builder())
      .build()
      .execute();

    List<DataPath> downStreamDataPaths = execution.getDownStreamDataPaths();
    Assertions.assertEquals(1, downStreamDataPaths.size());
    DataPath resultDataPath = downStreamDataPaths.get(0);
    // Test on the entry path
    Assertions.assertEquals("empty.txt", resultDataPath.getName());


  }

  /**
   * No path in the target uri is not allowed
   */
  @Test
  public void unzipBadTargetDataUri() {

    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/archive.tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    PipelineBuilder pipelineBuilder = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.RESULTS)
          .setTargetDataUri(
            DataUriNode
              .builder()
              .setConnection(tabular.getTmpConnection())
              .build()
          )
      )
      .addStep(PrintPipelineStep.builder());

    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, pipelineBuilder::build);
    String message = exception.getMessage();
    // visual test
    System.out.println(message);
    Assertions.assertTrue(message.contains("target data uri should have a path"));


  }

  @Test
  public void unzipResultsOutput() {

    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/archive.tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    PipelineResult execution = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.RESULTS)
      )
      .addStep(PrintPipelineStep.builder())
      .build()
      .execute();

    List<DataPath> downStreamDataPaths = execution.getDownStreamDataPaths();
    Assertions.assertEquals(1, downStreamDataPaths.size());
    DataPath resultDataPath = downStreamDataPaths.get(0);
    List<List<?>> records = resultDataPath.getRecords();
    Assertions.assertEquals(2, records.size());
    // Test on the entry path
    Assertions.assertEquals("zip/empty.txt", records.get(0).get(1));
    Assertions.assertEquals("zip/foo.txt", records.get(1).get(1));


  }

  @Test
  public void unzipTargetStrippedOutput() {

    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/archive.tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    PipelineResult execution = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.TARGETS)
          .setStripComponents(1)
      )
      .build()
      .execute();

    List<DataPath> downStreamDataPaths = execution.getDownStreamDataPaths();
    Assertions.assertEquals(2, downStreamDataPaths.size());
    Assertions.assertEquals("empty.txt", downStreamDataPaths.get(0).getCompactPath());
    Assertions.assertEquals("foo.txt", downStreamDataPaths.get(1).getCompactPath());


  }

  @Test
  public void unzipTargetDefaultOutput() {

    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/archive.tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    PipelineResult execution = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.TARGETS)
      )
      .build()
      .execute();

    List<DataPath> downStreamDataPaths = execution.getDownStreamDataPaths();
    Assertions.assertEquals(2, downStreamDataPaths.size());
    DataPath emptyDataPath = downStreamDataPaths.get(0);
    Assertions.assertEquals("empty.txt", emptyDataPath.getName());
    Assertions.assertEquals("zip/empty.txt", emptyDataPath.getCompactPath());
    Assertions.assertEquals("tmp", emptyDataPath.getConnection().getName().toString());
    Assertions.assertEquals("foo.txt", downStreamDataPaths.get(1).getName());


  }

  @Test
  public void unzipTargetTemplateOutput() {

    String logicalName = "archive";
    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/" + logicalName + ".tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    String root = "root";
    String templateRoot = root + "/${input_logical_name}/${entry_path}";
    PipelineResult execution = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.TARGETS)
          .setTargetDataUri(
            DataUriNode.builder()
              .setConnection(tabular.getTmpConnection())
              .setPath(templateRoot)
              .build()
          )
      )
      .build()
      .execute();

    List<DataPath> downStreamDataPaths = execution.getDownStreamDataPaths();
    Assertions.assertEquals(2, downStreamDataPaths.size());
    Assertions.assertEquals(root + "/" + logicalName + "/zip/empty.txt", downStreamDataPaths.get(0).getCompactPath());
    Assertions.assertEquals(root + "/" + logicalName + "/zip/foo.txt", downStreamDataPaths.get(1).getCompactPath());


  }

  @Test
  public void unzipResultsSplitOutput() {

    DataPath dataPath = tabular.getResourceDataPath(UnZipPipelineStepTest.class, "archives/archive.tar.gz");
    Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());

    PipelineResult execution = Pipeline.builder(tabular)
      .addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      )
      .addStep(
        UnZipPipelineStep
          .builder()
          .setOutput(StepOutputArgument.RESULTS)
          .setStreamType(StreamType.SPLIT)
      )
      .addStep(
        PrintPipelineStep
          .builder()
          .setFormat(PrinterPrintFormat.STREAM)
      )
      .build()
      .execute();

    List<DataPath> downStreamDataPaths = execution.getDownStreamDataPaths();
    Assertions.assertEquals(2, downStreamDataPaths.size());
    DataPath emptyFileResult = downStreamDataPaths.get(0);
    String entryPath = (String) emptyFileResult.getRecords().get(0).get(1);
    Assertions.assertEquals("zip/empty.txt", entryPath);
    DataPath fooFileResult = downStreamDataPaths.get(1);
    entryPath = (String) fooFileResult.getRecords().get(0).get(1);
    Assertions.assertEquals("zip/foo.txt", entryPath);
    //Assertions.assertEquals("foo.txt", downStreamDataPaths.get(1).getName());


  }

  @Test
  void yamlPipelineTest() throws URISyntaxException {


    Path flowYml = Paths.get(Objects.requireNonNull(UnZipPipelineStepTest.class.getResource("/manifest/unzip--pipeline.yml")).toURI());
    Pipeline pipeline = Pipeline
      .createFromYamlPath(tabular, flowYml);
    Assertions.assertEquals(2, pipeline.size());
    List<DataPath> dataPaths = pipeline.execute()
      .getDownStreamDataPaths();
    /**
     * Only zip/empty.txt is selected
     */
    Assertions.assertEquals(1, dataPaths.size());
    DataPath dataPath = dataPaths.get(0);
    Tabulars.print(dataPath);
    List<?> objects = dataPath.getRecords().get(0);
    /**
     * We strip the first part (ie zip)
     */
    Assertions.assertEquals("archive/foo.txt@tmp", objects.get(0));
    /**
     * The original entry path is intact
     */
    Assertions.assertEquals("zip/foo.txt", objects.get(1));

  }

}
