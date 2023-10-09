package net.bytle.cli;

import java.util.List;

/**
 * Static function that are used against a {@link CliCommand tree of command}
 */
public class CliTree {


  /**
   * The active command is the last leaf command in the {@link CliCommand cliCommand Tree}
   * that has words
   *
   * @param cliCommand
   * @return
   */
  public static CliCommand getActiveLeafCommand(CliCommand cliCommand) {
    CliCommand activeCliCommand = cliCommand;
    for (CliCommand childCommand : cliCommand.getChildCommands()) {
      List<CliWord> words = childCommand.getLocalWords();
      if (words.size() != 0) {
        activeCliCommand = childCommand;
        for (CliCommand subChild : childCommand.getChildCommands()) {
          if (subChild.getLocalWords().size()>0) {
            activeCliCommand = getActiveLeafCommand(childCommand);
          }
        }
      }
    }
    return activeCliCommand;
  }

}
