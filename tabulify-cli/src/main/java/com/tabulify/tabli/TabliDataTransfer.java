package com.tabulify.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;

import java.util.ArrayList;
import java.util.List;



public class TabliDataTransfer  {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Transfer one or more data resources."
    );
    childCommand.addExample(
      "To download the table `time` from the data store `sqlite` into the file `time.csv`, you would execute",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " time@sqlite time.csv",
      CliUsage.CODE_BLOCK
    );
    childCommand.addExample(
      "To download all the table that starts with `dim` from the data store `oracle` into the directory `dim`, you would execute",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " dim*@oracle dim",
      CliUsage.CODE_BLOCK
    );
    childCommand.addExample(
      "To download the data of the query defined in the file `QueryToDownload.sql` and executed against the data store `sqlite` into the file `QueryData.csv`, you would execute the following command:",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (QueryToDownload.sql)@sqlite QueryData.csv",
      CliUsage.CODE_BLOCK
    );
    childCommand.addExample(
      "To download the data of all query defined in all `sql` files of the current directory, execute them against the data store `sqlite` and save the results into the directory `result`, you would execute the following command:",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (*.sql)@sqlite result",
      CliUsage.CODE_BLOCK
    );
    childCommand.addExample("Transfer the result of the query `top10product` from sqlite to the table `top10product` of sql server",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (top10product.sql)@sqlite @sqlserver ",
      CliUsage.CODE_BLOCK
    );

    TabliDataTransferManager.addAllTransferOptions(childCommand, TransferOperation.INSERT);
    CliParser cliParser = childCommand.parse();
    return new ArrayList<>(TabliDataTransferManager.runAndGetFeedBacks(tabular,cliParser, TabliDataTransferManager.TransferCommandType.DEFAULT));

  }



}
