package net.bytle.db.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.connection.ConnectionAttribute;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.exception.NoVariableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 */
public class TabliConnectionList {

  private static final Logger LOGGER = LoggerFactory.getLogger(TabliConnectionList.class);


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the parser
    childCommand
      .setDescription(
        "List the connections attributes in a tabular format"
      )
      .addExample("List all connections that starts with `sql` and add the `name` and `user` connection attributes to the output",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + TabliWords.ATTRIBUTE_PROPERTY + " name " +
          TabliWords.ATTRIBUTE_PROPERTY + " user sql*",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabliWords.NAME_SELECTORS)
      .setDescription("One ore several glob pattern that select connections by name")
      .setMandatory(false)
      .setDefaultValue("*");
    childCommand.addProperty(TabliWords.ATTRIBUTE_PROPERTY)
      .setDescription("A connection attribute to add to the output")
      .addDefaultValue(ConnectionAttribute.NAME)
      .addDefaultValue(ConnectionAttribute.URI);


    CliParser cliParser = childCommand.parse();

    final List<String> names = cliParser.getStrings(TabliWords.NAME_SELECTORS);

    final List<Connection> connections = tabular.selectConnections(names.toArray(new String[0]));
    Collections.sort(connections);

    // Creating a table to use the print function
    RelationDef connectionDef = tabular.getAndCreateRandomMemoryDataPath()
      .getOrCreateRelationDef();

    List<String> attributes = cliParser.getStrings(TabliWords.ATTRIBUTE_PROPERTY);
    for (String attribute : attributes) {
      connectionDef.addColumn(attribute);
    }

    try (InsertStream insertStream = connectionDef.getDataPath().getInsertStream()) {
      for (Connection connection : connections) {

        List<String> row = new ArrayList<>();
        for (String attribute : attributes) {
          try {
            row.add((String) connection.getVariable(attribute).getValueOrDefaultOrNull());
          } catch (NoVariableException e) {
            // ok
          }
        }
        insertStream
          .insert(row);

      }
    }


    return Collections.singletonList(connectionDef.getDataPath());

  }


}
