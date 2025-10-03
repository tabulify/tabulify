package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;

import java.util.List;


/**
 * <p>
 */
public class TabulDataUpdate {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Update one or more data resources with other data resources.",
      "",
      "Note: This is an alias command to the `transfer` command with the `update` transfer operation."
    );

    childCommand
      .addExample(
        "To update the result of the Sql query `query_11.sql` into the Sql table `analytics`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (query_11.sql)@sqlite analytics@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To update the file `foo.csv` into the `sqlite` table `foo`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " foo.csv foo@sqlite",
        CliUsage.CODE_BLOCK
      );

    return TabulDataTransferManager.config(
        tabular,
        childCommand,
        TransferOperation.UPDATE,
        TabulDataTransferCommandType.DEFAULT
      )
      .build()
      .run();

  }

}
