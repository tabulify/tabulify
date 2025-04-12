package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.connection.ConnectionHowTos;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.DependencyStep;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.sample.BytleSchema;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUri;
import com.tabulify.uri.DataUriString;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.DATA_SELECTORS;
import static com.tabulify.tabli.TabliWords.WITH_DEPENDENCIES_PROPERTY;


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
