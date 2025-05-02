package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.conf.ConfVault;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.exception.CastException;
import net.bytle.type.KeyNormalizer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class TabliAttributeDelete {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Define the command and its arguments
    childCommand
      .setDescription("Delete a tabulify attribute from the conf file")
      .addExample(
        "To remove the `log-level` attribute, you would use the following command:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + KeyNormalizer.create(TabularAttributeEnum.LOG_LEVEL).toKebabCase(),
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabliWords.KEY)
      .setDescription("the configuration key to delete")
      .setMandatory(true);


    // Args control
    CliParser cliParser = childCommand.parse();

    final String key = cliParser.getString(TabliWords.KEY);

    Path conf = TabliAttribute.getVariablesFilePathToModify(tabular, cliParser);
    ConfVault confVault = ConfVault
      .createFromPath(conf, tabular);
    Object value;
    try {
      value = confVault.deleteVariable(key);
    } catch (CastException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    confVault.flush();

    DataPath feedbackDataPath = tabular.getMemoryDataStore().getDataPath("configurationDeleted")
      .setDescription("The below configuration was deleted")
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
