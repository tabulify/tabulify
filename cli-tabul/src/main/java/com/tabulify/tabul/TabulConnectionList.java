package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 */
public class TabulConnectionList {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the parser
    childCommand
      .setDescription(
        "List the connections attributes in a tabular format"
      )
      .addExample("List all connections that starts with `sql` and add the `name` and `user` connection attributes to the output",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + TabulWords.ATTRIBUTE_OPTION + " name " +
          TabulWords.ATTRIBUTE_OPTION + " user sql*",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabulWords.NAME_SELECTORS)
      .setDescription("One ore several glob pattern that select connections by name")
      .setMandatory(false)
      .setDefaultValue("*");
    childCommand.addProperty(TabulWords.ATTRIBUTE_OPTION)
      .setDescription("A connection attribute to add to the output")
      .addDefaultValue(ConnectionAttributeEnumBase.NAME)
      .addDefaultValue(ConnectionAttributeEnumBase.URI);


    CliParser cliParser = childCommand.parse();

    final List<String> names = cliParser.getStrings(TabulWords.NAME_SELECTORS);

    final List<Connection> connections = tabular.selectConnections(names.toArray(new String[0]));
    Collections.sort(connections);

    // Creating a table to use the print function
    RelationDef connectionDef = tabular.getAndCreateRandomMemoryDataPath()
      .getOrCreateRelationDef();


    List<String> attributes = cliParser.getStrings(TabulWords.ATTRIBUTE_OPTION);
    for (String attribute : attributes) {
      connectionDef.addColumn(tabular.toPublicName(attribute));
    }

    try (InsertStream insertStream = connectionDef.getDataPath().getInsertStream()) {
      for (Connection connection : connections) {
        List<String> row = new ArrayList<>();
        for (String attributeString : attributes) {
          KeyNormalizer requestedAttributeNormalized = KeyNormalizer.createSafe(attributeString);
          // By default, not an attribute of this connection, empty string
          String value = "";
          for (Attribute attribute : connection.getAttributes()) {
            KeyNormalizer connectionAttributeNormalized = KeyNormalizer.createSafe(attribute.getAttributeMetadata());
            if (requestedAttributeNormalized.equals(connectionAttributeNormalized)) {
              value = attribute.getPublicValue();
              break;
            }
          }
          row.add(value);
        }
        insertStream.insert(row);
      }

    }


    return Collections.singletonList(connectionDef.getDataPath());

  }


}
