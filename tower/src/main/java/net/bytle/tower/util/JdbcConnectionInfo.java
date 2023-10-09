package net.bytle.tower.util;

import io.vertx.pgclient.PgConnectOptions;
import net.bytle.exception.InternalException;
import net.bytle.vertx.ConfigAccessor;

import java.util.Arrays;

/**
 * A wrapper around connection information
 * <p>
 * It's passed to synchronize all JDBC library
 * with the same data (ie {@link JdbcPoolCs}, {@link JdbcSchemaManager}, {@link net.bytle.db.Tabular}, ..)
 */
public class JdbcConnectionInfo {
  private String url;
  private String user;
  private String password;
  private Integer maxPoolSize;
  private String schemaPath;

  public static JdbcConnectionInfo createFromJson(ConfigAccessor config) {
    String url = JdbcConnectionAttribute.URL.getValue(config, String.class);
    if (url == null) {
      throw new InternalException("The url configuration was not found. You should add it with the " + JdbcConnectionAttribute.URL.getKey() + " attribute.");
    }
    String user = JdbcConnectionAttribute.USER.getValue(config, String.class);
    if (user == null) {
      throw new InternalException("The user configuration was not found. You should add it with the " + JdbcConnectionAttribute.USER.getKey() + " attribute.");
    }
    String password = JdbcConnectionAttribute.PASSWORD.getValue(config, String.class);
    if (password == null) {
      throw new InternalException("The password configuration was not found. You should add it with the " + JdbcConnectionAttribute.PASSWORD.getKey() + " attribute.");
    }
    String workingSchema = JdbcConnectionAttribute.SCHEMA_PATH.getValue(config, String.class);
    if (workingSchema == null) {
      throw new InternalException("The schema path configuration was not found. You should add it with the " + JdbcConnectionAttribute.SCHEMA_PATH.getKey() + " attribute.");
    }
    Integer maxPoolSize = JdbcConnectionAttribute.POOL_SIZE.getValueOrDefault(config, Integer.class);

    return new JdbcConnectionInfo()
      .setUrl(url)
      .setUser(user)
      .setPassword(password)
      .setMaxPoolSize(maxPoolSize)
      .setSchemaPath(workingSchema);
  }

  private JdbcConnectionInfo setSchemaPath(String schemaPath) {
    this.schemaPath = schemaPath;
    return this;
  }


  private JdbcConnectionInfo setMaxPoolSize(Integer value) {
    this.maxPoolSize = value;
    return this;
  }

  private JdbcConnectionInfo setPassword(String value) {
    this.password = value;
    return this;
  }

  private JdbcConnectionInfo setUser(String value) {
    this.user = value;
    return this;
  }

  private JdbcConnectionInfo setUrl(String value) {
    this.url = value;
    return this;
  }

  public String getUrl() {
    return this.url;
  }

  public String getUser() {
    return this.user;
  }

  public String getPassword() {
    return this.password;
  }

  public int getMaxPoolSize() {
    return this.maxPoolSize;
  }

  /**
   * postgresql://[user[:password]@][netloc][:port][,...][/dbname][?param1=value1&...]
   *
   * @return a postgresUri used in {@link PgConnectOptions#fromUri(String)}
   */
  public String getPostgresUri() {
    return this.url.substring("jdbc:".length());
  }

  public String getSchemaPath() {
    return this.schemaPath;
  }

  /**
   * The default, working schema
   * Utility function to indicate what is the working schema
   * <p>
   * For PG, the current_schema returns the name of the schema that is at the front of the search path
   * <a href="https://www.postgresql.org/docs/current/ddl-schemas.html">...</a>
   * @return the working schema
   */
  @SuppressWarnings("unused")
  public String getWorkingSchema() {
    return Arrays
      .stream(this.schemaPath.split(","))
      .map(String::trim)
      .findFirst()
      .orElse("");
  }

}
