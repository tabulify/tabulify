package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

public class CliParserValueMultipleTest {

  @Test
  public void multipleOptionValues() {

    String optionWord = "--option";
    String[] args = {
      optionWord,
      "1",
      optionWord,
      "2"
    };
    CliCommand cliCommand = CliCommand.createRoot("test", args);
    cliCommand.addProperty(optionWord);
    CliParser cliParser = cliCommand.parse();
    Assert.assertEquals("Two values for the option", 2, cliParser.getStrings(optionWord).size());


  }
}
