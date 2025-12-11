package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliUsage;

import java.util.List;

import static com.tabulify.tabul.TabulWords.TARGET_DATA_URI;


/**
 * <p>
 */
public class TabulDataConcat {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Concatenate data resources"
    );

    childCommand
      .addExample(
        "To concatenate all `log*.txt` files in the current directory to a `log` table located in sqlite, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " log*.txt@cd log@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "In the current directory to concatenate the files `visit1.csv` and `visit2.csv` to a `visits.csv` file, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " visit1.csv visit2.csv visits.csv",
        CliUsage.CODE_BLOCK
      );

    TabulDataTransferManager tabulDataTransferManager =
      TabulDataTransferManager
        .config(tabular, childCommand, TransferOperation.INSERT, TabulDataTransferCommandType.CONCAT)
        .build();


    List<DataPath> dataPaths = tabulDataTransferManager.run();

    /**
     * Single source cat mode (ie shortcut to print text file without headers, in pipe mode)
     */
    String targetDataUri = tabulDataTransferManager.getParser().getString(TARGET_DATA_URI);
    if (targetDataUri == null) {
      return List.of();
    }

    return dataPaths;

  }

}
