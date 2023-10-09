package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.flow.step.SummaryStep;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliWords.DATA_SELECTORS;
import static net.bytle.db.tabli.TabliWords.WITH_DEPENDENCIES_PROPERTY;


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



