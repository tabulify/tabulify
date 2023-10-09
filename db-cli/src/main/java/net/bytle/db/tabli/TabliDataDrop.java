package net.bytle.db.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.DropFunction;
import net.bytle.db.flow.step.ListCollector;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliWords.*;


public class TabliDataDrop {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Create the parser
    childCommand
      .setDescription("Drop data resource(s)")
      .addExample("To drop the tables D_TIME and F_SALES in the Oracle connection:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D_TIME@oracle F_SALES@oracle",
        CliUsage.CODE_BLOCK)
      .addExample("To drop only the table D_TIME with force (ie deleting the foreign keys constraint):",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + FORCE_FLAG + " D_TIME@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample("To drop all dimension tables that begins with (D_):",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D_*@sql-server",
        CliUsage.CODE_BLOCK)
      .addExample("To drop all tables from the current schema:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *@database",
        CliUsage.CODE_BLOCK
      );
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addFlag(TabliWords.NOT_STRICT_FLAG);
    childCommand.addFlag(FORCE_FLAG)
      .setDescription("if set, the foreign keys referencing the tables to drop will be dropped");
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY)
      .setDescription("if set, the foreign table referencing the tables will be dropped");

    // Args
    CliParser cliParser = childCommand.parse();
    final Boolean withForce = cliParser.getBoolean(FORCE_FLAG);
    final Set<DataUri> dataSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final Boolean withDependencies = cliParser.getBoolean(TabliWords.WITH_DEPENDENCIES_PROPERTY);
    final Boolean notStrict = cliParser.getBoolean(NOT_STRICT_FLAG);

    return new ArrayList<>(
      Pipeline
        .createFrom(
          tabular
        )
        .addStepToGraph(
          SelectSupplier.create()
            .setDataSelectors(dataSelectors)
            .setWithDependencies(withDependencies)
            .setIsStrict(!notStrict)
        )
        .addStepToGraph(
          DropFunction.create()
        )
        .addStepToGraph(
          ListCollector.create()
            .setTargetLogicalName("Dropped data resources")
        )
        .execute()
        .getDownStreamDataPaths()
    );

  }

}
