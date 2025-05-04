package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabli.TabliWords.ADD_COMMAND;


/**
 * <p>
 */
public class TabliConnectionAdd {

  protected static final String DRIVER_PROPERTY = "--driver";
  protected static final String URI_ARGUMENT = "uri";
  protected static final String USER_PROPERTY = "--user";
  protected static final String PASSWORD_PROPERTY = "--password";



  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the parser
    childCommand
      .setDescription("Add a connection")
      .addExample("To add the connection `name` with the URI `uri`:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " name uri",
        CliUsage.CODE_BLOCK
      );


    TabliConnection.addOrUpsertConnection(tabular, childCommand, ADD_COMMAND);

    return new ArrayList<>();

  }

}


