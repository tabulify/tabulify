package net.bytle.vertx;

/**
 * Properties for a net/http server
 */
public enum ServerProperties {

  // Because they are constants, the names of an enum type's fields are in uppercase letters.
  HOST("host"),
  LISTENING_PORT("port"), // the private listening port
  PUBLIC_PORT("port.public"); // the public port (the proxy port, normally 80)


  private final String confKey;

  ServerProperties(String confKey) {
    this.confKey = confKey;
  }

  @Override
  public String toString() {
    return confKey.toLowerCase();
  }

}
