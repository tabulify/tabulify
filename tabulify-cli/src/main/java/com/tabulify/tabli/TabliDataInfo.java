package com.tabulify.tabli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.InfoStep;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUri;
import net.bytle.exception.NullValueException;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.NOT_STRICT_FLAG;
import static com.tabulify.tabli.TabliWords.TYPE_PROPERTY;


/**
 *
 */
public class TabliDataInfo {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Print the attributes of one or more data resources (files, tables, ...) in a form fashion",
        "",
        "Tip: To get the attributes in a list fashion, check the `list` command"
      )
      .addExample(
        "Show the information on all files in the current directory ",
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


    childCommand.addArg(TabliWords.DATA_SELECTORS)
      .setDescription("One or more name data resource selectors (ie pattern[@connection])")
      .setMandatory(true);
    childCommand.addFlag(TabliWords.WITH_DEPENDENCIES_PROPERTY);
    childCommand.addFlag(TabliWords.NOT_STRICT_FLAG);
    childCommand.addProperty(TYPE_PROPERTY);

    // Arguments
    final CliParser cliParser = childCommand.parse();

    final Boolean isNotStrictPresent = cliParser.getBoolean(NOT_STRICT_FLAG);
    if(isNotStrictPresent){
      tabular.setStrict(false);
    }

    final Set<DataUri> dataSelectors = cliParser
      .getStrings(TabliWords.DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    final Boolean withDependencies = cliParser.getBoolean(TabliWords.WITH_DEPENDENCIES_PROPERTY);

    MediaType type = null;
    try {
      type = MediaTypes.createFromMediaTypeString(cliParser.getString(TYPE_PROPERTY));
    } catch (NullValueException e) {
      // ok supplier step take a null value if any
    }

    return new ArrayList<>(
      Pipeline
        .createFrom(tabular)
        .addStepToGraph(
          SelectSupplier.create()
            .setDataSelectors(dataSelectors)
            .setWithDependencies(withDependencies)
            .setMediaType(type)
        )
        .addStepToGraph(
          InfoStep.create()
        )
        .execute()
        .getDownStreamDataPaths()
    );

  }


}


