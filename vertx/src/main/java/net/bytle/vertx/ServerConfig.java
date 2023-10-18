package net.bytle.vertx;

import io.vertx.core.json.JsonObject;

public class ServerConfig {

  /**
   * Listen from all hostname
   * On ipv4 and Ipv6.
   * The wildcard implementation depends on the language
   * and in Java it works for the 2 Ip formats.
   */
  public static final String WILDCARD_IPV4_ADDRESS = "0.0.0.0";
  @SuppressWarnings("unused")
  public static final String WILDCARD_IPV6_ADDRESS = "[::]";
  private final ConfigAccessor configAccessor;

  public ServerConfig(ConfigAccessor configAccessor) {
    this.configAccessor = configAccessor;
  }

  public int getListeningPort(Integer defaultPort) {
    if(defaultPort==null){
      defaultPort = 80;
    }
    return configAccessor.getInteger(ServerProperties.PORT.toString(), defaultPort);
  }


  public  String getListeningHost() {
    return configAccessor.getString(ServerProperties.HOST.toString(), WILDCARD_IPV4_ADDRESS);
  }





  /**
   * This port is the port that is all external communication
   * (in email, in oauth callback, ...)
   */
  public static int getPublicPort(JsonObject config) {
    return config.getInteger(ServerProperties.PUBLIC_PORT.toString(), 80);
  }

  public static ServerConfig create(ConfigAccessor configAccessor) {
    return new ServerConfig(configAccessor);
  }
}
