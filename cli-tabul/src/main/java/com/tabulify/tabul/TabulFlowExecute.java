package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.engine.PipelineResult;
import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.type.MediaTypes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.NO_RESULTS;
import static com.tabulify.tabul.TabulWords.NO_STRICT_SELECTION;

public class TabulFlowExecute {

  public static final Logger LOGGER = Logger.getLogger(TabulFlowExecute.class.getName());

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription("Run one or more pipeline scripts");

    childCommand.addArg(TabulWords.PIPELINE_SELECTORS)
      .setDescription("One or several data selectors that select(s) pipeline file(s)")
      .setValueName("pipeline-selector...")
      .setMandatory(true);


    childCommand.addFlag(NO_STRICT_SELECTION)
      .setDescription("When present no error will be raised, if no flow file has been selected")
      .setDefaultValue(true);

    childCommand.addFlag(NO_RESULTS)
      .setDescription("When present no pipeline results will be added")
      .setDefaultValue(true);

    CliParser cliParser = childCommand.parse();

    List<DataUriNode> playSelectors = cliParser.getStrings(TabulWords.PIPELINE_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());

    final Boolean isStrict = cliParser.getBoolean(NO_STRICT_SELECTION);
    final Boolean showResults = cliParser.getBoolean(NO_RESULTS);

    List<DataPath> feedBacks = new ArrayList<>();
    List<DataPath> dataPaths = tabular.select(playSelectors, isStrict, MediaTypes.TEXT_PLAIN);
    List<DataPath> dataPathsList = dataPaths
      .stream()
      .sorted()
      .collect(Collectors.toList());
    for (DataPath dataPath : dataPathsList) {
      if (!dataPath.getMediaType().isContainer()) {
        FsDataPath fsDataPath = (FsDataPath) dataPath;
        Path path = fsDataPath.getAbsoluteNioPath();
        Pipeline pipeline = Pipeline.createFromYamlPath(tabular, path);
        LOGGER.info("########");
        LOGGER.info("Executing the pipeline " + pipeline.getNodeName());
        LOGGER.info("########");
        PipelineResult pipelineResult = pipeline.execute();
        if (showResults) {
          feedBacks.addAll(
            pipelineResult
              .getAllResultsAsDataPath()
          );
        }
      } else {
        throw new UnsupportedOperationException("Only file resources are supported as flow. " + dataPath + " is not a file but a " + dataPath.getMediaType());
      }

    }


    return feedBacks;

  }
}
