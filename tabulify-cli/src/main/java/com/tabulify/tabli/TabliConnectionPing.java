package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabli.TabliConnectionInfo.CONNECTION_NAMES;

public class TabliConnectionPing {
  public static List<DataPath> run(Tabular tabular, CliCommand subChildCommand) {

    subChildCommand.setDescription("Ping a connection");

    subChildCommand.addArg(CONNECTION_NAMES)
      .setDescription("one or more connection name")
      .setMandatory(true);

    CliParser cliParser = subChildCommand.parse();


    // Retrieve
    List<String> connectionNames = cliParser.getStrings(CONNECTION_NAMES);
    final List<Connection> connections = tabular.selectConnections(connectionNames.toArray(new String[0]));
    for (Connection connection : connections) {
       Boolean result = connection.ping();
       if (result){
         System.out.println();
         System.out.println("The connection ("+connection+") has been pinged successfully");
         System.out.println();
       } else {
         System.out.println("The connection ("+connection+") could not be pinged");
         tabular.setExitStatus(1);
       }
    }


    return new ArrayList<>();
  }
}
