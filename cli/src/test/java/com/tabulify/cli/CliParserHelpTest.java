package com.tabulify.cli;

import org.junit.Test;

public class CliParserHelpTest {

  public static final String LONG_HELP_WORD = "--help";
  public static final String SHORT_HELP_WORD = "-h";


  @Test(expected = HelpPrintedException.class)
  public void helpLOngOptionTest() {

    String[] args = {
      LONG_HELP_WORD
    };
    CliCommand cliCommand = CliCommand.createRoot("myCli", args)
      .setDescription("A one level cliCommand where we are going to test the help")
      .setHelpWord(LONG_HELP_WORD);

    cliCommand
      .addFlag(LONG_HELP_WORD)
      .setDescription("help flag")
      .setShortName(SHORT_HELP_WORD);


    cliCommand.parse();

  }

  @Test(expected = HelpPrintedException.class)
  public void helpShortOptionTest() {

    String[] args = {
      SHORT_HELP_WORD
    };
    CliCommand cliCommand = CliCommand.createRoot("myCli", args)
      .setDescription("A one level cliCommand where we are going to test the help")
      .setHelpWord(LONG_HELP_WORD);

    cliCommand
      .addFlag(LONG_HELP_WORD)
      .setDescription("help flag")
      .setShortName(SHORT_HELP_WORD);

    cliCommand.parse();

  }
}
