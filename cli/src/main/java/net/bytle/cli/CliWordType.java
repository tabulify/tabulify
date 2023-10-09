package net.bytle.cli;

public enum CliWordType {

  COMMAND, // command
  ARGUMENT, // operand
  PROPERTY, // A key/value
  FLAG; // A boolean


  @Override
  public String toString() {
   return super.toString().toLowerCase();
  }

}
