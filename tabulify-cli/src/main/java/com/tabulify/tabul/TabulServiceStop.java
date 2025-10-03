package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.service.Service;
import com.tabulify.spi.DataPath;
import com.tabulify.sqlserver.SqlServerProvider;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.regexp.Glob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 */
public class TabulServiceStop {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the parser
    childCommand
      .setDescription(
        "Stop one or more services"
      )
      .addExample("Stop the service " + SqlServerProvider.HOWTO_SQLSERVER_NAME,
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + SqlServerProvider.HOWTO_SQLSERVER_NAME,
        CliUsage.CODE_BLOCK
      )
      .addExample("Stop all services that starts with sql",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " sql*",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabulWords.NAME_SELECTORS)
      .setDescription("One ore several glob pattern that select services by name")
      .setMandatory(true);


    CliParser cliParser = childCommand.parse();

    // The feedback data resource
    DataPath feedbackDataResource = tabular
      .getAndCreateRandomMemoryDataPath()
      .getOrCreateRelationDef()
      .addColumn("Name")
      .addColumn("Type")
      .getDataPath();

    try (InsertStream insertStream = feedbackDataResource.getInsertStream()) {
      int servicesStopped = 0;
      for (String name : cliParser.getStrings(TabulWords.NAME_SELECTORS)) {
        Glob glob = Glob.createOf(name);
        for (Service service : tabular.getServices()) {
          if (glob.matchesIgnoreCase(service.getName().toString())) {
            servicesStopped++;
            List<String> row = new ArrayList<>();
            service.stop();
            row.add(service.getName().toString());
            row.add(service.getType());
            insertStream.insert(row);
          }
        }
      }
      String description = servicesStopped + " services were";
      if (servicesStopped == 1) {
        description = servicesStopped + " service was";
      }
      feedbackDataResource.setComment(description + " stopped");
    }


    return Collections.singletonList(feedbackDataResource);

  }


}
