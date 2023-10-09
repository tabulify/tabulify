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
public class TabliDataCopy {


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

    TabliDataTransferManager.addAllTransferOptions(childCommand, TransferOperation.COPY);
    CliParser cliParser = childCommand.parse();
    return new ArrayList<>(TabliDataTransferManager.runAndGetFeedBacks(tabular, cliParser, TabliDataTransferManager.TransferCommandType.DEFAULT));

  }

}
