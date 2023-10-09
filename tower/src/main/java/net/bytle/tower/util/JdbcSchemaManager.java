package net.bytle.tower.util;

import io.vertx.core.Vertx;
import net.bytle.db.Tabular;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import static net.bytle.tower.eraldy.app.combopublicapi.implementer.IpPublicapiImpl.CS_IP_SCHEMA;

/**
 * Manage, create and migrate schema for combo
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
  public static final String REALM_ID_COLUMN = RealmProvider.TABLE_PREFIX + COLUMN_PART_SEP + RealmProvider.ID;
  public static final String MODIFICATION_TIME_COLUMN_SUFFIX = "modification_time";
  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSchemaManager.class);
  public static final String VERSION_LOG_TABLE = "version_log";

  /**
   * The prefix is here to be able to make the difference between
   * system schema (such as pg_catalog, public, ...) and combo schema
   */
  public static final String SCHEMA_PREFIX = "cs_";


  private final DataSource dataSource;

  /**
   * Tabular does not support actually directly to wrap a SQL connection
   */
  private JdbcConnectionInfo jdbcConnectionInfo;

  public JdbcSchemaManager(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  static private final Map<Vertx, JdbcSchemaManager> jdbcSchemaManagerMap = new HashMap<>();

  public static JdbcSchemaManager create(Vertx vertx, DataSource dataSource) {

    JdbcSchemaManager jdbcMigration = new JdbcSchemaManager(dataSource);
    jdbcSchemaManagerMap.put(vertx, jdbcMigration);
    return jdbcMigration;

  }

  public static JdbcSchemaManager get(Vertx vertx) {
    JdbcSchemaManager jdbcSchemaManager = jdbcSchemaManagerMap.get(vertx);
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
      .dataSource(dataSource);
  }

  /**
   * Migrate the ip schema
   */
  public JdbcSchemaManager migrateComboIp() throws DbMigrationException {

    Flyway flywayIp = this.getFlyWayCommonConf()
      .locations("classpath:db/cs-ip")
      .schemas(CS_IP_SCHEMA)
      .load();
    this.migrateAndClose(flywayIp);

    if (!Env.IS_DEV) {
      /**
       * Take 10 seconds to load the tabular env ...
       */
      loadIpDataIfNeeded();
    }

    return this;
  }

  public void loadIpDataIfNeeded() throws DbMigrationException {
    // Load meta
    LOGGER.info("Loading Ip data");
    String dataStoreName = "ip";
    // tabular needs a secret when a password is given because it may store them
    // we don't store any password
    try (Tabular tabular = Tabular.tabular("secret")) {
      LOGGER.info("Ip Table count");
      DataPath ipTable = tabular
        .createRuntimeConnection(dataStoreName, jdbcConnectionInfo.getUrl())
        .setUser(jdbcConnectionInfo.getUser())
        .setPassword(jdbcConnectionInfo.getPassword())
        .getDataPath(CS_IP_SCHEMA + ".ip");
      Long count = ipTable.getCount();
      LOGGER.info("Total Ip Table count " + count);
      if (count == 0) {
        LOGGER.info("Loading Ip Table");
        Path csvPath = Paths.get("./IpToCountry.csv");
        if (!Files.exists(csvPath)) {
          try {
            // Download the zip locally
            URL zipFile = new URL("https://datacadamia.com/datafile/IpToCountry.zip");
            Path source = Paths.get(zipFile.toURI());
            Path zipTemp = Files.createTempFile("IpToCountry", ".zip");
            Files.copy(source, zipTemp, StandardCopyOption.REPLACE_EXISTING);

            // Extract the csv with a zipfs file system
            try (FileSystem zipFs = FileSystems.newFileSystem(zipTemp, null)) {
              Path zipPath = zipFs.getPath("IpToCountry.csv");
              Files.copy(zipPath, csvPath);
            }

          } catch (URISyntaxException | IOException e) {
            throw new DbMigrationException("Error with zip ip download", e);
          }
        }
        try {
          CsvDataPath csvDataPath = (CsvDataPath) CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), csvPath)
            .setQuoteCharacter('"')
            .setHeaderRowId(0)
            .createRelationDef()
            .addColumn("ip_from", Types.BIGINT)
            .addColumn("ip_to", Types.BIGINT)
            .addColumn("registry", Types.VARCHAR, 255)
            .addColumn("assigned", Types.BIGINT)
            .addColumn("ctry", Types.VARCHAR, 2)
            .addColumn("cntry", Types.VARCHAR, 3)
            .addColumn("country", Types.VARCHAR, 255)
            .getDataPath();
          Tabulars.copy(csvDataPath, ipTable);
        } catch (Exception e) {

          String errorMessage = e.getMessage();
          Throwable cause = e.getCause();
          String causeMessage = "Null";
          if (cause != null) {
            causeMessage = cause.getMessage();
          }
          LOGGER.error(" Error : {}, Cause: {}", errorMessage, causeMessage);
          throw new DbMigrationException("CsvLoading Error", cause);

        }
      }
    }
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


  /**
   * Migrate the app by schema
   */
  public JdbcSchemaManager migrateComboRealms() throws DbMigrationException {
    String schema = JdbcSchemaManager.getSchemaFromHandle("realms");
    Flyway flywayTenant = this.getFlyWayCommonConf()
      .locations("classpath:db/cs-realms")
      .schemas(schema)
      .load();
    this.migrateAndClose(flywayTenant);
    return this;
  }

  @SuppressWarnings("SameParameterValue")
  private static String getSchemaFromHandle(String handle) {
    return SCHEMA_PREFIX + handle;
  }


  public JdbcSchemaManager setConnectionInfo(JdbcConnectionInfo jdbcConnectionInfo) {
    this.jdbcConnectionInfo = jdbcConnectionInfo;
    return this;
  }


}
