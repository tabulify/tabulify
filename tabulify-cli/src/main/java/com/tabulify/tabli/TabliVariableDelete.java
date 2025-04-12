package com.tabulify.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.conf.ConfManager;
import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class TabliVariableDelete {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Define the command and its arguments
    childCommand
      .setDescription("Delete a configuration from the configuration file")
      .addExample(
        "To remove the `log-level` configuration, you would use the following command:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " log-level",
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabliWords.KEY)
      .setDescription("the configuration key to delete")
      .setMandatory(true);


    // Args control
    CliParser cliParser = childCommand.parse();

    final String key = cliParser.getString(TabliWords.KEY);

    Path conf = TabliVariable.getVariablesFilePathToModify(tabular, cliParser);
    ConfManager confManager = ConfManager
      .createFromPath(conf);
    Object value = confManager
      .delete(key);
    confManager.flush();

    DataPath feedbackDataPath = tabular.getMemoryDataStore().getDataPath("configurationDeleted")
      .setDescription("The below configuration was deleted")
      .createRelationDef()
      .addColumn("key")
      .addColumn("value")
      .getDataPath();
    try(InsertStream insertStream= feedbackDataPath.getInsertStream()){
      insertStream.insert(key,value);
    }

    return Collections.singletonList(feedbackDataPath);

  }

}
