package com.tabulify.cli;

import com.tabulify.type.env.OsEnvs;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class CliEnvTest {

  @Test
  public void envTest() {

    String appName = "test";
    String propertyName = "--conf";

    String defaultEnvConfKey = appName+"."+propertyName;
    String defaultEnvConfValue = "value for "+defaultEnvConfKey;
    Map<String, String> testEnv = Map.of(defaultEnvConfKey, defaultEnvConfValue);

    CliCommand cliCommand = CliCommand.createRootWithEmptyInput(appName);
    // Change the default app home word
    cliCommand.addProperty(propertyName);
      cliCommand.setOsEnv(testEnv);

    CliParser cliParser = cliCommand.parse();
    Assert.assertEquals("Equal", defaultEnvConfValue, cliParser.getString(propertyName));


  }

}
