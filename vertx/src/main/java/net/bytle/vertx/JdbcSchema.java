package net.bytle.vertx;

import io.vertx.core.Future;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Jdbc Schema
 * The JDBC schema handle the schema name,
 * and the migration (on mount)
 * It's why it's a service.
 */
public class JdbcSchema extends TowerService {

  private static final Logger LOGGER = LogManager.getLogger(JdbcSchema.class);

  private final Builder builder;


  public JdbcSchema(Builder builder) {
      super(builder.jdbcClient.getServer());
      this.builder = builder;
  }

  public static Builder builder(JdbcClient jdbcClient, String schemaHandle) {
    return new Builder(jdbcClient,schemaHandle);
  }

  public JdbcClient getJdbcClient() {
    return builder.jdbcClient;
  }

  public String getLocation() {
    return this.builder.location;
  }

  public String getSchemaName() {
    return this.builder.schemaName;
  }

  /**
   * When we will generate our own Table Class
   * (Was used with Jooq)
   */
  @SuppressWarnings("unused")
  public String getTargetJavaPackageName() {
    return this.builder.targetPackage;
  }

  @Override
  public Future<Void> mount() {

    LOGGER.info(this.builder.schemaName+ " schema Migration");
    JdbcSchemaManager jdbcSchemaManager = this.builder.jdbcClient.getSchemaManager();
    try {
      jdbcSchemaManager.migrate(this);
    } catch (DbMigrationException e) {
      return Future.failedFuture(new InternalException("The database migration failed for the schema (" + this + ")", e));
    }
    return super.mount();

  }


  public static class Builder {

    private final String schemaHandle;
    private final JdbcClient jdbcClient;
    private String location;
    private String targetPackage;
    private String schemaPrefixName = "cs";
    private String schemaName;

    /**
     * @param jdbcClient - the client (ie database and migration)
     * @param schemaHandle - the handle without any prefix
     */
    public Builder(JdbcClient jdbcClient, String schemaHandle) {

      this.jdbcClient = jdbcClient;
      this.schemaHandle = schemaHandle;

    }

    /**
     *
     * @param location - the location of the migration file
     */
    public Builder setMigrationFileLocation(String location) {
      this.location = location;
      return this;
    }

    /**
     * The prefix is here to be able to make the difference between
     * system schema (such as pg_catalog, public, ...) and combo schema
     */
    @SuppressWarnings("unused")
    public Builder setSchemaPrefix(String prefix) {
      this.schemaPrefixName = prefix;
      return this;
    }

    public JdbcSchema build() {
      if (this.location == null) {
        setMigrationFileLocation("classpath:db/" + schemaPrefixName + "-" + schemaHandle);
      }
      if (schemaName == null) {
        setSchemaName(schemaPrefixName + "_" + schemaHandle);
      }
      return new JdbcSchema(this);
    }

    /**
     * @param schemaName - the database schema name
     */
    public Builder setSchemaName(String schemaName) {
      this.schemaName = schemaName;
      return this;
    }

    /**
     * @param targetPackage - the target package for the generated schema class
     */
    public Builder setJavaPackageForClassGeneration(String targetPackage) {
      this.targetPackage = targetPackage;
      return this;
    }

  }

  @Override
  public String toString() {
    return this.builder.schemaName;
  }
}
