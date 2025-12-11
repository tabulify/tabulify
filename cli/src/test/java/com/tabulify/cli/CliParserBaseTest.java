package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The parser is test here on one level
 * To see the test on several level see the class {@link CliParserCommandHierarchyTest}
 */
public class CliParserBaseTest {

  private final String OPTION_O = "--OPTION_O";

  /**
   * Test where:
   * * the cli doesn't have any sub command (One level then)
   * * the get value functions are tested
   */
  @Test
  public void baseOneLevelCliTest() {


    String[] args = {
      "arg1",
      OPTION_O,
      "valueO",
      "arg2-0",
      CliParser.PREFIX_LONG_OPTION + "int",
      "2",
      CliParser.PREFIX_LONG_OPTION + "v",
      "arg2-1",
      CliParser.PREFIX_LONG_OPTION + "f",
      "./src/test/java/com/tabulify/cli/CliParserBaseTest.java"
    };
    CliCommand cliCommand = CliCommand.createRoot("myCli", args)
      .setDescription("A one level cli with one command where we are going to parse and get args");

    CliWord verboseOption = cliCommand.addFlag("--v").setDescription("version flag");

    CliWord oArg = cliCommand.addProperty(OPTION_O).setDescription("o option");
    CliWord arg1Arg = cliCommand.addArg("arg1").setDescription("First argument");
    CliWord arg2Arg = cliCommand.addArg("arg2").setDescription("Second Argument Several value");
    String aIntOption = "--int";
    cliCommand.addProperty(aIntOption).setDescription("An option with an integer value");
    String aPathOption = "--f";
    cliCommand.addProperty(aPathOption).setDescription("An option with a local path value");


    CliParser cliParser = cliCommand.parse();

    Integer i = cliParser.getNumberOfFoundWords();
    Assert.assertEquals("All arguments must be recognized", (Integer) 6, i);

    Boolean b = cliParser.getBoolean(verboseOption);
    Assert.assertEquals("The flag v is present by arg", true, b);
    b = cliParser.getBoolean(verboseOption.getName());
    Assert.assertEquals("The flag v is present by name", true, b);

    String arg1Value = cliParser.getString(arg1Arg);
    Assert.assertEquals("The arg arg1 is present by word", "arg1", arg1Value);
    arg1Value = cliParser.getString(arg1Arg.getName());
    Assert.assertEquals("The arg arg1 is present by name", "arg1", arg1Value);
    String arg2Value = cliParser.getString(arg2Arg);
    Assert.assertEquals("The arg arg2 is present", "arg2-0 arg2-1", arg2Value);

    // Multiple arg
    List<String> arg2Values = cliParser.getStrings(arg2Arg);
    Assert.assertEquals("The first value of arg2 is present", "arg2-0", arg2Values.get(0));
    Assert.assertEquals("The second value of arg2 is present", "arg2-1", arg2Values.get(1));

    arg2Values = cliParser.getStrings(arg2Arg.getName());
    List<String> arg2ValuesExpected = new ArrayList<>();
    arg2ValuesExpected.add("arg2-0");
    arg2ValuesExpected.add("arg2-1");
    Assert.assertEquals("The arg arg2 gives the good list", arg2ValuesExpected, arg2Values);

    String oValue = cliParser.getString(oArg);
    Assert.assertEquals("The option OPTION_O is present", "valueO", oValue);

    Integer intValue = cliParser.getInteger(aIntOption);
    Assert.assertEquals("The option int is present and has the expected value by name", (Integer) 2, intValue);
    intValue = cliParser.getInteger(cliCommand.getOrCreateWordOf(aIntOption));
    Assert.assertEquals("The option int is present and has the expected value by word", (Integer) 2, intValue);

    Double dblValue = cliParser.getDouble(aIntOption);
    Assert.assertEquals("The option int is present and has the expected value by name", (Double) 2.0, dblValue);
    dblValue = cliParser.getDouble(cliCommand.getOrCreateWordOf(aIntOption));
    Assert.assertEquals("The option int is present and has the expected value by word", (Double) 2.0, dblValue);

    Path filePath = cliParser.getPath(cliCommand.getOrCreateWordOf(aPathOption));
    Assert.assertTrue("The file exist", Files.exists(filePath));

    String fileContentValue = cliParser.getFileContent(aPathOption);
    Assert.assertNotNull("The file content is not null", fileContentValue);
    Assert.assertTrue("The file content length (" + fileContentValue.length() + ") is bigger than 10", fileContentValue.length() > 10);
    Assert.assertEquals("The file content package declaration can be read", "package com.", fileContentValue.substring(0, 12));
    String fileContentValue2 = cliParser.getFileContent(cliCommand.getOrCreateWordOf(aPathOption));
    Assert.assertEquals("The content must be the same", fileContentValue, fileContentValue2);


  }


