package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.CastStep;
import com.tabulify.flow.step.EnrichStep;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUri;
import net.bytle.exception.NullValueException;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.*;


public class TabliDataPrint {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Print the content of data resources.",
        "",
        "This command will print all data.",
        "To print in a limited fashion, you can use the following commands:",
        " * `head` :  print the head (the first pieces of content)",
        " * `tail` :  print the tail (the last pieces of content)"
      )
      .addExample(
        "Show all the records of the table `sales` from the data store `sqlite`:",
        CliUsage.EOL,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " sales@sqlite"
      )
      .addExample(
        "Show the content of the file `request.log`:",
        CliUsage.EOL,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " request.log"
      );
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addProperty(TabliWords.ATTRIBUTE_PROPERTY)
      .setDescription("Set specific data resource attributes")
      .setValueName("attributeName=value");
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addProperty(TabliWords.IS_STRICT_FLAG)
      .setDescription("If set the selection will return an error if no data resources have been selected")
      .setDefaultValue(false);
    childCommand.addProperty(TYPE_PROPERTY);

    // Args
    CliParser cliParser = childCommand.parse();
    final Set<DataUri> dataUriSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());

    Boolean isStrict = cliParser.getBoolean(TabliWords.IS_STRICT_FLAG);

    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    Map<String, ?> attributes = cliParser.getProperties(TabliWords.ATTRIBUTE_PROPERTY);
    Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);
    MediaType type = null;
    try {
      type = MediaTypes.createFromMediaTypeString(cliParser.getString(TYPE_PROPERTY));
    } catch (NullValueException e) {
      // ok supplier step take a null value if any
    }

    Set<DataPath> dataPaths = Pipeline
      .createFrom(tabular)
      .addStepToGraph(
        SelectSupplier.create()
          .setDataSelectors(dataUriSelectors)
          .setIsStrict(isStrict)
          .setWithDependencies(withDependencies)
          .setAttributes(attributes)
          .setMediaType(type)
      )
      .addStepToGraph(
        CastStep.create()
      )
      .addStepToGraph(
        EnrichStep
          .create()
          .addVirtualColumns(virtualColumns)
      )
      .execute()
      .getDownStreamDataPaths();
    if (dataPaths.size() == 0) {
      System.out.println();
      System.out.println("No data resources selected by the data selectors (" + dataUriSelectors.stream().sorted().map(DataUri::toString).collect(Collectors.joining(",")) + ")");
      System.out.println();
    }
    return new ArrayList<>(dataPaths);

  }


}
