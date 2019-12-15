package net.bytle.api;

public enum ConfKeys {

  // Because they are constants, the names of an enum type's fields are in uppercase letters.
  PING_RESPONSE,
  HOST,
  PORT,
  PATH;

  @Override
  public String toString() {
    return name().toLowerCase();
  }

}
