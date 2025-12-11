package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

public class CliUsageTest {

  private static final String OPTION_O = "--output";
  private static final String CHILD_COMMAND_2 = "childCommand2";
  private static final String CHILD_COMMAND_3 = "childCommand3";
  private final String CHILD_COMMAND = "childCommand";

  /**
   * A test that prints the usage from the root command (ie the cli)
   * <p>
   * https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html
   * // --help and --version
   */
  @Test
  public void usageRootCommandTest() {

    CliCommand cliCommand = CliCommand.createRootWithEmptyInput("cliName")
      .setDescription("A hierarchical root cliCommand with children command");

    cliCommand.addChildCommand(CHILD_COMMAND);
    cliCommand.addChildCommand("childCommand2");

    cliCommand.addProperty("--help")
      .setShortName("-h")
      .setDescription("Show the help");
    cliCommand.addProperty("--version")
      .setDescription("Show the version");
    cliCommand.addProperty("--option.o")
      .setConfigFileKey("option.o")
      .setDescription("An option that expects a value");

    cliCommand.addProperty("--p")
      .setValueName("path");
    cliCommand.addArg("arg1");
    cliCommand.addArg("bigargName");

    String usage = CliUsage.get(cliCommand);
    Assert.assertNotNull("The usage must be not null and give no error", usage);
    System.out.println(usage);

  }

  @Test
  public void CommandHierarchyTwoLevelTest() {

    CliCommand rootCliCommand = CliCommand.createRootWithEmptyInput("cliName")
      .setDescription("A hierarchical root cli with children command");
    rootCliCommand.addFlag("--version")
      .setDescription("version flag");
    rootCliCommand.addFlag("--help")
      .setShortName("-h")
      .setDescription("help flag");

    rootCliCommand.addWordToLibrary(OPTION_O)
      .setTypeAsProperty()
      .setDescription("An O option")
      .setShortName("-o");

    rootCliCommand.addChildCommand(CHILD_COMMAND)
      .setDescription("Child command desc");

    String[] args = {
      CHILD_COMMAND,
      CHILD_COMMAND_2,
      "-" + OPTION_O,
      "ovalue",
      "arg1",
      "arg2",
    };

    CliParser cliParser = rootCliCommand.parse();
    for (CliCommand cliCommand : cliParser.getFoundedChildCommands()) {
      switch (cliCommand.getName()) {
        case CHILD_COMMAND:
          // Simulation as if you pass the args and the child cliCommand to another class or method
          cliCommand.addChildCommand(CHILD_COMMAND_2);
          cliCommand.addChildCommand(CHILD_COMMAND_3);
          cliCommand.addProperty(OPTION_O);
          cliCommand.addArg("args");

          // Bug
          CliParser cliParserChildCommand = cliCommand.parse();
          cliParserChildCommand.getFoundedChildCommands();
          Assert.assertEquals("One local option", 1, cliCommand.getLocalOptions().size());

          CliUsage.print(cliCommand);
          break;
      }
    }
  }

  @Test
  public void UsagePrintLevelTest() {

    String[] args = {
      CHILD_COMMAND,
      CHILD_COMMAND_2,
      OPTION_O,
      "ovalue",
      "arg1",
      "arg2",
    };
    CliCommand rootCliCommand = CliCommand.createRoot("usagePrintLevel",args)
      .setDescription("A description");
    rootCliCommand.addFlag("--version")
      .setDescription("version flag");
    rootCliCommand.addFlag("--help")
      .setShortName("-h")
      .setDescription("help flag");

    rootCliCommand.addWordToLibrary(OPTION_O)
      .setTypeAsProperty()
      .setDescription("An O option")
      .setShortName("-o");

    rootCliCommand.addChildCommand(CHILD_COMMAND)
      .setDescription("Child command desc");


    CliParser cliParser = rootCliCommand.parse();

    for (CliCommand cliCommand : cliParser.getFoundedChildCommands()) {
      switch (cliCommand.getName()) {
        case CHILD_COMMAND:
          cliCommand.addProperty(OPTION_O);
          CliWordGroup group1 = CliWordGroup.get(cliCommand, "Group1")
            .setImportanceLevel(2);
          cliCommand.addProperty("--option2")
            .setGroup(group1);
          cliCommand.addArg("args");


          CliUsage.print(cliCommand);
          break;
      }
    }
  }


  @Test
  public void cliChain() {

    String[] args = {};
    CliCommand cliCommand = CliCommand
      .createRoot("cliChain", args)
      .addChildCommand("sub1")
      .addChildCommand("sub2");

    String fullChain = CliUsage.getFullChainOfCommand(cliCommand);
    Assert.assertEquals("cliChain sub1 sub2", fullChain);

  }
}
