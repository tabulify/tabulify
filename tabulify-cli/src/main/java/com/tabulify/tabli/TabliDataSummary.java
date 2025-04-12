package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.flow.step.SummaryStep;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.DATA_SELECTORS;
import static com.tabulify.tabli.TabliWords.WITH_DEPENDENCIES_PROPERTY;


public class TabliDataSummary {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand
  ) {

    // Command
    childCommand.setDescription("Summarize a data resources collection and return min, max, avg on count and size attributes");
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);

    // Parser
    final CliParser cliParser = childCommand.parse();
    final Set<DataUri> dataSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);


    return new ArrayList<>(
      Pipeline
        .createFrom(
          tabular)
        .addStepToGraph(
          SelectSupplier.create()
            .setDataSelectors(dataSelectors)
            .setWithDependencies(withDependencies)
        )
        .addStepToGraph(
          SummaryStep.create()
        )
        .execute()
        .getDownStreamDataPaths()
    );

  }

}



