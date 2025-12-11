package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.DependencyPipelineStep;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.sample.BytleSchema;
import com.tabulify.spi.DataPath;
import com.tabulify.sqlite.SqliteProvider;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;


public class TabulDataDependency {


  public static List<DataPath> run(Tabular tabular, CliCommand cliCommand) {


    // Create the command
    cliCommand
      .setDescription("List the data dependencies (foreign key, view, ...)")
      .addExample(
        "To list the dependencies of the " + ConnectionBuiltIn.TPCDS_CONNECTION + " connection:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(cliCommand) + " " + DataUriStringNode
          .builder()
          .setPattern("*")
          .setConnection(ConnectionBuiltIn.TPCDS_CONNECTION)
          .build(),
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To list the foreign keys dependencies of the `sales` star schema asks the fact table `" + BytleSchema.TABLE_FACT_NAME + "` in the the " + SqliteProvider.HOWTO_SQLITE_NAME + " connection and its dependencies :",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(cliCommand) + " " + WITH_DEPENDENCIES_PROPERTY + " " + DataUriStringNode.builder()
          .setPattern(BytleSchema.TABLE_FACT_NAME)
          .setConnection(SqliteProvider.HOWTO_SQLITE_NAME)
          .build(),
        CliUsage.CODE_BLOCK
      );
    cliCommand.addArg(DATA_SELECTORS).setMandatory(true);
    cliCommand.addFlag(STRICT_SELECTION);
    cliCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);

    // Parse and control the args
    CliParser cliParser = cliCommand.parse();
    final List<DataUriNode> dataUriSelectors = cliParser.getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());
    final Boolean withDependencies = cliParser.getBoolean(TabulWords.WITH_DEPENDENCIES_PROPERTY);
    Boolean isStrictSelection = cliParser.getBoolean(STRICT_SELECTION);

    List<DataPath> downStreamDataPaths = Pipeline
      .builder(tabular)
      .addStep(
        SelectPipelineStep.builder()
          .setWithDependencies(withDependencies)
          .setDataSelectors(dataUriSelectors)
          .setStrictSelection(isStrictSelection)
      )
      .addStep(
        DependencyPipelineStep
          .builder()
      )
      .build()
      .execute()
      .getDownStreamDataPaths();
    if (downStreamDataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(dataUriSelectors);
    }
    return new ArrayList<>(downStreamDataPaths);

  }

}
