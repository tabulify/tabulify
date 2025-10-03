package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.HeadPipelineStep;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.gen.flow.enrich.EnrichPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.exception.CastException;
import net.bytle.exception.NullValueException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;


public class TabulDataHead {


  /**
   * 10 because you want the result to be visible in the console.
   */
  public static final int DEFAULT_LIMIT_VALUE = 10;


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Print the first N rows of content of data resources.",
        "",
        "By default, there is a limit of " + DEFAULT_LIMIT_VALUE + " on the number of rows printed"
      )
      .addExample(
        "Show the first " + DEFAULT_LIMIT_VALUE + " records of the table `sales` from the data store `sqlite`:",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " sales@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "Show the first 500 rows of the table `time` from the data store `postgres`:",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " " + LIMIT_PROPERTY + " 500 time@postgres",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "Show the first " + DEFAULT_LIMIT_VALUE + " lines from the file `request.log`:",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " request.log",
        CliUsage.CODE_BLOCK
      )
      .setFooter("Important:",
        "We don't recommend to increase the rows limit to a very high number.",
        "This command loads the data into memory to calculate the data layout and",
        "render data aligned properly.",
        "Increasing the limit will then increase the memory footprint and may cause an out-of-memory error."
      );
    childCommand.addProperty(LIMIT_PROPERTY)
      .setDescription("Limit the number of rows printed (See footer)")
      .setDefaultValue(DEFAULT_LIMIT_VALUE);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addProperty(TabulWords.ATTRIBUTE_OPTION)
      .setDescription("Set specific data resource attributes")
      .setValueName("attributeName=value");
    childCommand.addProperty(TYPE_PROPERTY);
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);

    // Args
    CliParser cliParser = childCommand.parse();
    final Integer limit = cliParser.getInteger(LIMIT_PROPERTY);
    final List<DataUriNode> dataUriSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);

    Map<KeyNormalizer, ?> attributes;
    Map<String, String> attributesRaw = cliParser.getProperties(ATTRIBUTE_OPTION);
    try {
      attributes = Casts.castToNewMap(attributesRaw, KeyNormalizer.class, Object.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the value of the option " + ATTRIBUTE_OPTION + " returns an error " + e.getMessage(), e);
    }
    Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);
    MediaType type = null;
    try {
      type = MediaTypes.parse(cliParser.getString(TYPE_PROPERTY));
    } catch (NullValueException e) {
      // ok, supplier step take a null value if any
    }

    return new ArrayList<>(
      Pipeline
        .builder(tabular)
        .addStep(
          SelectPipelineStep.builder()
            .setDataSelectors(dataUriSelectors)
            .setWithDependencies(withDependencies)
            .setDataDef(attributes)
            .setMediaType(type)
        )
        .addStep(
          EnrichPipelineStep
            .builder()
            .addMetaColumns(virtualColumns)
        )
        .addStep(
          HeadPipelineStep
            .builder()
            .setLimit(limit)
        )
        .build()
        .execute()
        .getDownStreamDataPaths()
    );


  }


}
