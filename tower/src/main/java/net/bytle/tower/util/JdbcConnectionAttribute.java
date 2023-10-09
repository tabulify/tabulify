package net.bytle.tower.util;

import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.vertx.ConfigAccessor;

public enum JdbcConnectionAttribute {

  URL("jdbc.url", "jdbc:postgresql://localhost:5432/postgres"),
  POOL_SIZE("jdbc.pool_size", 16),
  PASSWORD("jdbc.password", "welcome"),
  USER("jdbc.user", "postgres"),
  /**
   * The schema search path for object when they are not specified
   * comma-separated list of schema names
   * <a href="https://www.postgresql.org/docs/current/runtime-config-client.html#GUC-SEARCH-PATH">Doc</a>
   */
  SCHEMA_PATH("jdbc.schema_path", null);
  private final String key;
  private final Object defaultValue;

  JdbcConnectionAttribute(String configKey, Object defaultValue) {
    this.key = configKey;
    this.defaultValue = defaultValue;
  }

  public <T> T getValue(ConfigAccessor config, Class<T> clazz) {
    Object conf = config.getValue(key);
    try {
      return Casts.cast(conf, clazz);
    } catch (CastException e) {
      throw new ClassCastException(e.getMessage());
    }
  }

  public Object getDefault() {
    return this.defaultValue;
  }

  public String getKey() {
    return this.key;
  }

  public <T> T getValueOrDefault(ConfigAccessor config, Class<T> clazz) {
    Object conf = config.getValue(key, defaultValue);
    try {
      return Casts.cast(conf, clazz);
    } catch (CastException e) {
      throw new ClassCastException(e.getMessage());
    }
  }
}
