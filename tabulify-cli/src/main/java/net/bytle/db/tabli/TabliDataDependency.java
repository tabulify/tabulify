package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.connection.ConnectionHowTos;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.DependencyStep;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.sample.BytleSchema;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.db.uri.DataUriString;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliWords.DATA_SELECTORS;
import static net.bytle.db.tabli.TabliWords.WITH_DEPENDENCIES_PROPERTY;


public class TabliDataDependency {


  public static List<DataPath> run(Tabular tabular, CliCommand cliCommand) {


    // Create the command
    cliCommand
      .setDescription("List the data dependencies (foreign key, view, ...)")
      .addExample(
        "To list the dependencies of the " + Tabular.TPCDS_CONNECTION + " connection:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(cliCommand) + " " + DataUriString.create().setPattern("*").setConnection(Tabular.TPCDS_CONNECTION),
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To list the foreign keys dependencies of the `sales` star schema asks the fact table `" + BytleSchema.TABLE_FACT_NAME + "` in the the " + ConnectionHowTos.SQLITE_CONNECTION_NAME + " connection and its dependencies :",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(cliCommand) + " " + WITH_DEPENDENCIES_PROPERTY + " " + DataUriString.create().setPattern(BytleSchema.TABLE_FACT_NAME).setConnection(ConnectionHowTos.SQLITE_CONNECTION_NAME),
        CliUsage.CODE_BLOCK
      );
    cliCommand.addArg(DATA_SELECTORS)
      .setMandatory(true);
    cliCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);

    // Parse and control the args
    CliParser cliParser = cliCommand.parse();
    final Set<DataUri> dataUriSelectors = cliParser.getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final Boolean withDependencies = cliParser.getBoolean(TabliWords.WITH_DEPENDENCIES_PROPERTY);

    return new ArrayList<>(
      Pipeline
        .createFrom(tabular)
        .addStepToGraph(
          SelectSupplier.create()
            .setWithDependencies(withDependencies)
            .setDataSelectors(dataUriSelectors)
        )
        .addStepToGraph(DependencyStep.create())
        .execute()
        .getDownStreamDataPaths());

  }

}
