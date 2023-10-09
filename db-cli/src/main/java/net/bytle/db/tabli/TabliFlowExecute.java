package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliWords.NOT_STRICT_FLAG;

public class TabliFlowExecute {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription("Run one or more pipeline scripts");

    childCommand.addArg(TabliWords.PIPELINE_SELECTORS)
      .setDescription("One or several data selectors that select(s) pipeline file(s)")
      .setValueName("pipeline-selector...")
      .setMandatory(true);


    childCommand.addFlag(NOT_STRICT_FLAG)
      .setDescription("When present no error will be raised, if no flow file has been selected")
      .setDefaultValue(true);

    CliParser cliParser = childCommand.parse();

    Set<DataUri> playSelectors = cliParser.getStrings(TabliWords.PIPELINE_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());

    final Boolean isStrict = cliParser.getBoolean(NOT_STRICT_FLAG);

    List<DataPath> feedBacks = new ArrayList<>();
    Set<DataPath> dataPaths = tabular.select(playSelectors, isStrict, MediaTypes.TEXT_PLAIN);
    List<DataPath> dataPathsList = dataPaths
      .stream()
      .sorted()
      .collect(Collectors.toList());
    for (DataPath dataPath : dataPathsList) {
      if (!dataPath.getMediaType().isContainer()) {
        FsDataPath fsDataPath = (FsDataPath) dataPath;
        Path path = fsDataPath.getAbsoluteNioPath();
        try (Pipeline pipeline = Pipeline.createFromYamlPath(tabular, path)) {
          System.out.println("########");
          System.out.println("Executing the pipeline "+pipeline.getLogicalName());
          System.out.println("########");
          DataPath feedBack = pipeline
            .execute()
            .getRunsByStepDataPath();
          feedBacks.add(feedBack);
        }
      } else {
        throw new UnsupportedOperationException("Only file resources are supported as flow. " + dataPath + " is not a file but a " + dataPath.getMediaType());
      }

    }


    return feedBacks;

  }
}
