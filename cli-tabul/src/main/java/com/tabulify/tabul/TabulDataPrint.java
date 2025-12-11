package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.gen.flow.enrich.EnrichPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NullValueException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;


public class TabulDataPrint {


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
    childCommand.addProperty(TabulWords.ATTRIBUTE_OPTION)
      .setDescription("Set specific data resource attributes")
      .setValueName("attributeName=value");
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addFlag(STRICT_SELECTION);
    childCommand.addProperty(TYPE_PROPERTY);

    // Args
    CliParser cliParser = childCommand.parse();
    final List<DataUriNode> dataUriSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());


    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    Map<String, ?> attributesRaw = cliParser.getProperties(TabulWords.ATTRIBUTE_OPTION);
    Map<KeyNormalizer, ?> attributes;
    try {
      attributes = Casts.castToNewMap(attributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the values of the option " + ATTRIBUTE_OPTION + " returns an error " + e.getMessage(), e);
    }
    Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);

    SelectPipelineStep selectPipelineStep = SelectPipelineStep
      .builder()
      .setDataSelectors(dataUriSelectors)
      .setStrictSelection(cliParser.getBoolean(TabulWords.STRICT_SELECTION))
      .setWithDependencies(withDependencies)
      .setDataDef(attributes);

    try {
      MediaType type = MediaTypes.parse(cliParser.getString(TYPE_PROPERTY));
      selectPipelineStep.setMediaType(type);
    } catch (NullValueException e) {
      // ok, no media type supplied
    }


    List<DataPath> dataPaths = Pipeline
      .builder(tabular)
      .addStep(selectPipelineStep)
      .addStep(
        EnrichPipelineStep
          .builder()
          .addMetaColumns(virtualColumns)
      )
      .build()
      .execute()
      .getDownStreamDataPaths();
    if (dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(dataUriSelectors);
    }
    return dataPaths;

  }


}
