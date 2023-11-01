package net.bytle.vertx;

public enum JdbcConnectionAttribute {

  URL("url", "jdbc:postgresql://localhost:5432/postgres"),
  POOL_SIZE("pool_size", 2),
  PASSWORD("password", "welcome"),
  USER("user", "postgres"),
  /**
   * The schema search path for object when they are not specified
   * comma-separated list of schema names
   * <a href="https://www.postgresql.org/docs/current/runtime-config-client.html#GUC-SEARCH-PATH">Doc</a>
   */
  SCHEMA_PATH("schema_path", null);
  private final String key;
  private final Object defaultValue;

  JdbcConnectionAttribute(String configKey, Object defaultValue) {
    this.key = configKey;
    this.defaultValue = defaultValue;
  }

  public Object getDefault() {
    return this.defaultValue;
  }

  public String getKey() {
    return this.key;
  }


}
