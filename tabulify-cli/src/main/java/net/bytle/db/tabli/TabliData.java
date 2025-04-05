package net.bytle.db.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliLog.LOGGER_TABLI;
import static net.bytle.db.tabli.TabliWords.*;


/**
 *
 */
public class TabliData  {




  /**
   * @return
   */
  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    /**
     * Add the new commands
     */
    childCommand.addChildCommand(TabliWords.CREATE_COMMAND)
      .setDescription("Create a data resource");
    childCommand.addChildCommand(TabliWords.TRANSFER_COMMAND)
      .setDescription("Transfer a data resource ("+ Arrays.stream(TransferOperation.values()).map(TransferOperation::toString).collect(Collectors.joining(", "))+")");
    childCommand.addChildCommand(TabliWords.FILL_COMMAND)
      .setDescription("Fill a data resource with generated data");
    childCommand.addChildCommand(TabliWords.LIST_COMMAND)
      .setDescription("List data resources");
    childCommand.addChildCommand(TabliWords.DROP_COMMAND)
      .setDescription("Drop data resources");
    childCommand.addChildCommand(TabliWords.TRUNCATE_COMMAND)
      .setDescription("Truncate data resources");
    childCommand.addChildCommand(TabliWords.SUMMARY)
      .setDescription("Count the number of data resources");
    childCommand.addChildCommand(TabliWords.STRUCTURE_COMMAND)
      .setDescription("Show the data structure of data resources")
      .setShortName("struct");
    childCommand.addChildCommand(TabliWords.PRINT_COMMAND)
      .setDescription("Print the content of data resources");
    childCommand.addChildCommand(TabliWords.HEAD_COMMAND)
      .setDescription("Print the first content of data resources");
    childCommand.addChildCommand(TabliWords.TAIL_COMMAND)
      .setDescription("Print the last content of data resources");
    childCommand.addChildCommand(TabliWords.TEMPLATE_COMMAND)
      .setDescription("Create data resources from a template and a data source");
    childCommand.addChildCommand(TabliWords.QUERY_COMMAND)
      .setDescription("Test load your system with queries and fetch data resources (ie query, view, ...)");
    childCommand.addChildCommand(DEPENDENCY_COMMAND)
      .setDescription("Show the data dependencies")
      .setShortName("dep");
    childCommand.addChildCommand(INFO_COMMAND)
      .setDescription("Show the attributes of a data resource in a form fashion");
    childCommand.addChildCommand(COMPARE_COMMAND)
      .setDescription("Perform a diff operation against two data resources");
    childCommand.addChildCommand(COPY_COMMAND)
      .setDescription("Copy a data resource");
    childCommand.addChildCommand(MOVE_COMMAND)
      .setDescription("Move a data resource");
    childCommand.addChildCommand(INSERT_COMMAND)
      .setDescription("Insert data into a data resource");
    childCommand.addChildCommand(UPDATE_COMMAND)
      .setDescription("Update data from a data resource");
    childCommand.addChildCommand(UPSERT_COMMAND)
      .setDescription("Merge data into a data resource");
    childCommand.addChildCommand(DELETE_COMMAND)
      .setShortName("del")
      .setDescription("Delete data from a data resource");

    /**
     * Parse
     */
    CliParser cliParser = childCommand.parse();

    /**
     * Check for a new command
     */
    List<DataPath> feedbackDataPaths = new ArrayList<>();
    List<CliCommand> subChildCommands = cliParser.getFoundedChildCommands();
    if (subChildCommands.size() == 0) {
      throw new IllegalArgumentException("A known command must be given for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ").");
    } else {
      for (CliCommand subChildCommand : subChildCommands) {
        LOGGER_TABLI.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case TRANSFER_COMMAND:
            feedbackDataPaths = TabliDataTransfer.run(tabular, subChildCommand);
            break;
          case FILL_COMMAND:
            feedbackDataPaths = TabliDataFill.run(tabular, subChildCommand);
            break;
          case STRUCTURE_COMMAND:
            feedbackDataPaths = TabliDataStructure.run(tabular, subChildCommand);
            break;
          case PRINT_COMMAND:
            feedbackDataPaths = TabliDataPrint.run(tabular, subChildCommand);
            break;
          case HEAD_COMMAND:
            feedbackDataPaths = TabliDataHead.run(tabular, subChildCommand);
            break;
          case TAIL_COMMAND:
            feedbackDataPaths = TabliDataTail.run(tabular, subChildCommand);
            break;
          case LIST_COMMAND:
            feedbackDataPaths = TabliDataList.run( tabular, subChildCommand);
            break;
          case INFO_COMMAND:
            feedbackDataPaths = TabliDataInfo.run(tabular, subChildCommand);
            break;
          case DROP_COMMAND:
            feedbackDataPaths = TabliDataDrop.run(tabular, subChildCommand);
            break;
          case TRUNCATE_COMMAND:
            feedbackDataPaths = TabliDataTruncate.run(tabular, subChildCommand);
            break;
          case SUMMARY:
            feedbackDataPaths = TabliDataSummary.run(tabular, subChildCommand);
            break;
          case CREATE_COMMAND:
            feedbackDataPaths = TabliDataCreate.run(tabular, subChildCommand);
            break;
          case COMPARE_COMMAND:
            feedbackDataPaths = TabliDataCompare.run(tabular, subChildCommand);
            break;
          case QUERY_COMMAND:
            feedbackDataPaths = TabliDataQuery.run(tabular, subChildCommand);
            break;
          case DEPENDENCY_COMMAND:
            feedbackDataPaths = TabliDataDependency.run(tabular, subChildCommand);
            break;
          case COPY_COMMAND:
            feedbackDataPaths = TabliDataCopy.run(tabular, subChildCommand);
            break;
          case MOVE_COMMAND:
            feedbackDataPaths = TabliDataMove.run(tabular, subChildCommand);
            break;
          case INSERT_COMMAND:
            feedbackDataPaths = TabliDataInsert.run(tabular, subChildCommand);
            break;
          case UPDATE_COMMAND:
            feedbackDataPaths = TabliDataUpdate.run(tabular, subChildCommand);
            break;
          case UPSERT_COMMAND:
            feedbackDataPaths = TabliDataUpsert.run(tabular, subChildCommand);
            break;
          case DELETE_COMMAND:
            feedbackDataPaths = TabliDataDelete.run(tabular, subChildCommand);
            break;
          case TEMPLATE_COMMAND:
            feedbackDataPaths = TabliDataTemplate.run(tabular, subChildCommand);
            break;
          default:
            // Should never be there if the commands are well defined
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command ("+ CliUsage.getFullChainOfCommand(childCommand)+")");

        }
      }


      return feedbackDataPaths;
    }

  }
}