  /**
   * The parsing should stops for a module after the first unknown options
   * because the option may be defined by a command
   * at a later stage.
   */
  @Test
  public void parseUnknownOptionModuleTest() {


    CliCommand cliCommand = CliCommand.createRootWithEmptyInput("myCli");


    CliCommand command1Level1 = cliCommand.addChildCommand("commandLevel1");
    CliCommand commandLevel2 = command1Level1.addChildCommand("commandLevel2");

    String[] args = {
      command1Level1.getName(),
      commandLevel2.getName(),
      CliParser.PREFIX_LONG_OPTION + "whatever",
      "value",
      "arg"
    };


    CliParser cliParser = command1Level1.parse(args);
    Assert.assertEquals("The value of the words found must be goods", 2, (int) cliParser.getNumberOfFoundWords());
    Assert.assertEquals("The value of the options must be goods", 0, cliParser.getOptions().size());
    Assert.assertEquals("The value of the commands must be good", 1, cliParser.getFoundedChildCommands().size());


  }

  /**
   * The parsing should stops for a module after the first unknown options
   * because the option may be defined by a command
   * at a later stage.
   */
  @Test(expected = IllegalArgumentException.class)
  public void parseUnknownOptionCommandTest() {


    String commandLevel1 = "commandLevel1";
    String commandLevel21 = "commandLevel2";
    String[] args = {
      commandLevel1,
      commandLevel21,
      CliParser.PREFIX_LONG_OPTION + "youpla",
      "yolo",
      "arg"
    };
    CliCommand cliCommand = CliCommand.createRoot("myCli", args);
    CliCommand command1Level1 = cliCommand.addChildCommand(commandLevel1);
    CliCommand commandLevel2 = command1Level1.addChildCommand(commandLevel21);


    CliParser cliParser = commandLevel2.parse();
    cliParser.getString(""); // force a parse


  }

  /**
   * A known property should be allowed before the command
   * This is to be able to create a wrapper script with some global predefined
   * property (ie passphrase for instance)
   */
  @Test
  public void knownPropertyBeforeCommand() {

    String commandLevel1 = "commandLevel1";
    String commandLevel21 = "commandLevel2";
    String knownProperty = "--knownProperty";
    String knownFlag = "--knownFlag";
    String knownPropertyValue = "yolo";
    String[] args = {
      knownProperty,
      knownPropertyValue,
      knownFlag,
      commandLevel1,
      commandLevel21,
    };
    CliCommand cliCommand = CliCommand.createRoot("myCli", args);
    cliCommand.addFlag(knownFlag);
    cliCommand.addProperty(knownProperty);
    CliCommand command1Level1 = cliCommand.addChildCommand(commandLevel1);
    CliCommand commandLevel2 = command1Level1.addChildCommand(commandLevel21);


    CliParser cliParser = commandLevel2.parse();
    String value = cliParser.getString(knownProperty);
    Assert.assertEquals("The known property value should be the good one", knownPropertyValue, value);
    Assert.assertTrue("The known flag value should be the good one", cliParser.getBoolean(knownFlag));

  }

  /**
   * If the command is a module , the processing is strict before
   * we encounter the word of the command
   * and discard any unknown words afterwards
   * <p>
   * This is to avoid that a value of an unknown property option
   * will be taken for a command
   * <p>
   * Example: the word `count` is a command
   * and can be also the value of the `attributes` property
   * of the `data list` command
   * <p>
   * You can have:
   * * `data list --attribute count`
   * * `data count`
   */
  @Test
  public void commandValueCanAlsoBeUsedAsPropertyValue() {

    String commandDataWord = "data";
    String countWord = "count";
    String commandListWord = "list";
    String propertyListWord = "--attribute";

    CliCommand cliCommand = CliCommand.createRootWithEmptyInput("tabli");
    CliCommand commandData = cliCommand.addChildCommand(commandDataWord);
    CliCommand commandList = commandData.addChildCommand(commandListWord);
    CliCommand commandCount = commandData.addChildCommand(countWord);


    /**
     * First - Base
     *
     */
    String[] args = {
      commandDataWord,
      countWord,
    };
    CliParser cliParser = commandCount.parse(args);
    Integer numberOfFoundWords = cliParser.getNumberOfFoundWords();
    Assert.assertEquals("The command are there", (Integer) 2, numberOfFoundWords);

    /**
     * Third -the count word should not be seen as a command
     */
    args = new String[]{
      commandDataWord,
      commandListWord,
      propertyListWord,
      countWord
    };
    cliParser = commandData.parse(args);
    numberOfFoundWords = cliParser.getNumberOfFoundWords();
    Assert.assertEquals("The 2 command are there, the other were discarded because the property is an unknown option", (Integer) 2, numberOfFoundWords);
    Assert.assertEquals("The count word was not seen as a command because of the option before", 1, cliParser.getFoundedChildCommands().size());

    /**
     * Building further the command list, it should work
     */
    commandList.addProperty(propertyListWord);
    args = new String[]{
      commandDataWord,
      commandListWord,
      propertyListWord,
      countWord
    };
    cliParser = commandList.parse(args);
    numberOfFoundWords = cliParser.getNumberOfFoundWords();
    Assert.assertEquals("The command are there and the property", (Integer) 3, numberOfFoundWords);
    Assert.assertEquals("The property has the good value", countWord, cliParser.getString(propertyListWord));


  }
}
