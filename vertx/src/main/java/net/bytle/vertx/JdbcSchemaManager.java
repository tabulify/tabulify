package net.bytle.vertx;

import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage, create and migrate schema
 */
public class JdbcSchemaManager {

  public static final String SCRIPT_MIGRATION_PREFIX = "v";
  /**
   * The separator between names
   * (not - because this is seen as the minus sign even without space)
   */
  public static final String COLUMN_PART_SEP = "_";
  /**
   * The schema for the realm tables
   * We don't use a method to get the qualified database object name
   * to allow code analysis.
   */
  public static final String CS_REALM_SCHEMA = "cs_realms";
  /**
   * The sysdate column that stores the creation time
   * (Even if the creation time is set via a database trigger,
   * the code analysis see that the column should be set.
   * We, therefore add it in the java code).
   */
  public static final String CREATION_TIME_COLUMN_SUFFIX = "creation_time";
  public static final String MODIFICATION_TIME_COLUMN_SUFFIX = "modification_time";
  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSchemaManager.class);
  public static final String VERSION_LOG_TABLE = "version_log";

  /**
   * The prefix is here to be able to make the difference between
   * system schema (such as pg_catalog, public, ...) and combo schema
   */
  private static final String SCHEMA_PREFIX = "cs_";
  private static JdbcSchemaManager jdbcSchemaManager;

  /**
   * Tabular does not support actually directly to wrap a SQL connection
   */
  private final JdbcConnectionInfo jdbcConnectionInfo;

  public JdbcSchemaManager(JdbcConnectionInfo jdbcConnectionInfo) {
    this.jdbcConnectionInfo = jdbcConnectionInfo;
  }

  public static JdbcSchemaManager create(JdbcConnectionInfo jdbcConnectionInfo) {

    jdbcSchemaManager = new JdbcSchemaManager(jdbcConnectionInfo);
    LOGGER.info("Schema Manager created");
    return jdbcSchemaManager;

  }

  public static JdbcSchemaManager get() {

    if (jdbcSchemaManager == null) {
      throw new InternalException("No Jdbc Schema manager found for this vertx");
    }
    return jdbcSchemaManager;
  }


  private FluentConfiguration getFlyWayCommonConf() {
    return Flyway
      .configure()
      .sqlMigrationPrefix(SCRIPT_MIGRATION_PREFIX)
      .cleanDisabled(true)
      .table(VERSION_LOG_TABLE)
      .createSchemas(true)
      .dataSource(
        this.jdbcConnectionInfo.getUrl(),
        this.jdbcConnectionInfo.getUser(),
        this.jdbcConnectionInfo.getPassword()
      );
  }

  /**
   * Migrate the ip schema
   */
  public JdbcSchemaManager migrate(JdbcSchema jdbcSchema) throws DbMigrationException {

    Flyway flywayIp = this.getFlyWayCommonConf()
      .locations(jdbcSchema.getLocation())
      .schemas(jdbcSchema.getSchema())
      .load();
    this.migrateAndClose(flywayIp);

    return this;
  }


  /**
   * With flyway, you can create a flyway object,
   * and you run it.
   * This function runs the object and close the connection
   *
   * @param flyway the flyway object to run
   * @throws DbMigrationException if any error occurs
   */
  private void migrateAndClose(Flyway flyway) throws DbMigrationException {

    /**
     * It seems that we don't need to close the connection as Flyway do it
     * We can always get a connection via the getDataSource of the configuration {@link Flyway#getConfiguration()}
     */
    try {
      flyway.migrate();
    } catch (FlywayException e) {
      String schemas = String.join(",", flyway.getConfiguration().getSchemas());
      throw new DbMigrationException("Flyway Database migration error for the schema " + schemas, e);
    }


  }


  public static String getSchemaFromHandle(String handle) {
    return SCHEMA_PREFIX + handle;
  }


  public JdbcConnectionInfo getConnectionInfo() {
    return this.jdbcConnectionInfo;
  }

}
