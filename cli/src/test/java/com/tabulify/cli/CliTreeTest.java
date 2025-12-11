package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

public class CliTreeTest {

  @Test
  public void getActive() {

    CliCommand rootCliCommand = CliCommand
      .createRootWithEmptyInput("cliName")
      .setDescription("A hierarchical root cli with children command");

    CliCommand childCommand = rootCliCommand.addChildCommand("activeCommand")
      .setDescription("Child command being build because it has a word (a command)")
      .addChildCommand("yolo")
      .getParentCommand()
      .addArg("arg")
      .getCliCommand();

    rootCliCommand.addChildCommand("passiveCommand")
      .setDescription("Child command not being build because it has no words");

    CliCommand activeCommand = CliTree.getActiveLeafCommand(rootCliCommand);
    Assert.assertEquals("The command are equal",childCommand,activeCommand);

  }
}
