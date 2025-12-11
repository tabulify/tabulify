package com.tabulify.cli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class CliEdgeCaseTest {


  /**
   * Bug when the property name is the same as an argument value
   * In this example, the root command has the property log-level
   * That is also a value argument
   */
  @Test
  public void propertyNameEqualArgumentName() {
    String childCommandName = "attribute-set";
    String logLevelName = "log-level";
    String logLevelProp = "--" + logLevelName;
    String loglevelValue = "info";
    String[] args = {
      childCommandName,
      logLevelName,
      loglevelValue
    };
    CliCommand rootCommand = CliCommand.createRoot("test", args);
    rootCommand.addProperty(logLevelProp);

    CliCommand childCommand = rootCommand.addChildCommand(childCommandName);
    childCommand.addArg("key");
    childCommand.addArg("value");

    CliParser cliParser = childCommand.parse();
    Assertions.assertEquals(2, cliParser.getArgs().size(), "2 args were found");
  }
}
