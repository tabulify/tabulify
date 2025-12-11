package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.flow.operation.StructPipelineStep;
import com.tabulify.gen.flow.enrich.EnrichPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;


public class TabulDataDescribe {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // The command
    childCommand.setDescription("Show the data structure of one or more data resources (s)");
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addProperty(ATTRIBUTE_OPTION);
    childCommand.addFlag(STRICT_SELECTION);

    // Args
    final CliParser cliParser = childCommand.parse();


    final List<DataUriNode> dataUriSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);

    Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);
    Map<String, String> attributesRaw = cliParser.getProperties(ATTRIBUTE_OPTION);
    Map<KeyNormalizer, String> attributes;
    try {
      attributes = Casts.castToNewMap(attributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the value of the option " + ATTRIBUTE_OPTION + " returns an error " + e.getMessage(), e);
    }

    Boolean strict = cliParser.getBoolean(STRICT_SELECTION);
    ArrayList<DataPath> dataPaths = new ArrayList<>(
      Pipeline
        .builder(tabular)
        .addStep(
          SelectPipelineStep.builder()
            .setWithDependencies(withDependencies)
            .setDataSelectors(dataUriSelectors)
            .setDataDef(attributes)
            .setStrictSelection(strict)
        )
        .addStep(
          EnrichPipelineStep
            .builder()
            .addMetaColumns(virtualColumns)
        )
        .addStep(
          StructPipelineStep.create()
        )
        .build()
        .execute()
        .getDownStreamDataPaths()
    );

    // Select is not strict by default
    if (!strict && dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(dataUriSelectors);
    }

    return dataPaths;

  }


}
