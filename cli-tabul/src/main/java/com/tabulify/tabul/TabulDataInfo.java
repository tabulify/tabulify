package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.InfoPipelineStep;
import com.tabulify.flow.operation.SelectPipelineStep;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;


/**
 *
 */
public class TabulDataInfo {

  public static final String TABUL_EXCLUDED_ATTRIBUTE_OPTION = "--excluded-attribute";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Print the attributes of one or more data resources (files, tables, ...) in a form fashion",
        "",
        "Tip: To get the attributes in a list fashion, check the `list` command"
      )
      .addExample(
        "Show the information on all files in the current directory",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "Show the information on the `date` table of the `oracle` connection",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " date@oracle",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "Shows information on the tables that begins with `D` of the `sqlite` connection",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D*@sqlite",
        CliUsage.CODE_BLOCK
      );

    childCommand.addProperty(TABUL_EXCLUDED_ATTRIBUTE_OPTION)
      .setShortName("-ea")
      .setDescription("Attribute name to be excluded (Mostly computational attributes such as digest, md5, count, size, ...)");

    childCommand.addArg(TabulWords.DATA_SELECTORS)
      .setDescription("One or more name data resource selectors (ie pattern[@connection])")
      .setMandatory(true);
    childCommand.addFlag(TabulWords.WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(TYPE_PROPERTY);
    childCommand.addProperty(ATTRIBUTE_OPTION);
    childCommand.addFlag(STRICT_SELECTION);

    // Arguments
    final CliParser cliParser = childCommand.parse();

    final List<String> excludedAttributes = cliParser.getStrings(TABUL_EXCLUDED_ATTRIBUTE_OPTION);

    final List<DataUriNode> dataSelectors = cliParser
      .getStrings(TabulWords.DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());
    final Boolean withDependencies = cliParser.getBoolean(TabulWords.WITH_DEPENDENCIES_PROPERTY);

    MediaType type = null;
    try {
      type = MediaTypes.parse(cliParser.getString(TYPE_PROPERTY));
    } catch (NullValueException e) {
      // ok supplier step take a null value if any
    }
    Map<String, ?> attributesRaw = cliParser.getProperties(TabulWords.ATTRIBUTE_OPTION);
    Map<KeyNormalizer, ?> attributes;
    try {
      attributes = Casts.castToNewMap(attributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the values of the option " + ATTRIBUTE_OPTION + " returns an error " + e.getMessage(), e);
    }

    Boolean strictSelection = cliParser.getBoolean(STRICT_SELECTION);
    ArrayList<DataPath> dataPaths = new ArrayList<>(
      Pipeline
        .builder(tabular)
        .addStep(
          SelectPipelineStep
            .builder()
            .setDataSelectors(dataSelectors)
            .setWithDependencies(withDependencies)
            .setDataDef(attributes)
            .setStrictSelection(strictSelection)
            .setMediaType(type)
        )
        .addStep(
          InfoPipelineStep
            .create()
            .setExcludedAttributes(excludedAttributes)
        )
        .build()
        .execute()
        .getDownStreamDataPaths()
    );

    // Select is not strict by default
    if (!strictSelection && dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(dataSelectors);
    }

    return dataPaths;

  }


}
