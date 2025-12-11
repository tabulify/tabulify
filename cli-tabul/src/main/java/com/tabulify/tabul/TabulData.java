package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;
import static com.tabulify.tabul.TabulWords.*;


/**
 *
 */
public class TabulData {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    /**
     * Add the new commands
     */
    childCommand.addChildCommand(TabulWords.CREATE_COMMAND)
      .setDescription("Create a data resource");
    childCommand.addChildCommand(TabulWords.CONCAT_COMMAND)
      .setShortName("cat")
      .setDescription("Concatenate data resources");
    childCommand.addChildCommand(TabulWords.TRANSFER_COMMAND)
      .setDescription("Transfer a data resource (" + Arrays.stream(TransferOperation.values()).map(TransferOperation::toString).collect(Collectors.joining(", ")) + ")");
    childCommand.addChildCommand(TabulWords.FILL_COMMAND)
      .setDescription("Fill a data resource with generated data");
    childCommand.addChildCommand(TabulWords.LIST_COMMAND)
      .setShortName("ls")
      .setDescription("List data resources");
    childCommand.addChildCommand(UNZIP_COMMAND)
      .setDescription("Unzip archive data resources");
    childCommand.addChildCommand(TabulWords.DROP_COMMAND)
      .setShortName("rm")
      .setDescription("Drop data resources");
    childCommand.addChildCommand(TabulWords.TRUNCATE_COMMAND)
      .setShortName("trunc")
      .setDescription("Truncate data resources");
    childCommand.addChildCommand(TabulWords.SUMMARIZE_COMMAND)
      .setDescription("Count the number of data resources");
    childCommand.addChildCommand(TabulWords.DESCRIBE_COMMAND)
      .setDescription("Show the data structure of data resources")
      .setShortName("desc");
    childCommand.addChildCommand(TabulWords.PRINT_COMMAND)
      .setDescription("Print the content of data resources");
    childCommand.addChildCommand(TabulWords.DIFF_COMMAND)
      .setDescription("Compare data resources");
    childCommand.addChildCommand(TabulWords.HEAD_COMMAND)
      .setDescription("Print the first content of data resources");
    childCommand.addChildCommand(TabulWords.TAIL_COMMAND)
      .setDescription("Print the last content of data resources");
    childCommand.addChildCommand(TabulWords.TEMPLATE_COMMAND)
      .setDescription("Create data resources from a template and a data source");
    childCommand.addChildCommand(TabulWords.EXECUTE_COMMAND)
      .setShortName("exec")
      .setDescription("Execute one or more executable in a single, batch or performance fashion");
    childCommand.addChildCommand(DEPENDENCY_COMMAND)
      .setDescription("Show the data dependencies")
      .setShortName("dep");
    childCommand.addChildCommand(INFO_COMMAND)
      .setDescription("Show the attributes of a data resource in a form fashion");
    childCommand.addChildCommand(DIFF_COMMAND)
      .setDescription("Perform a diff operation against two data resources");
    childCommand.addChildCommand(COPY_COMMAND)
      .setDescription("Copy a data resource");
    childCommand.addChildCommand(MOVE_COMMAND)
      .setDescription("Move a data resource");
    childCommand.addChildCommand(REPLACE_COMMAND)
      .setDescription("Replace a data resource");
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
    if (subChildCommands.isEmpty()) {
      throw new IllegalCommandException("A known command must be given for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ").");
    }
    for (CliCommand subChildCommand : subChildCommands) {
      LOGGER_TABUL.info("The command (" + subChildCommand + ") was found");
      switch (subChildCommand.getName()) {
        case TRANSFER_COMMAND:
          feedbackDataPaths = TabulDataTransfer.run(tabular, subChildCommand);
          break;
        case FILL_COMMAND:
          feedbackDataPaths = TabulDataFill.run(tabular, subChildCommand);
          break;
        case DESCRIBE_COMMAND:
          feedbackDataPaths = TabulDataDescribe.run(tabular, subChildCommand);
          break;
        case PRINT_COMMAND:
          feedbackDataPaths = TabulDataPrint.run(tabular, subChildCommand);
          break;
        case HEAD_COMMAND:
          feedbackDataPaths = TabulDataHead.run(tabular, subChildCommand);
          break;
        case TAIL_COMMAND:
          feedbackDataPaths = TabulDataTail.run(tabular, subChildCommand);
          break;
        case LIST_COMMAND:
          feedbackDataPaths = TabulDataList.run(tabular, subChildCommand);
          break;
        case INFO_COMMAND:
          feedbackDataPaths = TabulDataInfo.run(tabular, subChildCommand);
          break;
        case DROP_COMMAND:
          feedbackDataPaths = TabulDataDrop.run(tabular, subChildCommand);
          break;
        case TRUNCATE_COMMAND:
          feedbackDataPaths = TabulDataTruncate.run(tabular, subChildCommand);
          break;
        case SUMMARIZE_COMMAND:
          feedbackDataPaths = TabulDataSummary.run(tabular, subChildCommand);
          break;
        case CREATE_COMMAND:
          feedbackDataPaths = TabulDataCreate.run(tabular, subChildCommand);
          break;
        case DIFF_COMMAND:
          feedbackDataPaths = TabulDataDiff.run(tabular, subChildCommand);
          break;
        case TabulWords.EXECUTE_COMMAND:
          feedbackDataPaths = TabulDataExec.run(tabular, subChildCommand);
          break;
        case DEPENDENCY_COMMAND:
          feedbackDataPaths = TabulDataDependency.run(tabular, subChildCommand);
          break;
        case CONCAT_COMMAND:
          feedbackDataPaths = TabulDataConcat.run(tabular, subChildCommand);
          break;
        case COPY_COMMAND:
          feedbackDataPaths = TabulDataCopy.run(tabular, subChildCommand);
          break;
        case MOVE_COMMAND:
          feedbackDataPaths = TabulDataMove.run(tabular, subChildCommand);
          break;
        case REPLACE_COMMAND:
          feedbackDataPaths = TabulDataReplace.run(tabular, subChildCommand);
          break;
        case INSERT_COMMAND:
          feedbackDataPaths = TabulDataInsert.run(tabular, subChildCommand);
          break;
        case UNZIP_COMMAND:
          feedbackDataPaths = TabulDataUnZip.run(tabular, subChildCommand);
          break;
        case UPDATE_COMMAND:
          feedbackDataPaths = TabulDataUpdate.run(tabular, subChildCommand);
          break;
        case UPSERT_COMMAND:
          feedbackDataPaths = TabulDataUpsert.run(tabular, subChildCommand);
          break;
        case DELETE_COMMAND:
          feedbackDataPaths = TabulDataDelete.run(tabular, subChildCommand);
          break;
        case TEMPLATE_COMMAND:
          feedbackDataPaths = TabulDataTemplate.run(tabular, subChildCommand);
          break;
        default:
          // Should never be there if the commands are well-defined
          throw new IllegalCommandException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");

      }
    }
    return feedbackDataPaths;

  }
}
