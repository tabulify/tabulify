package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

/**
 * The parsing of a module (ie a command without arguments
 */
public class CliParserModuleTest {


  @Test
  public void moduleParsing() {
    String childCommand = "childCommand";
    String flag = "--flag";
    String prop = "--prop";
    String propvalue = "propvalue";
    String[] args = {
      childCommand,
      "unknownSubCommand",
      flag,
      prop,
      propvalue,
      "arg"
    };
    CliCommand cliCommand =
      CliCommand.createRoot("test", args)
        .addChildCommand(childCommand)
        .getParentCommand()
        .addFlag(flag)
        .getCliCommand()
        .addProperty(prop)
        .getCliCommand();
    CliParser cliParser = cliCommand.parse();
    Assert.assertEquals("The flag was parsed", true, cliParser.getBoolean(flag));
    Assert.assertEquals("The prop was parsed", propvalue, cliParser.getString(prop));
  }
}
