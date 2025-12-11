package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CliHelpTest {



  /**
   * A basis help test case on the root command
   */
  @Test(expected = HelpPrintedException.class)
  public void helpOnRootTest() {


    String helpWord = "--help";
    String[] args = {helpWord};
    CliCommand cliCommand = CliCommand.createRoot("helpOnRoot", args);

    String shortName = "-h";
    cliCommand.addWordToLibrary(helpWord)
      .setTypeAsProperty()
      .setShortName(shortName)
      .setDescription("Print this help");

    cliCommand.setHelpWord(helpWord);
        cliCommand.parse();;


  }

  /**
   * The help is asked for the child command
   * The parser must then not exit
   */
  @Test
  public void notExitTest() {

    final String child = "childCommand";
    String helpWord = "--help";
    String[] args = {child, CliParser.PREFIX_LONG_OPTION + helpWord};
    CliCommand cliCommand = CliCommand.createRoot("noExit", args);

    String shortName = "-h";
    cliCommand.addWordToLibrary(helpWord)
      .setTypeAsFlag()
      .setShortName(shortName)
      .setDescription("Print this help");
    cliCommand.setHelpWord(helpWord);

    cliCommand.addChildCommand(child);



    CliParser cliParser = cliCommand.parse();;
    List<CliCommand> commands = cliParser.getFoundedChildCommands();
    Assert.assertEquals(1, commands.size());
    Assert.assertEquals(child, commands.get(0).getName());


  }

  /**
   * The help is asked on the child command
   * The parser must then exit
   */
  @Test(expected = HelpPrintedException.class)
  public void helpOnSubCommandTest() {

    final String child = "childCommand";
    String helpWord = "--help";
    String shortName = "-h";
    String[] args = {child, helpWord};
    CliCommand cliCommand = CliCommand.createRoot("helpOnSubCommand", args);

    cliCommand.addWordToLibrary(helpWord)
      .setTypeAsFlag()
      .setShortName(shortName)
      .setDescription("Print this help");
    cliCommand.setHelpWord(helpWord);

    CliCommand childCommand = cliCommand.addChildCommand(child);



    CliParser cliParser = childCommand.parse();;
    cliParser.getFoundedChildCommands();


  }

  /**
   * The help is asked with the short name
   * The parser must then exit
   */
  @Test(expected = HelpPrintedException.class)
  public void helpOnShortNameTest() {

    final String child = "childCommand";
    String shortName = "-h";
    String[] args = {child, shortName};
    CliCommand cliCommand = CliCommand.createRoot("helpOnShortName", args);

    String helpWord = "--help";
    cliCommand.addWordToLibrary(helpWord)
      .setTypeAsFlag()
      .setShortName(shortName)
      .setDescription("Print this help");
    cliCommand.setHelpWord(helpWord);

    CliCommand childCommand = cliCommand.addChildCommand(child);


    childCommand.parse();;


  }

  /**
   * The help is asked with the short name
   * <p>
   * The help will compete with a mandatory argument
   */
  @Test(expected = HelpPrintedException.class)
  public void helpWithMandatoryParamTest() {

    final String child = "childCommand";
    String shortHelpName = "-h";
    String[] args = {child, shortHelpName};
    CliCommand cliCommand = CliCommand.createRoot("helpWithMandatory", args);

    String helpWord = "--help";
    cliCommand.addWordToLibrary(helpWord)
      .setTypeAsFlag()
      .setShortName(shortHelpName)
      .setDescription("Print this help");
    cliCommand.setHelpWord(helpWord);

    CliCommand childCommand = cliCommand.addChildCommand(child);
    childCommand
      .addProperty("--mandatory")
      .setMandatory(true);


    childCommand.parse();


  }

  @Test
  public void printPropertiesTest() {

    final String name = "--nico";
    String[] args = {};
    CliCommand cli = CliCommand.createRoot("test", args);
    CliWord word = cli.addProperty(name);

    Assert.assertEquals("The printed properties with only a name must be equal",  name + " <" + CliWord.DEFAULT_PRINTED_VALUE + ">", CliUsage.getPrintWord(word));

    final String shortName = "-n";
    word.setShortName(shortName);

    Assert.assertEquals("The printed properties with a shortName must be equal",  shortName + "," +  name + " <" + CliWord.DEFAULT_PRINTED_VALUE + ">", CliUsage.getPrintWord(word));

    final String theNico = "TheNico";
    word.setValueName(theNico);
    Assert.assertEquals("The printed properties with a value name must be equal", shortName + "," +  name + " <" + theNico + ">", CliUsage.getPrintWord(word));


  }
}
