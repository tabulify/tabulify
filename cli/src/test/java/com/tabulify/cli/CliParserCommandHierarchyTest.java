package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CliParserCommandHierarchyTest {

  private static final String OPTION_O = "--o";
  private static final String CHILD_COMMAND = "child";
  private static final String CHILD_COMMAND_2 = "CHILD_2";


  /**
   * The cli root is passed to the
   * Two levels then with a global option
   */
  @Test
  public void twoLevelClassInitializationTest() {

    String[] args = {
      CHILD_COMMAND,
      OPTION_O,
      "ovalue",
      "arg1",
      "arg2",
    };
    CliCommand rootCliCommand = CliCommand.createRoot("cliName", args)
      .setDescription("A hierarchical root cli with children command");
    rootCliCommand.addFlag("--v").setDescription("version flag");
    rootCliCommand.addFlag("--h").setDescription("help flag");

    rootCliCommand.addWordToLibrary(OPTION_O)
      .setTypeAsProperty()
      .setDescription("An O option");

    rootCliCommand.addChildCommand(CHILD_COMMAND)
      .setDescription("Child command desc");


    CliParser cliParser = rootCliCommand.parse();

    Assert.assertEquals("Must be 0 args", 0, cliParser.getArgs().size());
    Assert.assertEquals("Must be 0 option", 0, cliParser.getOptions().size());
    Assert.assertEquals("The cli was found", CHILD_COMMAND, cliParser.getCommand().getName());

    for (CliCommand cliCommand : cliParser.getFoundedChildCommands()) {
      switch (cliCommand.getName()) {
        case CHILD_COMMAND:
          // Simulation as if you pass the args and the child cliCommand to another class or method
          CliWord oArg = cliCommand.addProperty(OPTION_O);
          CliWord arg = cliCommand.addArg("args");
          CliUsage.print(cliCommand);
          Assert.assertEquals("Their is 3 options", 3, cliCommand.getAllOptions().size());
          cliParser = cliCommand.parse();

          Assert.assertEquals("ChildCli: Must be one args", 1, cliParser.getArgs().size());
          Assert.assertEquals("ChildCli: Must be one args with two values", 2, cliParser.getStrings(arg).size());
          Assert.assertEquals("ChildCli: Must be ovalue", "ovalue", cliParser.getString(oArg));

          break;
      }
    }
  }

  /**
   * Test where the cli has a sub command
   * Two levels then with a global option
   */
  @Test
  public void twoCommandTest() {

    final String childCommand1Word = "childCommand1";
    final String childCommand2Word = "childCommand2";
    String[] args = {
      childCommand2Word,
      childCommand1Word,
      "arg2"
    };
    CliCommand rootCliCommand = CliCommand.createRoot("cliName", args)
      .setDescription("A cliCommand with two command");

    rootCliCommand.addChildCommand(childCommand1Word)
      .setDescription("Child command 1");
    CliCommand command2 = rootCliCommand.addChildCommand(childCommand2Word)
      .setDescription("Child command 2");


    CliParser cliParser = rootCliCommand.parse();

    List<CliCommand> cliCommands = cliParser.getFoundedChildCommands();
    Assert.assertEquals("Two cliCommands were found", 2, cliCommands.size());

    CliCommand cliCommand = cliParser.getCommand();
    Assert.assertEquals("Same cliCommand", cliCommand, command2);

    cliCommand = cliParser.getCommand(childCommand2Word);
    Assert.assertEquals("Same cliCommand by word", cliCommand, command2);

  }

  /**
   * Test where the cli has a sub command
   * Two levels then with a global option
   */
  @Test
  public void twoLevelTest() {

    CliCommand rootCliCommand = CliCommand.createRootWithEmptyInput("cliName")
      .setDescription("A hierarchical root cli with children command");
    CliWord firstRootOption = rootCliCommand.addFlag("--v").setDescription("version flag");
    CliWord secondRootOption = rootCliCommand.addFlag("--h").setDescription("help flag");

    rootCliCommand.addWordToLibrary(OPTION_O).setDescription("An O option");

    CliCommand childRootCommand = rootCliCommand.addChildCommand("CHILD_COMMAND")
      .setDescription("Child command desc");
    childRootCommand.addArg("argfiles");
    CliWord thirdOption = childRootCommand.addProperty(OPTION_O);

    Assert.assertEquals("global option size", 1, rootCliCommand.getGlobalWords().size());

    CliCommand expectedRootCliCommand = childRootCommand.getRootCommand();
    Assert.assertEquals("The root object must be equals", expectedRootCliCommand, rootCliCommand);

    String usage = CliUsage.get(rootCliCommand);
    Assert.assertNotNull("The root usage must be not null and give no error", usage);
    // System.out.println(usage);
    String usageChild = CliUsage.get(childRootCommand);
    Assert.assertNotNull("The child usage must be not null and give no error", usageChild);
    System.out.println(usageChild);

    List<CliWord> options = childRootCommand.getAllOptions();
    Assert.assertEquals("The child cli must take the cli of its parent", (Integer) 3, (Integer) options.size());
    // Alphabetical order
    Assert.assertEquals("The h option", options.get(0), secondRootOption);
    Assert.assertEquals("The v option", options.get(1), firstRootOption);
    Assert.assertEquals("The 0 option", options.get(2), thirdOption);

    List<CliCommand> cliCommands = childRootCommand.getParentsCommands();
    Assert.assertEquals("The child cli must take the cli of its parent", (Integer) 1, (Integer) cliCommands.size());
    Assert.assertEquals("The first one is the root (cli)", rootCliCommand, cliCommands.get(0));


  }

  /**
   * The options are set on different command
   * There is a two command hierarchy
   */
  @Test
  public void nCommandNOptionsDifferentLevelsTest() {

    String OPTION1 = "--option1";
    String option1Value = "option1Value";
    String OPTION2 = "--option2";
    String option2Value = "option2Value";
    String[] args = {
      CHILD_COMMAND,
      CHILD_COMMAND_2,
      OPTION1,
      option1Value,
      OPTION2,
      option2Value,
    };
    CliCommand rootCliCommand = CliCommand.createRoot("cliName", args)
      .setDescription("A test with n options and n command");

    rootCliCommand.addProperty(OPTION1).setDescription("option1");


    CliCommand childCommand = rootCliCommand.addChildCommand(CHILD_COMMAND);
    CliCommand childCommand2 = childCommand.addChildCommand(CHILD_COMMAND_2);

    childCommand2.addProperty(OPTION2).setDescription("option2");

    CliParser cliParser = childCommand2.parse();

    Assert.assertEquals("Option2 was found", option2Value, cliParser.getString(OPTION2));
    Assert.assertEquals("Option1 was found", option1Value, cliParser.getString(OPTION1));
    Assert.assertEquals("Command1 was found", CHILD_COMMAND, cliParser.getCommand(CHILD_COMMAND).getName());
    Assert.assertEquals("Command2 was found", CHILD_COMMAND_2, cliParser.getCommand(CHILD_COMMAND_2).getName());

  }


}
