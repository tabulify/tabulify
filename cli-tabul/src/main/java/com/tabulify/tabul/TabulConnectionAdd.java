package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliUsage;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabul.TabulWords.ADD_COMMAND;


/**
 * <p>
 */
public class TabulConnectionAdd {

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


    TabulConnection.addOrUpsertConnection(tabular, childCommand, ADD_COMMAND);

    return new ArrayList<>();

  }

}
