package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

public class CliParserFlagTest {

  /**
   * If the flag is present the default value is given
   */
  @Test
  public void flagTest() {

    final String noStrictFlagName = "--nostrict";
    String[] args = {noStrictFlagName};
    CliCommand cli = CliCommand.createRoot("test", args);

    cli.addFlag(noStrictFlagName)
      .setDescription("A flag");

    CliParser cliParser = cli.parse();
    Boolean noStrictFlag = cliParser.getBoolean(noStrictFlagName);
    Assert.assertEquals("The flag should give its presence", true, noStrictFlag);

  }

  /**
   * If a default value is given, the flag reverse it
   */
  @Test
  public void flagDefaultValueTest() {

    CliCommand cli = CliCommand.createRootWithEmptyInput("test");

    final String noStrictFlagName = "--nostrict";
    cli.addFlag(noStrictFlagName)
      .setDescription("A flag")
      .setEnvName("DB_CONFIG_FILE")
      .setDefaultValue(false);


    CliParser cliParser = cli.parse();
    Boolean noStrictFlag = cliParser.getBoolean(noStrictFlagName);
    Assert.assertEquals("The flag should give the default value false", false, noStrictFlag);

    String[] args = new String[]{noStrictFlagName};
    ;
    cliParser = cli.parse(args);
    noStrictFlag = cliParser.getBoolean(noStrictFlagName);
    Assert.assertEquals("The flag (" + noStrictFlagName + ") should give the reverse of the default value ", true, noStrictFlag);

  }
}
