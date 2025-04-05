package net.bytle.db.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.gen.GenDataPathType;
import net.bytle.db.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.tabli.TabliWords.*;


/**
 * load generated data in a table
 */
public class TabliDataFill {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand
      .setDescription(
        "Load generated data into data resources.",
        "",
        "You select the data resources to be filled with:",
        "   * the data selector argument (`" + TARGET_SELECTOR + "`)",
        "   * and optionally the dependency option (`" + WITH_DEPENDENCIES_PROPERTY + "`)",
        "",
        "For more control on the data generated, you can add the data resource generators (`*" + GenDataPathType.DATA_GEN.getExtension() + "`)",
        "by selecting them with the generator selector option (" + GENERATOR_SELECTOR + ")",
        "",
        "This is an alias to the `transfer` command where:",
        "  * the targets are the selected data resources",
        "  * the sources are generated from the generator data resources and/of target metadatas",
        "",
        "The default transfer operation is `" + TabliDataTransferManager.DEFAULT_FILL_TRANSFER_OPERATION + "`."
      )
      .addExample(
        "To load the table `D_TIME` from the connection `sqlite` with auto-generated data:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D_TIME@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To load the table `D_TIME` with the data generation file `D_TIME" + GenDataPathType.DATA_GEN.getExtension() + "` present in the current directory:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + GENERATOR_SELECTOR + " D_TIME D_TIME@connection",
        CliUsage.CODE_BLOCK

      )
      .addExample(
        "To load all data in the whole schema with the data generation file in the dir directory:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + GENERATOR_SELECTOR + " dir/* *@connection",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "Load auto generated data into the table F_SALES and its dependencies",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + "" + WITH_DEPENDENCIES_PROPERTY + " F_SALES@sqlite",
        CliUsage.CODE_BLOCK
      );


    TabliDataTransferManager.addFillTransferOptions(childCommand);
    CliParser cliParser = childCommand.parse();
    return new ArrayList<>(
      TabliDataTransferManager.runAndGetFeedBacks(
        tabular,
        cliParser,
        TabliDataTransferManager.TransferCommandType.FILL
      )
    );


  }


}
