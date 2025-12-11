package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferResourceOperations;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliUsage;

import java.util.List;


/**
 * <p>
 */
public class TabulDataReplace {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Replace one or more data resources.",
      "",
      "A `replace` operation will perform:",
      "  * a `drop` of the target if it exists",
      "  * a `copy` of the source ",
      "",
      "Note: This is just a alias command to the `transfer` command with the transfer operation option set to `copy` with a drop of the target."
    );

    childCommand
      .addExample(
        "To replace the file `time.csv` with the content of the `sqlite` table `time`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " time@sqlite time.csv",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To replace the file `dir/foo.txt` with the content of the file `foo.txt`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " foo.txt dir/foo.txt",
        CliUsage.CODE_BLOCK
      );

    return TabulDataTransferManager
      .config(tabular, childCommand, TransferOperation.COPY, TabulDataTransferCommandType.DEFAULT)
      .setTargetOperation(TransferResourceOperations.DROP)
      .build()
      .run();

  }

}
