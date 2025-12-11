package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.service.Service;
import com.tabulify.spi.DataPath;
import com.tabulify.sqlserver.SqlServerProvider;
import com.tabulify.stream.InsertStream;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.glob.Glob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 */
public class TabulServiceDrop {


    public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


        // Create the parser
        childCommand
                .setDescription(
                        "Drop one or more services instance from the service system\n" +
                                "In the case of docker service, drop the created container"
                )
          .addExample("For a docker service, drop the docker container " + SqlServerProvider.HOWTO_SQLSERVER_NAME,
                        CliUsage.CODE_BLOCK,
            CliUsage.getFullChainOfCommand(childCommand) + " " + SqlServerProvider.HOWTO_SQLSERVER_NAME,
                        CliUsage.CODE_BLOCK
                )
                .addExample("Drop all services instances that starts with sql",
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
            int servicesDropped = 0;
          for (String name : cliParser.getStrings(TabulWords.NAME_SELECTORS)) {
                Glob glob = Glob.createOf(name);
                for (Service service : tabular.getServices()) {
                  if (glob.matchesIgnoreCase(service.getName().toString())) {
                        servicesDropped++;
                        List<String> row = new ArrayList<>();
                        service.drop();
                    row.add(service.getName().toString());
                        row.add(service.getType());
                        insertStream.insert(row);
                    }
                }
            }
          feedbackDataResource.setComment(servicesDropped + " services were started");
        }


        return Collections.singletonList(feedbackDataResource);

    }


}
