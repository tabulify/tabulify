package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

public class CliParserValueAsSystemPropTest {

    @Test
    public void sysPropertyVariableTest() {


        CliCommand cli = CliCommand.createRootWithEmptyInput("myCli");

        String wordName = "--config";
        String systemPropertyName = "CLI_CONFIG_PATH";

        cli.addProperty(wordName)
                .setSystemPropertyName(systemPropertyName);

        String expectedValue = "3";
        System.setProperty(systemPropertyName, expectedValue);

        CliParser cliParser = cli.parse();
        String actualValue = cliParser.getString(wordName);
        Assert.assertEquals("the value in the system property variable must be found", expectedValue, actualValue);


        // Cleaning
        System.clearProperty(systemPropertyName);

    }

  @Test
  public void sysPropertyDefaultVariableTest() {


    String myCli = "myCli";
    CliCommand cli = CliCommand.createRootWithEmptyInput(myCli);

    String config = "config";
    String wordName = "--" + config;
    String systemPropertyName = myCli+"."+config;

    cli.addProperty(wordName);

    String expectedValue = "3";
    System.setProperty(systemPropertyName, expectedValue);

    CliParser cliParser = cli.parse();
    String actualValue = cliParser.getString(wordName);
    Assert.assertEquals("the value in the system property variable must be found", expectedValue, actualValue);


    // Cleaning
    System.clearProperty(systemPropertyName);

  }
}
