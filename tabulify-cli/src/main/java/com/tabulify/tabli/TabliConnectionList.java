package com.tabulify.tabli;


import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.exception.CastException;
import net.bytle.exception.NoValueException;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <p>
 */
public class TabliConnectionList {


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
      .addDefaultValue(ConnectionAttributeEnumBase.NAME)
      .addDefaultValue(ConnectionAttributeEnumBase.URI);


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

        Map<KeyNormalizer, String> connectionAttributes = connection.getAttributes()
          .stream()
          .collect(
            Collectors.toMap(
              a -> KeyNormalizer.create(a.getAttributeMetadata()),
              a -> {
                try {
                  return a.getValueOrDefaultCastAs(String.class);
                } catch (NoValueException | CastException e) {
                  return "";
                }
              }
            )
          );
        List<String> row = new ArrayList<>();
        for (String attributeString : attributes) {
          row.add(connectionAttributes.get(KeyNormalizer.create(attributeString)));
        }
        insertStream
          .insert(row);

      }
    }


    return Collections.singletonList(connectionDef.getDataPath());

  }


}
