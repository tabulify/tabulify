package net.bytle.db.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferOperation;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 */
public class TabliDataUpsert {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Merge one or more data resources into other data resources.",
      "",
      "Note: This is an alias command to the `transfer` command with the `merge` transfer operation."
    );

    childCommand
      .addExample(
        "To merge the result of the Sql query `query_11.sql` into the Sql table `analytics`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (query_11.sql)@sqlite analytics@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To merge the file `foo.csv` into the `sqlite` table `foo`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " foo.csv foo@sqlite",
        CliUsage.CODE_BLOCK
      );

    TabliDataTransferManager.addAllTransferOptions(childCommand,TransferOperation.UPSERT);
    CliParser cliParser = childCommand.parse();
    return new ArrayList<>(TabliDataTransferManager.runAndGetFeedBacks(tabular,cliParser, TabliDataTransferManager.TransferCommandType.DEFAULT));

  }

}
