package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.ListCollector;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.flow.step.TruncateFunction;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.cli.CliUsage.CODE_BLOCK;
import static net.bytle.db.tabli.TabliWords.*;


public class TabliDataTruncate {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the command
    childCommand
      .setDescription(
        "Truncate data resources(s) - ie remove all records/content from data resources"
      )
      .addExample(
        "To truncate the tables D_TIME and F_SALES:",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + "D_TIME@connection F_SALES@connection",
        CODE_BLOCK
      )
      .addExample(
        "To truncate only the table D_TIME with force (ie deleting the foreign keys constraint):",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + FORCE_FLAG + "D_TIME@database",
        CODE_BLOCK
      )
      .addExample(
        "To truncate all dimension tables that begins with (D_):",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D_*@connection",
        CODE_BLOCK
      )
      .addExample(
        "To truncate all tables from the current schema:",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *@database",
        CODE_BLOCK
      );
    childCommand.addArg(DATA_SELECTORS);

    childCommand.addFlag(TabliWords.FORCE_FLAG)
      .setDescription("Delete the foreign keys constraint")
      .setDefaultValue(false);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);

    // Args
    CliParser cliParser = childCommand.parse();
    final Boolean withForce = cliParser.getBoolean(FORCE_FLAG);
    final Set<DataUri> dataSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final Boolean withDependencies = cliParser.getBoolean(TabliWords.WITH_DEPENDENCIES_PROPERTY);


    Set<DataPath> dataPaths = Pipeline
      .createFrom(tabular)
      .addStepToGraph(
        SelectSupplier.create()
          .setDataSelectors(dataSelectors)
          .setWithDependencies(withDependencies)
          .setIsStrict(tabular.isStrict())
      )
      .addStepToGraph(
        TruncateFunction.create()
          .setWithForce(withForce)
      )
      .addStepToGraph(
        ListCollector.create()
          .setTargetLogicalName("truncated_data_resource")
          .setTargetDescription("The truncated data resources")
      )
      .execute()
      .getDownStreamDataPaths();

    return new ArrayList<>(dataPaths);

  }

}

