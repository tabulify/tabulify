package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.ListPipelineStep;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.exception.CastException;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.STRICT_SELECTION;


/**
 *
 */
public class TabulDataList {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Print a list of data resources (files, tables, ...)"
      )
      .addExample(
        "List all the current files ",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "List all the tables of the current schema of the `sqlite` connection",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "List all the tables that begins with `D` of the `sqlite` connection",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D*@sqlite",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabulWords.DATA_SELECTORS)
      .setDescription("One or more name data resource selectors (ie pattern[@connection])")
      .setMandatory(true);
    childCommand.addProperty(TabulWords.ATTRIBUTE_OPTION)
      .setDescription("Set additional data resource attributes to show (`count`, `size`, `connection`, ...)")
      .setValueName("attributeName");

    childCommand.addFlag(TabulWords.WITH_DEPENDENCIES_PROPERTY);
    childCommand.addFlag(STRICT_SELECTION);
    childCommand.addProperty(TabulWords.ATTRIBUTE_OPTION)
      .setDescription("Add one or more data resource attribute");

    // Arguments
    final CliParser cliParser = childCommand.parse();


    final List<DataUriNode> dataUriSelectors = cliParser
      .getStrings(TabulWords.DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());
    final List<KeyNormalizer> attributes = new ArrayList<>();
    attributes.add(DataPathAttribute.PATH.getKeyNormalized());
    attributes.add(DataPathAttribute.MEDIA_TYPE.getKeyNormalized());
    for (String attribute : cliParser.getStrings(TabulWords.ATTRIBUTE_OPTION)) {
      try {
        attributes.add(KeyNormalizer.create(attribute));
      } catch (CastException e) {
        throw new IllegalArgumentException("The attribute " + attribute + " is not valid. Error: " + e.getMessage(), e);
      }
    }

    final Boolean withDependencies = cliParser.getBoolean(TabulWords.WITH_DEPENDENCIES_PROPERTY);
    Boolean isStrictSelection = cliParser.getBoolean(STRICT_SELECTION);

    /**
     * Stream
     */

    List<DataPath> dataPaths = Pipeline
      .builder(tabular)
      .addStep(
        SelectPipelineStep
          .builder()
          .setDataSelectors(dataUriSelectors)
          .setWithDependencies(withDependencies)
          .setStrictSelection(isStrictSelection)
      )
      .addStep(
        ListPipelineStep
          .builder()
          .setDataAttributes(attributes)
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
