package net.bytle.vertx;

import io.vertx.pgclient.PgConnectOptions;
import net.bytle.exception.NullValueException;
import net.bytle.type.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * A wrapper around connection information
 * <p>
 * It's passed to synchronize all JDBC library
 * with the same data (ie {@link JdbcPostgres}, {@link JdbcSchemaManager}, ..)
 */
public class JdbcConnectionInfo {

  static Logger LOGGER = LogManager.getLogger(JdbcConnectionInfo.class);
  private String url;
  private String user;
  private String password;
  private Integer maxPoolSize;
  private String schemaPath;
  private String databaseName;

  public static JdbcConnectionInfo createFromJson(String prefix, ConfigAccessor config) {

    String urlKey = prefix + "." + JdbcConnectionAttribute.URL.getKey();
    String url = config.getString(urlKey);
    if (url == null) {
      url = (String) JdbcConnectionAttribute.URL.getDefault();
      LOGGER.info("The jdbc url configuration (" + urlKey + ") was not found and got the default value: " + url);
    } else {
      LOGGER.info("The jdbc url configuration (" + urlKey + ") was found and got the value: " + url);
    }


    // jdbc:hyperion:sqlserver://
    int endIndex = url.indexOf("://");
    String scheme;
    if(endIndex!=-1){
     scheme = url.substring(0, endIndex).toLowerCase();
    } else {
      scheme = url;
    }
    String databaseName = scheme.replace("jdbc:","");
    LOGGER.info("The database name got the value: " + databaseName);

    String userKey = prefix + "." + JdbcConnectionAttribute.USER.getKey();
    String user = config.getString(userKey);
    if (user == null) {
      user = (String) JdbcConnectionAttribute.USER.getDefault();
      LOGGER.info("The jdbc user configuration (" + userKey + ") was not found and got the default value: " + user);
    } else {
      LOGGER.info("The jdbc user configuration (" + userKey + ") was found and got the value: " + user);
    }

    String passwordKey = prefix + "." + JdbcConnectionAttribute.PASSWORD.getKey();
    String password = config.getString(passwordKey);
    if (password == null) {
      password = (String) JdbcConnectionAttribute.PASSWORD.getDefault();
      LOGGER.info("The jdbc password configuration (" + passwordKey + ") was not found and got the default value.");
    } else {
      LOGGER.info("The jdbc password configuration (" + passwordKey + ") was found and got the value: " + Strings.createFromString("x").multiply(password.length()).toString());
    }

    String workingSchemaKey = prefix + "." + JdbcConnectionAttribute.SCHEMA_PATH.getKey();
    String workingSchema = config.getString(workingSchemaKey);
    if (workingSchema == null) {
      LOGGER.info("The jdbc workingSchema configuration (" + workingSchemaKey + ") was not found and got no value.");
    } else {
      LOGGER.info("The jdbc workingSchema configuration (" + workingSchemaKey + ") was found and got the value " + workingSchema);
    }

    String maxPoolSizeKey = prefix + "." + JdbcConnectionAttribute.POOL_SIZE.getKey();
    Integer maxPoolSize = config.getInteger(maxPoolSizeKey);
    if (maxPoolSize == null) {
      maxPoolSize = (Integer) JdbcConnectionAttribute.POOL_SIZE.getDefault();
      LOGGER.info("The jdbc max pool size configuration (" + maxPoolSizeKey + ") was not found and got the value:" + maxPoolSize);
    } else {
      LOGGER.info("The jdbc max pool size configuration (" + maxPoolSizeKey + ") was found and got the value:" + maxPoolSize);
    }

    JdbcConnectionInfo jdbcConnectionInfo = new JdbcConnectionInfo();
    jdbcConnectionInfo.url = url;
    jdbcConnectionInfo.databaseName = databaseName;
    jdbcConnectionInfo.user = user;
    jdbcConnectionInfo.password = password;
    jdbcConnectionInfo.maxPoolSize = maxPoolSize;
    jdbcConnectionInfo.schemaPath = workingSchema;

    return jdbcConnectionInfo;
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

  public String getSchemaPath() throws NullValueException {
    if (this.schemaPath == null) {
      throw new NullValueException();
    }
    return this.schemaPath;
  }

  /**
   * The default, working schema
   * Utility function to indicate what is the working schema
   * <p>
   * For PG, the current_schema returns the name of the schema that is at the front of the search path
   * <a href="https://www.postgresql.org/docs/current/ddl-schemas.html">...</a>
   *
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

  /**
   *
   * @return the connect timeout in ms
   */
  public int getConnectTimeout() {
    // the vertx default
    return 6000;
  }


  public String getDatabaseName() {
    return this.databaseName;
  }

}
