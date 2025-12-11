package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

public class CliWordTest {

  @Test
  public void IdTest() {

    String root = "root";
    String rootProp = "--rootProp";
    String subCommand = "command";
    String subCommandProp = "--subCommandProp";
    String[] args = {
      root
    };
    CliCommand cliCommand = CliCommand.createRoot(root, args);
    CliWord rootPropWord = cliCommand.addProperty(rootProp);
    CliCommand childCommand = cliCommand.addChildCommand(subCommand);
    CliWord subCommandPropWord = childCommand.addProperty(subCommandProp);

    /**
     * Id test
     */
    Assert.assertEquals("Root must be the same", root, cliCommand.getId());
    Assert.assertEquals("RootProp must be the same", root+CliWord.PROPERTY_NAME_SEPARATOR+rootProp, rootPropWord.getId());
    Assert.assertEquals("SubCommand must be the same", root+CliWord.PROPERTY_NAME_SEPARATOR+subCommand, childCommand.getId());
    Assert.assertEquals("SubCommandProp must be the same", root+CliWord.PROPERTY_NAME_SEPARATOR+subCommand+CliWord.PROPERTY_NAME_SEPARATOR+subCommandProp, subCommandPropWord.getId());
    Assert.assertEquals("Relative SubCommandProp must be the same", subCommand+CliWord.PROPERTY_NAME_SEPARATOR+subCommandProp, subCommandPropWord.getRelativeId());


  }
}
