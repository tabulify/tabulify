package net.bytle.ip;

import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.db.Tabular;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.exception.DbMigrationException;
import net.bytle.ip.api.IpApiImpl;
import net.bytle.ip.handler.IpHandler;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.ApiTokenAuthenticationProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.sql.Types;

public class IpApp extends TowerApp {

  static Logger LOGGER = LogManager.getLogger(IpApp.class);

  public static final String CS_IP_SCHEMA = JdbcSchemaManager.getSchemaFromHandle("ip");

  public IpApp(TowerApexDomain towerApexDomain) {
    super(towerApexDomain);
  }

  public static TowerApp createForDomain(EraldyDomain eraldyDomain) {
    return new IpApp(eraldyDomain);
  }

  public static void loadIpDataIfNeeded(JdbcConnectionInfo jdbcConnectionInfo) throws DbMigrationException {
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
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path csvPath = tempDir.resolve("IpToCountry.csv");
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

  @Override
  public String getAppName() {
    return "ip";
  }

  @Override
  public TowerApp openApiMount(RouterBuilder builder) {
    new IpHandler(new IpApiImpl()).mount(builder);
    return this;
  }

  public void migrateDatabaseSchema() throws DbMigrationException {
    JdbcSchema ipSchema = JdbcSchema.builder()
      .setLocation("classpath:db/cs-ip")
      .setSchema(CS_IP_SCHEMA)
      .build();
    JdbcSchemaManager jdbcManager = getApexDomain().getHttpServer().getServer().getJdbcManager();
    jdbcManager.migrate(ipSchema);
    if (!JavaEnvs.IS_DEV) {
      /**
       * Take 10 seconds to load the tabular env ...
       * We load in test and prod only
       */
      loadIpDataIfNeeded(jdbcManager.getConnectionInfo());
    }
  }

  @Override
  public TowerApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor configAccessor) {

    ApiTokenAuthenticationProvider apiTokenAuthenticationProvider = new ApiTokenAuthenticationProvider(configAccessor);
    builder
      .securityHandler(ApiTokenAuthenticationProvider.BEARER_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> APIKeyHandler.create(apiTokenAuthenticationProvider));

    return this;
  }

  @Override
  protected String getPublicSubdomainName() {
    return "api";
  }

  @Override
  protected Future<Void> mountOnThirdServices() {
    try {
      this.migrateDatabaseSchema();
      return Future.succeededFuture();
    } catch (DbMigrationException e) {
      RuntimeException failedMigration = new RuntimeException("Failed Migration for the app (" + this + ")", e);
      return Future.failedFuture(failedMigration);
    }
  }

  @Override
  protected TowerApp addSpecificAppHandlers(Router router) {
    return this;
  }

  @Override
  public boolean hasOpenApiSpec() {
    return true;
  }

  @Override
  public String getPublicDefaultOperationPath() {
    return "/ip";
  }

  @Override
  protected String getPublicAbsolutePathMount() {
    return "/ip";
  }

  @Override
  public boolean getIsHtmlApp() {
    return false;
  }

  @Override
  public boolean isSocial() {
    return false;
  }

}
