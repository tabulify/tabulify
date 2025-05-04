package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.CastStep;
import com.tabulify.flow.step.EnrichStep;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.flow.step.StructStep;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUri;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.*;


public class TabliDataStructure {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // The command
    childCommand.setDescription("Show the data structure of one or more data resources (s)");
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addFlag(TabliWords.NOT_STRICT_FLAG);
    childCommand.addProperty(TABLI_ATTRIBUTE);

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
    Map<String, String> attributes = cliParser.getProperties(TABLI_ATTRIBUTE);
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
