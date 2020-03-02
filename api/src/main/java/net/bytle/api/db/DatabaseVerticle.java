/*
 *  Copyright (c) 2017 Red Hat, Inc. and/or its affiliates.
 *  Copyright (c) 2017 INSA Lyon, CITI Laboratory.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bytle.api.db;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;

/**
 * <a href="https://vertx.io/docs/vertx-jdbc-client/java/">Doc</a>
 */
public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class);

  // Default config
  public static final String JDBC_DRIVER_DEFAULT = "org.hsqldb.jdbcDriver";
  public static final String JDBC_URL_DEFAULT = "jdbc:hsqldb:file:db/api";
  public static final int JDBC_POOL_SIZE_DEFAULT = 30;

  // The client
  private JDBCClient dbClient;

  @Override
  public void stop() {
    dbClient.close();
  }

  // The key of the properties
  public static final String KEY_JDBC_URL = "jdbc.url";
  public static final String KEY_JDBC_DRIVER = "jdbc.driver_class";
  public static final String KEY_JDBC_MAX_POOL_SIZE = "jdbc.max_pool_size";

  // The name of the queue
  public static final String IP_QUEUE_NAME = "ip.queue";

  @Override
  public void start(Promise<Void> promise) {


    String url = config().getString(KEY_JDBC_URL, JDBC_URL_DEFAULT);
    String jdbcDriver = config().getString(KEY_JDBC_DRIVER, JDBC_DRIVER_DEFAULT);
    int jdbcPoolSize = config().getInteger(KEY_JDBC_MAX_POOL_SIZE, JDBC_POOL_SIZE_DEFAULT);


    // CreateShared creates a pool connection shared among Verticles known to the vertx instance
    JsonObject config = new JsonObject()
      .put("url", url)
      .put("driver_class", jdbcDriver)
      .put("max_pool_size", jdbcPoolSize);

    // Execute the blocking database  migration code
    vertx.<Boolean>executeBlocking(future -> {

      // Migrate if not done
      // https://flywaydb.org/documentation/api/
      try {
        Flyway flyway = Flyway.configure().dataSource(url, null, null).load();
        flyway.migrate();
      } catch (FlywayException e) {
        LOGGER.error("Flyway Database preparation error {}", e.getMessage());
        future.fail(e);
      }

      // Load meta
      String dataStoreName = "ip";
      Tabular tabular = Tabular.tabular();
      DataPath ipTable = tabular
        .getDataStore(dataStoreName)
        .setConnectionString(url)
        .getDefaultDataPath("ip");
      if (Tabulars.getSize(ipTable) == 0) {
        Path csvPath = Paths.get("./IpToCountry.csv");
        if (!Files.exists(csvPath)) {
          try {
            // Download the zip locally
            URL zipFile = new URL("https://gerardnico.com/datafile/IpToCountry.zip");
            Path source = Paths.get(zipFile.toURI());
            Path zipTemp = Files.createTempFile("IpToCountry", ".zip");
            Files.copy(source, zipTemp, StandardCopyOption.REPLACE_EXISTING);

            // Extract the csv with a zipfs file system
            FileSystem zipFs = FileSystems.newFileSystem(zipTemp, null);
            Path zipPath = zipFs.getPath("IpToCountry.csv");
            Files.copy(zipPath, csvPath);

          } catch (URISyntaxException | IOException e) {
            future.fail(e);
          }
        }
        try {
          DataPath csvDataPath = tabular.getDataPath(csvPath);
          Tabulars.copy(csvDataPath, ipTable);
        } catch (Exception e) {
          LOGGER.error("Csv Loading error {}", e.getCause().getMessage());
          future.fail(e);
        }

      }
      future.complete(true);
      tabular.close();
    }, res -> {

      if (res.succeeded()) {

        // Register the service
        LOGGER.info("Register the service with the address {}", IP_QUEUE_NAME);
        dbClient = JDBCClient.createShared(vertx, config);
        LOGGER.info("Instantiate the implementation class with the creation method of the interface");
        DatabaseServiceInterface.create(dbClient, asyncResult -> {
          if (asyncResult.succeeded()) {
            // ready result is the implementation (ie DatabaseServiceInterfaceImpl instance)
            ServiceBinder binder = new ServiceBinder(vertx).setAddress(IP_QUEUE_NAME);
            binder.register(DatabaseServiceInterface.class, asyncResult.result());
            LOGGER.info("Database Verticle complete");
            promise.complete();
          } else {
            promise.fail(asyncResult.cause());
          }
        });

      } else {

        promise.fail(res.cause());

      }
    });


  }


}
