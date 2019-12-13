package net.bytle.api;

public enum ConfKeys {
  PING_RESPONSE,
  HOST,
  PORT,
  POKE_API_PATH;

  @Override
  public String toString() {
    return name().toLowerCase();
  }

}
