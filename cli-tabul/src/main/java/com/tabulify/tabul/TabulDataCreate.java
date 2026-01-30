package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.engine.PipelineBuilder;
import com.tabulify.flow.operation.CreatePipelineStep;
import com.tabulify.flow.operation.DefinePipelineStep;
import com.tabulify.flow.operation.ExecutePipelineStep;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NullValueException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;
import static com.tabulify.tabul.TabulWords.*;

public class TabulDataCreate {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // The command
    childCommand.setDescription("Create a data resource(s) (table, file, ..) from:",
      "  * the metadata of another data resource",
      "  * a script (DDL)",
      "",
      "If the `target uri` is: ",
      "  * not specified, if the `data selector` is:",
      "       * a script selector, the selected resources is executed",
      "       * otherwise, a data resource is created (with the `data selector` as `data uri`)",
      "  * specified. If the target: " +
        "       * is a view, a sql select query has source is expected" +
        "       * otherwise, the metadata of the selected resources is copied"
    );
    childCommand.addArg(TabulWords.SOURCE_SELECTORS)
      .setDescription("A data selectors that select one or more data resources (Example: `*--datadef.yml@connection`)")
      .setMandatory(true);
    childCommand.addArg(TARGET_DATA_URI)
      .setDescription("A target data Uri that defines the connection and optionally the table name. If the target is not specified, the selected data resource must be of type SCRIPTS and will just run.")
      .setMandatory(false);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY)
      .setDescription("Create also the table dependencies (ie the foreign data resources will also be created).");
    childCommand.addProperty(SOURCE_ATTRIBUTE);
    childCommand.addProperty(TARGET_ATTRIBUTE);
    childCommand.addFlag(STRICT_SELECTION);

    // Args
    final CliParser cliParser = childCommand.parse();

    final DataUriNode dataSelector = tabular.createDataUri(cliParser.getString(TabulWords.SOURCE_SELECTORS));
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    final Map<String, String> sourceAttributesRaw = cliParser.getProperties(SOURCE_ATTRIBUTE);
    Map<KeyNormalizer, Object> sourceAttributes;
    try {
      sourceAttributes = Casts.castToNewMap(sourceAttributesRaw, KeyNormalizer.class, Object.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the value of the option " + SOURCE_ATTRIBUTE + " returns an error " + e.getMessage(), e);
    }

    Boolean strictSelection = cliParser.getBoolean(STRICT_SELECTION);


    // Not a copy of metadata
    if (dataSelector.isRuntime()) {

      LOGGER_TABUL.info("Starting create action for the script selector (" + dataSelector + ")");
      ArrayList<DataPath> dataPaths = new ArrayList<>(Pipeline
        .builder(tabular)
        .addStep(
          SelectPipelineStep
            .builder()
            .setDataSelector(dataSelector)
            .setStrictSelection(strictSelection)
        )
        .addStep(ExecutePipelineStep.builder())
        .build()
        .execute()
        .getDownStreamDataPaths());

      // Select is not strict by default
      if (!strictSelection && dataPaths.isEmpty()) {
        System.out.println();
        System.out.println("No data resources selected by the data selector (" + dataSelector + ")");
        System.out.println();
      }

      return dataPaths;

    }

    final Map<String, String> targetAttributesRaw = cliParser.getProperties(TARGET_ATTRIBUTE);
    Map<KeyNormalizer, String> targetAttributes;
    try {
      targetAttributes = Casts.castToNewMap(targetAttributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the value of the option " + TARGET_ATTRIBUTE + " returns an error " + e.getMessage(), e);
    }


    String argTargetUri = cliParser.getString(TARGET_DATA_URI);
    DataUriNode targetDataUri = null;
    if (argTargetUri != null) {
      targetDataUri = tabular.createDataUri(argTargetUri);
    }

    /**
     * The creation
     * If there is a target, this a copy of metadata
     */
    PipelineBuilder builder = Pipeline.builder(tabular);
    if (targetDataUri == null) {
      LOGGER_TABUL.info("Starting create action for the data uri (" + dataSelector + ")");
      MediaType mediaType = null;
      try {
        mediaType = MediaTypes.parse(targetAttributes.get(KeyNormalizer.createSafe(DataPathAttribute.MEDIA_TYPE)));
      } catch (NullValueException e) {
        // ok null
      }
      DataPath dataPath = tabular
        .getDataPath(dataSelector, mediaType)
        .mergeDataDefinitionFromYamlMap(sourceAttributes);
      builder.addStep(
        DefinePipelineStep
          .builder()
          .addDataPath(dataPath)
      );
    } else {
      LOGGER_TABUL.info("Starting create action into (" + targetDataUri + ") from (" + dataSelector + ") " + (withDependencies ? "with" : "without") + " dependencies.");
      builder.addStep(
        SelectPipelineStep
          .builder()
          .setDataSelector(dataSelector)
          .setWithDependencies(withDependencies)
          .setDataDef(sourceAttributes)
          .setStrictSelection(strictSelection)
      );
    }


    List<DataPath> dataPaths = builder
      .addStep(
        CreatePipelineStep.builder()
          .setTargetDataUri(targetDataUri)
          .setTargetDataDef(targetAttributes)
      )
      .build()
      .execute()
      .getDownStreamDataPaths();

    // Select is not strict by default
    if (!strictSelection && dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(List.of(dataSelector));
    }

    return dataPaths;


  }

}
