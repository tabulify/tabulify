package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CliParserCommandAliasTest {

  private static final String OPTION_O = "o";
  private static final String CHILD_COMMAND = "child";
  private static final String CHILD_COMMAND_2 = "CHILD_2";


  /**
   * The cli root is passed to the
   * Two levels then with a global option
   */
  @Test
  public void baseAliasName() {

    final String cliName = "cliName";
    String[] args = {
      "short",
    };
    CliCommand rootCliCommand =
      CliCommand.createRoot(cliName, args)
      .setDescription("A cli with an alias");

    rootCliCommand.addChildCommand("name")
      .setShortName("short");


    CliParser cliParser = rootCliCommand.parse();
    List<CliCommand> command = cliParser.getFoundedChildCommands();
    Assert.assertEquals("The command with the alias was found", 1, command.size());

  }

}
