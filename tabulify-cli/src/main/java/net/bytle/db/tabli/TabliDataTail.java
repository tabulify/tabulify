package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.CastStep;
import net.bytle.db.flow.step.EnrichStep;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.flow.step.TailFunction;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.NullValueException;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliDataHead.DEFAULT_LIMIT_VALUE;
import static net.bytle.db.tabli.TabliWords.*;


public class TabliDataTail {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Print the last N rows of content of data resources.",
        "",
        "By default, there is a limit of " + DEFAULT_LIMIT_VALUE + " on the number of rows printed"
      )
      .addExample(
        "Show the first " + DEFAULT_LIMIT_VALUE + " records data of the table `sales` from the data store `sqlite`:",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + "sales@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "Show the last 500 rows of the table `time` from the data store `postgres`:",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " " + LIMIT_PROPERTY + " 500 time@postgres",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "Show the last " + DEFAULT_LIMIT_VALUE + " lines from the file `request.log`:",
        CliUsage.CODE_BLOCK,
        CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " request.log",
        CliUsage.CODE_BLOCK
      )
      .setFooter("Important:",
        "We don't recommend to increase the rows limit to a very high number.",
        "This command loads the data into memory to calculate the data layout and ",
        "render data aligned properly.",
        "Increasing the limit will then increase the memory footprint and may cause an out-of-memory error."
      );
    childCommand.addProperty(LIMIT_PROPERTY)
      .setDescription("Limit the number of rows printed (See footer)")
      .setDefaultValue(DEFAULT_LIMIT_VALUE);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addProperty(TabliWords.ATTRIBUTE_PROPERTY)
      .setDescription("Set specific data resource attributes")
      .setValueName("attributeName=value");
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addProperty(TYPE_PROPERTY);

    // Args
    CliParser cliParser = childCommand.parse();
    final Integer limit = cliParser.getInteger(LIMIT_PROPERTY);
    final Set<DataUri> dataUriSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);

    Map<String, ?> attributes = cliParser.getProperties(TabliWords.ATTRIBUTE_PROPERTY);
    Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);
    MediaType type = null;
    try {
      type = MediaTypes.createFromMediaTypeString(cliParser.getString(TYPE_PROPERTY));
    } catch (NullValueException e) {
      // ok supplier step take a null value if any
    }

    return Pipeline
      .createFrom(tabular)
      .addStepToGraph(
        SelectSupplier.create()
          .setDataSelectors(dataUriSelectors)
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
      .addStepToGraph(
        TailFunction.create()
          .setLimit(limit)
      )
      .execute()
      .getDownStreamDataPaths()
      .stream()
      .sorted()
      .collect(Collectors.toList());


  }


}
