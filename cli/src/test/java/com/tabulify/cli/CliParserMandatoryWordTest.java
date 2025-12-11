package com.tabulify.cli;

import org.junit.Test;

/**
 * A mandatory option or argument word
 */
public class CliParserMandatoryWordTest {


  @Test(expected = IllegalArgumentException.class)
  public void basicTest() {

    String[] args = {};
    CliCommand cli = CliCommand.createRoot("test", args);
    String testOption = "--test";
    cli.addProperty(testOption).setMandatory(true);

    CliParser cliParser = cli.parse();
    cliParser.getString(testOption);


  }
}
