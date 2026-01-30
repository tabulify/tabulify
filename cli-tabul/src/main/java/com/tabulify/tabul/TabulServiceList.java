package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.model.RelationDef;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceAttributeBase;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.glob.Glob;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.Sorts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * <p>
 */
public class TabulServiceList {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the parser
    childCommand
      .setDescription(
        "List the services"
      )
      .addExample("List all services",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand),
        CliUsage.CODE_BLOCK
      )
      .addExample("Start all services that starts with sql",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " sql*",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabulWords.NAME_SELECTORS)
      .setDescription("One ore several glob pattern that select services by name")
      .setMandatory(false)
      .setDefaultValue("*");
    childCommand.addProperty(TabulWords.ATTRIBUTE_OPTION)
      .setDescription("A connection attribute to add to the output")
      .addDefaultValue(ServiceAttributeBase.NAME)
      .addDefaultValue(ServiceAttributeBase.TYPE);


    CliParser cliParser = childCommand.parse();

    // The feedback data resource
    RelationDef feedbackDataResourceDef = tabular.getAndCreateRandomMemoryDataPath().getOrCreateRelationDef();

    List<String> attributes = cliParser.getStrings(TabulWords.ATTRIBUTE_OPTION);
    for (String attribute : attributes) {
      feedbackDataResourceDef.addColumn(tabular.toPublicName(attribute));
    }
    if (attributes.size() != feedbackDataResourceDef.getColumnsSize()) {
      throw new IllegalArgumentException("The attribute option (" + TabulWords.ATTRIBUTE_OPTION + ") defines the same column multiple time. Values: " + String.join(", ", attributes));
    }


    DataPath feedbackDataPath = feedbackDataResourceDef.getDataPath();
    try (InsertStream insertStream = feedbackDataPath.getInsertStream()) {
      int servicesFound = 0;
      for (String name : cliParser.getStrings(TabulWords.NAME_SELECTORS)) {

        Glob glob = Glob.createOf(name);
        for (Service service : tabular.getServices()
          .stream()
          .sorted((s1, s2) -> Sorts.naturalSortComparator(s1.getName().toString(), s2.getName().toString()))
          .collect(Collectors.toList())
        ) {
          if (glob.matchesIgnoreCase(service.getName().toString())) {
            List<String> row = new ArrayList<>();
            servicesFound++;
            for (String attributeString : attributes) {
              KeyNormalizer requestedAttributeNormalized = KeyNormalizer.createSafe(attributeString);
              // By default, not an attribute of this service, empty string
              String value = "";
              for (Attribute attribute : service.getAttributes()) {
                KeyNormalizer connectionAttributeNormalized = KeyNormalizer.createSafe(attribute.getAttributeMetadata());
                if (requestedAttributeNormalized.equals(connectionAttributeNormalized)) {
                  value = String.valueOf(attribute.getPublicValue().orElse(null));
                  break;
                }
              }
              row.add(value);
            }
            insertStream.insert(row);
          }
        }
      }
      String description = servicesFound + " services were";
      if (servicesFound == 1) {
        description = servicesFound + " service was";
      }
      Path confVault = tabular.getConfPath();
      feedbackDataPath.setComment(description + " found in the configuration vault (" + confVault + ")");
    }


    return Collections.singletonList(feedbackDataPath);

  }


}
