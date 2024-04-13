package net.bytle.vertx;

import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Target;

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
  private static final Logger LOGGER = LogManager.getLogger(JdbcSchemaManager.class);
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
  private final JdbcClient jdbcClient;

  public JdbcSchemaManager(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public static JdbcSchemaManager create(JdbcClient jdbcClient) {

    jdbcSchemaManager = new JdbcSchemaManager(jdbcClient);
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

    int OneSec = 1000;
    return Flyway
      .configure()
      .sqlMigrationPrefix(SCRIPT_MIGRATION_PREFIX)
      .cleanDisabled(true)
      .table(VERSION_LOG_TABLE)
      .createSchemas(true)
      .connectRetries(2)
      .connectRetriesInterval(OneSec)
      .dataSource(this.jdbcClient.getDataSource());
  }

  /**
   * Migrate the ip schema
   */
  public JdbcSchemaManager migrate(JdbcSchema jdbcSchema) throws DbMigrationException {

    Flyway flyway = this.getFlyWayCommonConf()
      .locations(jdbcSchema.getLocation())
      .schemas(jdbcSchema.getSchema())
      .load();

    /**
     * It seems that we don't need to close the connection as Flyway do it
     * We can always get a connection via the getDataSource of the configuration {@link Flyway#getConfiguration()}
     */
    MigrateResult migrateResult;
    try {
      migrateResult = flyway.migrate();
    } catch (FlywayException e) {
      String schemas = String.join(",", flyway.getConfiguration().getSchemas());
      throw new DbMigrationException("Flyway Database migration error for the schema " + schemas, e);
    }

    String targetSchemaVersion = migrateResult.targetSchemaVersion;
    String initialSchemaVersion = migrateResult.initialSchemaVersion;
    if (!initialSchemaVersion.equals(targetSchemaVersion)) {
      LOGGER.info("Schema migrated from " + initialSchemaVersion + "+to " + targetSchemaVersion);
      if (JavaEnvs.IS_DEV) {
        LOGGER.info("Applying JOOQ generation");
        Configuration configuration = new org.jooq.meta.jaxb.Configuration()
          .withJdbc(new org.jooq.meta.jaxb.Jdbc()
            .withDriver("org.postgresql.Driver")
            .withUrl("jdbc:postgresql://localhost:5432/your_database")
            .withUser("your_username")
            .withPassword("your_password"))
          .withGenerator(new org.jooq.meta.jaxb.Generator()
            .withDatabase(new Database()
              .withName("org.jooq.meta.postgres.PostgresDatabase")
              .withInputSchema("your_schema_name")) // Specify the input schema
            .withTarget(new Target()
              .withPackageName("your.package.name"))); // Specify the package name for generated classes

        try {
          GenerationTool.generate(configuration);
        } catch (Exception e) {
          throw new DbMigrationException("Jooq Generation Failed for the schema" + jdbcSchema.getSchema(), e);
        }
      }
    }


    return this;
  }


  public static String getSchemaFromHandle(String handle) {
    return SCHEMA_PREFIX + handle;
  }

}
