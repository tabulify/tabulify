package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.flow.operation.SummaryPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.DATA_SELECTORS;
import static com.tabulify.tabul.TabulWords.WITH_DEPENDENCIES_PROPERTY;


public class TabulDataSummary {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand
  ) {

    // Command
    childCommand.setDescription("Summarize a data resources collection and return min, max, avg on count and size attributes");
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);

    // Parser
    final CliParser cliParser = childCommand.parse();
    final List<DataUriNode> dataSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);


    return new ArrayList<>(
      Pipeline
        .builder(
          tabular)
        .addStep(
          SelectPipelineStep.builder()
            .setDataSelectors(dataSelectors)
            .setWithDependencies(withDependencies)
        )
        .addStep(
          SummaryPipelineStep.builder()
        )
        .build()
        .execute()
        .getDownStreamDataPaths()
    );

  }

}



