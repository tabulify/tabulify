package net.bytle.vertx;

/**
 * The exception throw in a {@link ConfigManager}
 * or {@link ConfigAccessor}
 * This exception is local and not in the type package
 * to reduce its scope, you only need the vertx package
 */
public class ConfigIllegalException extends Exception {


  public ConfigIllegalException(String s) {
    super(s);
  }

  public ConfigIllegalException(String s, Exception e) {
    super(s,e);
  }

}
