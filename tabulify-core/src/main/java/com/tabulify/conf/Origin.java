package com.tabulify.conf;

/**
 * Provenance of the value
 */
public enum Origin implements AttributeValue {

  OS("Os environment"),
  DOTENV("Dot env file"),
  CONF("Conf file"),
  SYS("Java system properties"),
  COMMAND_LINE("Cli options"),
  ALL("All variables"), // just used by the cli to show all
  URI("Connection Uri"),
  // Calculated or added during test
  DEFAULT("Default");


  private final String description;


  Origin(String description) {

    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
