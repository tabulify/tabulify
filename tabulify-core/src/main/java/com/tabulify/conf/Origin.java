package com.tabulify.conf;

/**
 * Provenance of the value
 */
public enum Origin implements AttributeValue {

  OS("Os environment"),
  DOTENV("Dot env file"),
  CONF("Conf file"),
  SYS("Java system properties"),
  RUNTIME("Calculated"),
  COMMAND_LINE("Cli options"),
  ALL("All variables"), // just used by the cli to show all
  ;


  private final String description;


  Origin(String description) {

    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
