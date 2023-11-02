package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.db.Tabular;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.exception.DbMigrationException;
import net.bytle.type.Ip;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.sql.Types;

public class IpGeolocation {

  public static final String CS_IP_SCHEMA = JdbcSchemaManager.getSchemaFromHandle("ip");
  static Logger LOGGER = LogManager.getLogger(IpGeolocation.class);
  private final PgPool pgPool;
  private final JdbcSchemaManager jdbcManager;

  public IpGeolocation(PgPool pgPool, JdbcSchemaManager jdbcSchemaManager) {
    this.pgPool = pgPool;
    this.jdbcManager = jdbcSchemaManager;
  }

  public static IpGeolocation create(PgPool builder,JdbcSchemaManager jdbcSchemaManager) throws DbMigrationException {
      return new IpGeolocation(builder, jdbcSchemaManager)
        .init();
  }

  private IpGeolocation init() throws DbMigrationException {
    JdbcSchema ipSchema = JdbcSchema.builder()
      .setLocation("classpath:db/cs-ip")
      .setSchema(CS_IP_SCHEMA)
      .build();
    this.jdbcManager.migrate(ipSchema);
    loadIpDataIfNeeded(jdbcManager.getConnectionInfo());
    return this;
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

  public Future<IpInfo> ipGet(String ip) {

    final String ipv4;
    if (ip.equals("0:0:0:0:0:0:0:1")) {
      ipv4 = ip = "127.0.0.1";
    } else {
      ipv4 = ip;
    }
    Long numericIp = Ip.ipv4ToLong(ip);
    LOGGER.info("numericIp is {}", numericIp);
    // One shot, no need to close anything and return only one row
    // https://vertx.io/docs/apidocs/io/vertx/ext/sql/SQLOperations.html#querySingleWithParams-java.lang.String-io.vertx.core.json.JsonArray-io.vertx.core.Handler-
    String sql = "SELECT * FROM " + IpGeolocation.CS_IP_SCHEMA + ".ip " +
      "WHERE " +
      "ip_from <= $1 " +
      "and ip_to >= $2";
    return this.pgPool.preparedQuery(sql)
      .execute(Tuple.of(numericIp, numericIp))
      .onFailure(e -> LOGGER.error("Database query error with Sql:\n" + sql, e))
      .compose(rows -> {
        LOGGER.info("Fetch succeeded for IP {}", numericIp);
        IpInfo ipResponse = new IpInfo();
        if (rows.size() != 0) {

          Row row = rows.iterator().next();
          LOGGER.info("Query fetched {}", row);
          ipResponse.setCountry2(row.getString(4));
          ipResponse.setCountry3(row.getString(5));
          ipResponse.setCountry(row.getString(6));
          ipResponse.setIp(ipv4);

        }

        return Future.succeededFuture(ipResponse);
      });


  }
}
