package net.bytle.type;

public enum Origin implements AttributeValue {

  OS("Os environment", false),
  DOTENV("Dot env file", false),
  USER("User variables", true),
  SYS("Java system properties", false),
  PROJECT("Project variables", true),
  INTERNAL("Internal variables", false),
  COMMAND_LINE("Cli variables file", false),
  ALL("All variables", false), // just used to filter
  ;


  private final String description;
  /**
   * Does this variable is stored by Tabulify
   * If true, a security env such as a password should be hashed with a master password
   */
  private final boolean isTabulifyStore;

  Origin(String description, boolean isTabulifyStore) {

    this.description = description;
    this.isTabulifyStore = isTabulifyStore;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  public boolean isTabulifyStore() {
    return isTabulifyStore;
  }
}
