package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliUsage;

import java.util.ArrayList;
import java.util.List;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;

public class TabulVault {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription("The vault permits to encrypt or decrypt sensitive information");

    childCommand.addChildCommand(TabulWords.ENCRYPT_COMMAND)
      .setDescription("Encrypt sensitive information");
    childCommand.addChildCommand(TabulWords.DECRYPT_COMMAND)
      .setDescription("Decrypt sensitive information");

    List<DataPath> feedbackDataPaths = new ArrayList<>();
    List<CliCommand> commands = childCommand.parse().getFoundedChildCommands();
    if (commands.size() == 0) {
      throw new IllegalArgumentException("A known command must be given");
    } else {

      for (CliCommand subChildCommand : commands) {
        LOGGER_TABUL.info("The command (" + subChildCommand + ") was found");
        switch (subChildCommand.getName()) {
          case TabulWords.ENCRYPT_COMMAND:
            feedbackDataPaths = TabulVaultEncrypt.run(tabular, subChildCommand);
            break;
          case TabulWords.DECRYPT_COMMAND:
            feedbackDataPaths = TabulVaultDecrypt.run(tabular, subChildCommand);
            break;
          default:
            throw new IllegalArgumentException("The sub-command (" + subChildCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(childCommand) + ")");
        }
      }
    }
    return feedbackDataPaths;
  }
}
