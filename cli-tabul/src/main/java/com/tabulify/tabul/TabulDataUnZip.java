package com.tabulify.tabul;


import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import com.tabulify.zip.flow.UnZipPipelineStep;
import com.tabulify.zip.flow.UnZipPipelineStepArgument;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.exception.CastException;
import com.tabulify.glob.Glob;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.List;
import java.util.Map;

import static com.tabulify.tabul.TabulWords.*;


/**
 * <p>
 */
public class TabulDataUnZip {


  private static final String ENTRY_SELECTOR = "--" + UnZipPipelineStepArgument.ENTRY_SELECTOR.getKeyNormalized().toCliLongOptionName();
  private static final String STRIP_COMPONENTS = "--" + UnZipPipelineStepArgument.STRIP_COMPONENTS.getKeyNormalized().toCliLongOptionName();

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    childCommand.setDescription(
      "Unzip one or more archive data resources."
    );

    String defaultTargetDirectory = UnZipPipelineStepArgument.TARGET_DATA_URI.getDefaultValue().toString();

    childCommand
      .addExample(
        "To unzip the archive `world-db.tar.gz` into the default directory" + defaultTargetDirectory + ", you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " world-db.tar.gz@cd",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To unzip the archive `https://downloads.mysql.com/docs/world-db.tar.gz` into the tmp directory, you would execute",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " https://downloads.mysql.com/docs/world-db.tar.gz @tmp",
        CliUsage.CODE_BLOCK
      );

    childCommand.addArg(TabulWords.SOURCE_SELECTORS)
      .setDescription("A data selector that selects the archive data resources to unzip")
      .setMandatory(true);

    childCommand.addArg(TARGET_DATA_URI)
      .setDescription(UnZipPipelineStepArgument.TARGET_DATA_URI.getDescription())
      .setDefaultValue(defaultTargetDirectory)
      .setMandatory(false);
    childCommand.addProperty(SOURCE_ATTRIBUTE);
    childCommand.addFlag(STRICT_SELECTION);

    /**
     * Unzip properties
     */
    childCommand.addProperty(ENTRY_SELECTOR)
      .setDescription(UnZipPipelineStepArgument.ENTRY_SELECTOR.getDescription())
      .setDefaultValue(UnZipPipelineStepArgument.ENTRY_SELECTOR.getDefaultValue())
      .setMandatory(false);
    childCommand.addProperty(STRIP_COMPONENTS)
      .setDescription(UnZipPipelineStepArgument.STRIP_COMPONENTS.getDescription())
      .setDefaultValue(UnZipPipelineStepArgument.STRIP_COMPONENTS.getDefaultValue())
      .setMandatory(false);

    // Args
    CliParser cliParser = childCommand.parse();
    final DataUriNode dataSelector = tabular.createDataUri(cliParser.getString(TabulWords.SOURCE_SELECTORS));
    String targetUriArg = cliParser.getString(TARGET_DATA_URI);
    DataUriNode targetDataUri = null;
    if (targetUriArg != null) {
      targetDataUri = tabular.createDataUri(targetUriArg);
    }
    Boolean strict = cliParser.getBoolean(STRICT_SELECTION);
    final Map<String, String> sourceAttributesRaw = cliParser.getProperties(SOURCE_ATTRIBUTE);
    final Map<KeyNormalizer, String> sourceAttributes;
    try {
      sourceAttributes = Casts.castToNewMap(sourceAttributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the values of the option " + SOURCE_ATTRIBUTE + " returns an error: " + e.getMessage(), e);
    }
    String entrySelector = cliParser.getString(ENTRY_SELECTOR);
    Glob entrySelectorGlob = null;
    if (entrySelector != null) {
      entrySelectorGlob = new Glob(entrySelector);
    }
    Integer stripComponents = cliParser.getInteger(STRIP_COMPONENTS);

    List<DataPath> dataPaths = Pipeline
      .builder(tabular)
      .addStep(
        SelectPipelineStep.builder()
          .setDataSelector(dataSelector)
          .setWithDependencies(false)
          .setDataDef(sourceAttributes)
          .setStrictSelection(strict)
      )
      .addStep(
        UnZipPipelineStep.builder()
          .setEntrySelector(entrySelectorGlob)
          .setStripComponents(stripComponents)
          .setTargetDataUri(targetDataUri)
      )
      .build()
      .execute()
      .getDownStreamDataPaths();

    // Select is not strict by default
    if (!strict && dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(List.of(dataSelector));
    }

    return dataPaths;

  }

}
