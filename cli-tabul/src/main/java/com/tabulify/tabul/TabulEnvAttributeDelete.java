package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.ConfVault;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.type.KeyNormalizer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TabulEnvAttributeDelete {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Define the command and its arguments
    childCommand
      .setDescription("Delete a tabulify attribute from the conf file")
      .addExample(
        "To remove the `log-level` attribute, you would use the following command:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + KeyNormalizer.createSafe(TabularAttributeEnum.LOG_LEVEL).toKebabCase(),
        CliUsage.CODE_BLOCK
      );


    childCommand.addArg(TabulWords.KEY)
      .setDescription("The attribute to delete (globbing works too)")
      .setMandatory(true);


    // Args control
    CliParser cliParser = childCommand.parse();

    final String key = cliParser.getString(TabulWords.KEY);

    ConfVault confVault = ConfVault
      .createFromPath(tabular.getConfPath(), tabular);
    Set<Attribute> deleteAttributesByGlobName = confVault.deleteAttributesByGlobName(key);

    if (deleteAttributesByGlobName.isEmpty()) {
      if (tabular.isStrictExecution()) {
        throw new RuntimeException("No attribute were found with the glob " + key);
      }
    } else {
      confVault.flush();
    }

    DataPath feedbackDataPath = tabular.getMemoryConnection().getDataPath("configurationDeleted")
      .setComment("The below global attributes were deleted")
      .createRelationDef()
      .addColumn("key")
      .addColumn("value")
      .getDataPath();
    try (InsertStream insertStream = feedbackDataPath.getInsertStream()) {
      for (Attribute attribute : deleteAttributesByGlobName) {
        insertStream.insert(tabular.toPublicName(attribute), attribute.getValueOrDefaultAsStringNotNull());
      }
    }

    return Collections.singletonList(feedbackDataPath);

  }

}
