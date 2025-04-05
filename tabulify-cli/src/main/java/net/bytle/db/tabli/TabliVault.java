package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.tabli.TabliLog.LOGGER_TABLI;

public class TabliVault {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription("The vault permits to encrypt or decrypt sensitive information");

    childCommand.addChildCommand(TabliWords.ENCRYPT_COMMAND)
      .setDescription("Encrypt sensitive information");
    childCommand.addChildCommand(TabliWords.DECRYPT_COMMAND)
      .setDescription("Decrypt sensitive information");

    List<DataPath> feedbackDataPaths = new ArrayList<>();
    List<CliCommand> commands = childCommand.parse().getFoundedChildCommands();
    if (commands.size() == 0) {
      throw new IllegalArgumentException("A known command must be given");
    } else {

      for (CliCommand subChildCommand : commands) {
        LOGGER_TABLI.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case TabliWords.ENCRYPT_COMMAND:
            feedbackDataPaths = TabliVaultEncrypt.run(tabular, subChildCommand);
            break;
          case TabliWords.DECRYPT_COMMAND:
            feedbackDataPaths = TabliVaultDecrypt.run(tabular, subChildCommand);
            break;
          default:
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
        }
      }
    }
    return feedbackDataPaths;
  }
}
