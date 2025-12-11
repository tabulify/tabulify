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
public class TabulDataCopy {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Copy one or more data resources.",
      "",
      "Note: This is an alias command to the `transfer` command with the `copy` transfer operation."
    );

    childCommand
      .addExample(
        "To copy the table `time` from the data store `sqlite` into the file `time.csv`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " time@sqlite time.csv",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To copy the file `foo.txt` to the file `foo_copy.txt`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " foo.txt foo_copy.txt",
        CliUsage.CODE_BLOCK
      );

    return TabulDataTransferManager.config(
        tabular,
        childCommand,
        TransferOperation.COPY,
        TabulDataTransferCommandType.DEFAULT
      )
      .build()
      .run();

  }

}
