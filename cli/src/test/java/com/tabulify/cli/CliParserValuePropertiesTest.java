package com.tabulify.cli;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class CliParserValuePropertiesTest {

  /**
   * Test the {@link CliParser#getProperties(String)} method
   */
  @Test
  public void getProperties() {

    /**
     * The variables
     */
    String entry = "--entry";
    String value = "value";
    String key = "key";
    String key2 = "key2";
    String value2 = "value=2";
    String keyWithoutValue = "keyWithoutValue";

    String[] args = {
       entry,
      key + "=" + value,
      entry,
      key2 + "=" + value2,
       entry,
      keyWithoutValue
    };

    /**
     * Cli definition
     */
    CliCommand cli = CliCommand.createRoot("cli", args);
    cli.addProperty(entry);

    /**
     * Parse and get
     */
    CliParser cliParser = cli.parse();
    Map<String, String> entries = cliParser.getProperties(entry);

    /**
     * Test
     */
    Assert.assertEquals("size", 3, entries.size());
    Assert.assertEquals("key", value, entries.get(key));
    Assert.assertEquals("key2", value2, entries.get(key2));
    Assert.assertNull(keyWithoutValue, entries.get(keyWithoutValue));

  }
}
