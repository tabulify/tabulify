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
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <a href="https://vertx.io/docs/vertx-jdbc-client/java/">Doc</a>
 */
public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class);

  private JDBCClient dbClient;

  public static final String JDBC_URL = "jdbc.url";
  public static final String JDBC_DRIVER = "jdbc.driver_class";
  public static final String JDBC_MAX_POOL_SIZE = "jdbc.max_pool_size";
  public static final String EVENT_BUS_QUEUE_NAME = "ip.queue";

  @Override
  public void start(Promise<Void> promise) throws Exception {

    // "jdbc:sqlite:./db.db"
    String url = config().getString(JDBC_URL, "jdbc:hsqldb:file:db/wiki");
    String jdbcDriver = config().getString(JDBC_DRIVER, "org.hsqldb.jdbcDriver");
    int jdbcPoolSize = config().getInteger(JDBC_MAX_POOL_SIZE, 3);
    String eventBusQueueName = config().getString(EVENT_BUS_QUEUE_NAME, "ip.queue");

    // Migrate if not done
    // https://flywaydb.org/documentation/api/
    Flyway flyway = Flyway.configure().dataSource(url,null,null).load();
    flyway.migrate();

    // CreateShared creates a pool connection shared among Verticles known to the vertx instance
    dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", url)
      .put("driver_class", jdbcDriver)
      .put("max_pool_size", jdbcPoolSize)
    );

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        promise.fail(ar.cause());
      } else {
        vertx.eventBus().consumer(eventBusQueueName, this::onMessage);
        LOGGER.info("Queue created "+eventBusQueueName, ar.cause());
        promise.complete();
      }
    });

  }

  public enum ErrorCodes {
    NO_ACTION_SPECIFIED,
    BAD_ACTION,
    DB_ERROR
  }

  /**
   * The message handler from the event bus
   * @param message
   */
  public void onMessage(Message<JsonObject> message) {

    if (!message.headers().contains("action")) {
      LOGGER.error("No action header specified for message with headers {} and body {}",
        message.headers(), message.body().encodePrettily());
      message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
      return;
    }
    String action = message.headers().get("action");

    switch (action) {
      case "get-ip":
        fetchIp(message);
        break;
      default:
        message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
    }
  }

  private void fetchIp(Message<JsonObject> message) {
    String ip = message.body().getString("ip");
    JsonArray params = new JsonArray()
      .add(ip)
      .add(ip);
    // One shot, no need to close anything and return only one row
    // https://vertx.io/docs/apidocs/io/vertx/ext/sql/SQLOperations.html#querySingleWithParams-java.lang.String-io.vertx.core.json.JsonArray-io.vertx.core.Handler-
    dbClient.querySingleWithParams("SELECT * FROM ip WHERE ip_from <= ? and ip_to >= ?", params, fetch -> {
      if (fetch.succeeded()) {
        JsonArray row = fetch.result();
        JsonObject response = new JsonObject();
        if (row == null) {
          response.put("found", false);
        } else {
          response.put("found", true);
          response.put("country2",row.getString(4));
          response.put("country3",row.getString(5));
        }
        message.reply(response);
      } else {
        reportQueryError(message, fetch.cause());
      }
    });
  }



  private void reportQueryError(Message<JsonObject> message, Throwable cause) {
    LOGGER.error("Database query error", cause);
    message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }

}
