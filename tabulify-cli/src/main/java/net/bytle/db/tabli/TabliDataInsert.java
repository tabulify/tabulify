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
public class TabliDataInsert {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Insert one or more data resources into other data resources.",
      "",
      "Note: This is an alias command to the `transfer` command with the `insert` transfer operation."
    );

    childCommand
      .addExample(
        "To insert the table `time` from the data store `sqlite` into the file `time.csv`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " time@sqlite time.csv",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To insert the result of the Sql query `query_11.sql` into the Sql table `analytics`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (query_11.sql)@sqlite analytics@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To insert the file `foo.txt` into the file `bigfoo.txt`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " foo.txt bigfoo.txt",
        CliUsage.CODE_BLOCK
      );

    TabliDataTransferManager.addAllTransferOptions(childCommand, TransferOperation.INSERT);
    CliParser cliParser = childCommand.parse();
    return new ArrayList<>(TabliDataTransferManager.runAndGetFeedBacks(tabular, cliParser, TabliDataTransferManager.TransferCommandType.DEFAULT));

  }

}
