package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CliParserValueDefault {


  /**
   * Default value as scalar
   */
  @Test
  public void baseScalarTest() {

    String cliName = "test";
    String[] args = {
      cliName
    };
    CliCommand cliCommand = CliCommand.createRoot(cliName, args);
    String defaultOption = "--default";
    String defaultValue = "10";
    cliCommand.addProperty(defaultOption)
      .addDefaultValue(defaultValue);


    CliParser cliParser = cliCommand.parse();
    Assert.assertEquals("Found words must be good", (Integer) 1, cliParser.getNumberOfFoundWords());
    Assert.assertEquals("Value must be the same as string", defaultValue, cliParser.getString(defaultOption));
    Assert.assertEquals("Value must be the same as integer", Integer.valueOf(defaultValue), cliParser.getInteger(defaultOption));


  }

  /**
   * Default value as collections
   */
  @Test
  public void baseCollectionTest() {

    String cliName = "test";
    String defaultOption = "--default";
    String defaultValue = "10";
    String[] args = {
      cliName
    };
    CliCommand cliCommand = CliCommand.createRoot(cliName, args);
    cliCommand.addProperty(defaultOption)
      .addDefaultValue(defaultValue);


    CliParser cliParser = cliCommand.parse();
    Assert.assertEquals("Found words must be good", (Integer) 1, cliParser.getNumberOfFoundWords());
    List<String> strings = cliParser.getStrings(defaultOption);
    Assert.assertEquals("Size must be one", 1, strings.size());


  }


}
