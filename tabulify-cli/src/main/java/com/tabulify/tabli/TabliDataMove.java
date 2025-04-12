package com.tabulify.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 */
public class TabliDataMove {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Move one or more data resources.",
      "",
      "A `move` operation will perform:",
      "  * a `rename` if the target connection and the source connection are the same",
      "  * a `copy` and `drop` of the source if the target connection and the source connection are not the same",
      "",
      "Note: This is just a alias command to the `transfer` command with the transfer operation option set to `move` ."
    );

    childCommand
      .addExample(
        "To move the table `time` from the data store `sqlite` into the file `time.csv`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " time@sqlite time.csv",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To move the file `foo.txt` to the `dir` directory  `dir/foo.txt`, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " foo.txt dir/foo.txt",
        CliUsage.CODE_BLOCK
      );

    TabliDataTransferManager.addAllTransferOptions(childCommand, TransferOperation.MOVE);
    CliParser cliParser = childCommand.parse();
    return new ArrayList<>(TabliDataTransferManager.runAndGetFeedBacks(tabular, cliParser, TabliDataTransferManager.TransferCommandType.DEFAULT));

  }

}
