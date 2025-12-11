package com.tabulify.conf;

/**
 * Provenance of the value
 */
public enum Origin implements AttributeValue {

  OS("Os environment", false),
  DOTENV("Dot env file", false),
  MANIFEST("Conf file", true),
  PIPELINE("Pipeline file", false),
  SYS("Java system properties", false),
  COMMAND_LINE("Cli options", false),
  ALL("All variables", false), // just used by the cli to show all
  URI("Connection Uri", false),
  /**
   * The default as when all other origins could not be set
   * (Meaning for now created at runtime via setters)
   * It does not mean the default value as it should never be set
   * These attributes are going to the conf vault
   * because we create connection and services at runtime)
   */
  DEFAULT("Default", true);


  private final String description;
  // should we store this attribute in a conf vault dump
  private final boolean inVaultConf;


  Origin(String description, boolean inVaultConf) {

    this.description = description;
    this.inVaultConf = inVaultConf;

  }


  @Override
  public String getDescription() {
    return this.description;
  }


  public boolean isInVaultConf() {
    return inVaultConf;
  }
}
