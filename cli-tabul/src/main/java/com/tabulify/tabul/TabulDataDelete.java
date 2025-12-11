package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliUsage;

import java.util.List;


/**
 * <p>
 */
public class TabulDataDelete {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Delete records in a target resources defined in a source",
      "",
      "Note:",
      "This is an alias command to the `transfer` command with the `delete` transfer operation.",
      "",
      "This action will not execute a SQL DELETE statement. SQL statements are defined and executed with a SQL script."
    );

    childCommand
      .addExample(
        "To delete the records of the file `foo.csv` from the `sqlite` table `foo`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " foo.csv foo@sqlite",
        CliUsage.CODE_BLOCK
      );


    return TabulDataTransferManager.config(
        tabular,
        childCommand,
        TransferOperation.DELETE,
        TabulDataTransferCommandType.DEFAULT)
      .build()
      .run();

  }

}
