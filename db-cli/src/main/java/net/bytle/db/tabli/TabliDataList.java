package net.bytle.db.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.ListCollector;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAttribute;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliWords.NOT_STRICT_FLAG;


/**
 *
 */
public class TabliDataList {


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


    childCommand.addArg(TabliWords.DATA_SELECTORS)
      .setDescription("One or more name data resource selectors (ie pattern[@connection])")
      .setMandatory(true);
    childCommand.addProperty(TabliWords.ATTRIBUTE_PROPERTY)
      .setDescription("Set the data resource attributes to show (`path`, `name`,`count`, `size`, `type`, `connection`, ...)")
      .setValueName("attributeName")
      .setDefaultValue(DataPathAttribute.PATH);
    childCommand.addFlag(TabliWords.WITH_DEPENDENCIES_PROPERTY);
    childCommand.addFlag(NOT_STRICT_FLAG);


    // Arguments
    final CliParser cliParser = childCommand.parse();

    final Boolean isNotStrictPresent = cliParser.getBoolean(NOT_STRICT_FLAG);
    if(isNotStrictPresent){
      tabular.setStrict(false);
    }

    final Set<DataUri> dataUriPatterns = cliParser
      .getStrings(TabliWords.DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final List<String> attributes = cliParser.getStrings(TabliWords.ATTRIBUTE_PROPERTY);
    final Boolean withDependencies = cliParser.getBoolean(TabliWords.WITH_DEPENDENCIES_PROPERTY);

    /**
     * Stream
     */
    return new ArrayList<>(
      Pipeline
        .createFrom(tabular)
        .addStepToGraph(
          SelectSupplier.create()
            .setDataSelectors(dataUriPatterns)
            .setWithDependencies(withDependencies)
        )
        .addStepToGraph(
          ListCollector.create()
            .setAttributes(attributes)
        )
        .execute()
        .getDownStreamDataPaths());

  }


}
