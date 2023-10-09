package net.bytle.db.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.CastStep;
import net.bytle.db.flow.step.EnrichStep;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.flow.step.StructStep;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliWords.*;


public class TabliDataStructure {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // The command
    childCommand.setDescription("Show the data structure of one or more data resources (s)");
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addFlag(TabliWords.NOT_STRICT_FLAG);
    childCommand.addProperty(ATTRIBUTE_PROPERTY);

    // Args
    final CliParser cliParser = childCommand.parse();

    final Boolean notStrict = cliParser.getBoolean(NOT_STRICT_FLAG);
    if (notStrict) {
      tabular.setStrict(false);
    }

    final Set<DataUri> dataUriSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);

    Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);
    Map<String, String> attributes = cliParser.getProperties(ATTRIBUTE_PROPERTY);
    return new ArrayList<>(
      Pipeline
        .createFrom(tabular)
        .addStepToGraph(
          SelectSupplier.create()
            .setWithDependencies(withDependencies)
            .setDataSelectors(dataUriSelectors)
            .setAttributes(attributes)
        )
        .addStepToGraph(
          CastStep.create()
        )
        .addStepToGraph(
          EnrichStep
            .create()
            .addVirtualColumns(virtualColumns)
        )
        .addStepToGraph(
          StructStep.create()
        )
        .execute()
        .getDownStreamDataPaths()
    );


  }


}
