package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.jdbc.SqlDataStoreStatic;
import com.tabulify.model.RelationDef;
import com.tabulify.service.Service;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.regexp.Glob;
import net.bytle.type.KeyCase;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;


/**
 * See also {@link SqlDataStoreStatic}
 */
public class TabulServiceInfo {

  protected static final String SERVICE_NAMES = "(NamePattern)...";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    String description = "Print services attributes in a form fashion.";

    // Add information about the command
    childCommand
      .setDescription(description)
      .addExample(
        "To output information about the service `name`:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " name",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To output information about all the service with `sql` in their name:" +
          CliUsage.getFullChainOfCommand(childCommand) + " sql*");

    childCommand.addArg(SERVICE_NAMES)
      .setDescription("one or more service glob names")
      .setDefaultValue("*");

    childCommand.addFlag(TabulWords.NOT_STRICT_EXECUTION_FLAG)
      .setDescription("If there is no connection found, no errors will be emitted");

    CliParser cliParser = childCommand.parse();


    // Retrieve
    final List<Service> services = new ArrayList<>();
    List<String> globNames = cliParser.getStrings(SERVICE_NAMES);
    for (String name : globNames) {
      Glob glob = Glob.createOf(name);
      for (Service service : tabular.getServices()) {
        if (glob.matchesIgnoreCase(service.getName().toString())) {
          services.add(service);
        }
      }
    }

    List<DataPath> feedbackDataPaths = new ArrayList<>();
    if (services.isEmpty()) {
      tabular.warningOrTerminateIfStrict("No service was found with the names (" + globNames + ")");
      return feedbackDataPaths;
    }


    for (Service service : services) {

      RelationDef feedbackDataDef = tabular.getMemoryConnection()
        .getDataPath(service.getName() + "_info")
        .setComment("Information about the service (" + service + ")")
        .createRelationDef()
        .addColumn("Attribute")// attribute and not properties because the product is called `tabulify`
        .addColumn("Value")
        .addColumn("Description");
      feedbackDataPaths.add(feedbackDataDef.getDataPath());

      // Upper snake to show that you can overwrite it with an operating system
      KeyCase snakeUpper = KeyCase.SNAKE_UPPER;
      try (InsertStream insertStream = feedbackDataDef.getDataPath().getInsertStream()) {

        service.getAttributes().stream().sorted().forEach(attribute -> {

          List<Object> rowAttributes = new ArrayList<>();
          rowAttributes.add(KeyNormalizer.createSafe(attribute.getAttributeMetadata()).toCase(snakeUpper));
          rowAttributes.add(attribute.getPublicValue());
          rowAttributes.add(attribute.getAttributeMetadata().getDescription());
          insertStream.insert(rowAttributes);

        });
      }
    }
    return feedbackDataPaths;
  }
}
