package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.conf.ConfManager;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.exception.CastException;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class TabliVariableSet {
  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Define the command and its arguments
    childCommand
      .setDescription("Set a configuration")
      .addExample(
        "To set the `log-level` configuration to the value `tip`, you would use the following command:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " log-level tip",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabliWords.KEY)
      .setDescription("the configuration key")
      .setMandatory(true);
    childCommand.addArg(TabliWords.VALUE)
      .setDescription("the configuration value")
      .setMandatory(true);


    // Args control
    CliParser cliParser = childCommand.parse();

    final String key = cliParser.getString(TabliWords.KEY);
    final String value = cliParser.getString(TabliWords.VALUE);

    Path conf = TabliVariable.getVariablesFilePathToModify(tabular, cliParser);

    try (ConfManager fromPath = ConfManager.createFromPath(conf, tabular.getVault())) {
      fromPath.addVariable(key, value);
    } catch (CastException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    DataPath feedbackDataPath = tabular.getMemoryDataStore().getDataPath("configurationSet")
      .setDescription("The below configuration was set")
      .createRelationDef()
      .addColumn("key")
      .addColumn("value")
      .getDataPath();
    try (InsertStream insertStream = feedbackDataPath.getInsertStream()) {
      insertStream.insert(key, value);
    }
    return Collections.singletonList(feedbackDataPath);
  }

}
