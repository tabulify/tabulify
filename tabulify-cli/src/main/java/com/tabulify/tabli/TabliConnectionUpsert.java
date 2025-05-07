package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabli.TabliConnection.addOrUpsertConnection;
import static com.tabulify.tabli.TabliWords.UPSERT_COMMAND;


/**
 * <p>
 */
public class TabliConnectionUpsert {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Define the command and its arguments
    childCommand
      .setDescription("Update or insert a connection")
      .addExample(
        "To upsert the information of the connection called `db`",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + TabliConnection.URI_PROPERTY + " jdbc:sqlite///tmp/db.db db",
        CliUsage.CODE_BLOCK
      );


    addOrUpsertConnection(tabular, childCommand, UPSERT_COMMAND);

    return new ArrayList<>();
  }

}


