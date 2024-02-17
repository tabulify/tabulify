package net.bytle.tower.eraldy.schedule;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.java.Javas;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.JacksonMapperManager;
import net.bytle.vertx.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class SqlAnalytics implements Handler<Long> {

  private static final Logger LOGGER = LogManager.getLogger(SqlAnalytics.class);
  private static final String SQL_ANALYTICS_DIR_RESOURCES_PATH = "/analytics";
  private final EraldyApiApp apiApp;
  private final Path analyticsDirectory;

  private LocalDateTime executionLastTime = LocalDateTime.now();
  private boolean isRunning = false;
  private List<Path> executedPaths = new ArrayList<>();

  /**
   * Run the analytics regularly
   */
  public SqlAnalytics(EraldyApiApp apiApp) throws ConfigIllegalException {

    this.apiApp = apiApp;
    Server server = apiApp.getHttpServer().getServer();

    /**
     * Analytics Directory
     * where we found the SQL to run
     */
    if (JavaEnvs.IS_DEV) {
      analyticsDirectory = Paths.get("src/main/resources" + SQL_ANALYTICS_DIR_RESOURCES_PATH);
    } else {
      URL analyticsDirectoryUrl = SqlAnalytics.class.getResource(SQL_ANALYTICS_DIR_RESOURCES_PATH);
      if (analyticsDirectoryUrl == null) {
        throw new ConfigIllegalException("The Analytics Resource directory was not found.");
      }
      analyticsDirectory = Javas.getFilePathFromUrl(analyticsDirectoryUrl);
    }
    if (!Files.exists(analyticsDirectory)) {
      throw new ConfigIllegalException("The Analytics directory (" + analyticsDirectory + ") does not exist");
    }
    if (!Files.isDirectory(analyticsDirectory)) {
      throw new ConfigIllegalException("The Analytics path (" + analyticsDirectory + ") is not a directory");
    }
    LOGGER.info("The analytics path was set to: " + analyticsDirectory);

    /**
     * The job
     */
    long delayMsEveryHour = 1000 * 60 * 60;
    long startMs = 1000 * 60 * 60;
    if (JavaEnvs.IS_DEV) {
      startMs = 5 * 1000;
    }
    server.getVertx().setPeriodic(startMs, delayMsEveryHour, this);

    /**
     * The health of the job
     */
    server.getServerHealthCheck()
      .register(SqlAnalytics.class, promise -> {

        /**
         * Test
         */
        Duration agoLastExecution = Duration.between(this.executionLastTime, LocalDateTime.now());
        boolean executionTest = agoLastExecution.toMillis() <= delayMsEveryHour + 1000;

        /**
         * Data
         * Bug? even if we have added the LocalDateTime Jackson Time module, we get an error
         * {@link JacksonMapperManager}
         * We do it manually then
         */
        String executionsLastTimeString = DateTimeUtil.LocalDateTimetoString(this.executionLastTime);
        JsonObject data = new JsonObject();
        data.put("execution-last-time", executionsLastTimeString);
        data.put("execution-last-ago-sec", agoLastExecution.toSeconds());
        data.put("execution-paths", executedPaths.stream().map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")));

        /**
         * Checks and Status Report
         */
        // ok
        if (executionTest) {
          promise.complete(Status.OK(data));
          return;
        }
        // not ok
        Status status = Status.KO();
        data.put("message", "The last time execution date is too old.");
        status.setData(data);
        promise.complete(status);
      });

  }


  @Override
  public void handle(Long event) {

    synchronized (this) {
      if (this.isRunning) {
        return;
      }
      this.isRunning = true;
    }

    PgPool jdbcPool = this.apiApp.getHttpServer().getServer().getPostgresDatabaseConnectionPool();

    List<Future<RowSet<Row>>> futures = new ArrayList<>();
    this.executedPaths = new ArrayList<>();
    List<String> sqls = new ArrayList<>();

    try (DirectoryStream<Path> files = Files.newDirectoryStream(this.analyticsDirectory)) {
      for (Path path : files) {
        this.executionLastTime = LocalDateTime.now();
        String sql = Fs.getFileContent(path);
        futures.add(jdbcPool.preparedQuery(sql).execute());
        executedPaths.add(path);
        sqls.add(sql);
      }
    } catch (IOException e) {
      LOGGER.error("Error while reading the analytics directory", e);
      return;
    }
    Future
      .join(futures)
      .onComplete(async -> {
        this.isRunning = false;
        if (async.failed()) {
          LOGGER.error("Error while running the analytics directory", async.cause());
        }
        CompositeFuture composite = async.result();
        for(int i=0;i<composite.size();i++){
          if (composite.failed(i)) {
            Throwable err = composite.causes().get(i);
            LOGGER.error("The SQL analytic (" + executedPaths.get(i) + ") with the SQL: " + sqls.get(i), composite.cause(i), err);
          }
        }
      });
  }

}
