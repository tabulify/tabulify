package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.exec.PingTaskExecutor;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabul.TabulConnectionInfo.CONNECTION_NAMES;


public class TabulConnectionPing {

  public static final int DEFAULT_TIMEOUT_VALUE = 10;

  public static List<DataPath> run(Tabular tabular, CliCommand subChildCommand) {

    subChildCommand.setDescription("Ping a connection");

    subChildCommand.addArg(CONNECTION_NAMES)
      .setDescription("one or more connection name")
      .setMandatory(true);
    subChildCommand.addProperty(TabulWords.TIMEOUT_PROPERTY)
      .setDescription("timeout in second to wait until a successful connection")
      .setDefaultValue(DEFAULT_TIMEOUT_VALUE)
      .setMandatory(false);

    CliParser cliParser = subChildCommand.parse();

    Integer durationSeconds = cliParser.getInteger(TabulWords.TIMEOUT_PROPERTY);

    // Retrieve
    List<String> connectionNames = cliParser.getStrings(CONNECTION_NAMES);
    final List<Connection> connections = tabular.selectConnections(connectionNames.toArray(new String[0]));
    for (Connection connection : connections) {
      PingTaskExecutor.ExecutionResult pingTaskResult = PingTaskExecutor.executeAtInterval(() -> {
        boolean pingResult = connection.ping();
        if (pingResult) {
          System.out.println("The connection (" + connection + ") has been pinged successfully");
        } else {
          System.out.println("The connection (" + connection + ") could not be pinged");
        }
        return pingResult;
      }, 1, durationSeconds, true);

      if (pingTaskResult.getSuccessCount() == 0) {
        tabular.setExitStatus(1);
      }

    }

    return new ArrayList<>();
  }


}
