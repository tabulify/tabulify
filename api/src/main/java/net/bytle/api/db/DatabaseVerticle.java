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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <a href="https://vertx.io/docs/vertx-jdbc-client/java/">Doc</a>
 */
public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class);


  // The key of the properties
  public static final String KEY_JDBC_URL = "jdbc.url";
  public static final String KEY_JDBC_DRIVER = "jdbc.driver_class";
  public static final String KEY_JDBC_MAX_POOL_SIZE = "jdbc.max_pool_size";

  // The name of the queue
  public static final String IP_QUEUE_NAME = "ip.queue";

  @Override
  public void start(Promise<Void> promise) {

    // "jdbc:sqlite:./db.db"
    String url = config().getString(KEY_JDBC_URL, "jdbc:hsqldb:file:db/wiki");
    String jdbcDriver = config().getString(KEY_JDBC_DRIVER, "org.hsqldb.jdbcDriver");
    int jdbcPoolSize = config().getInteger(KEY_JDBC_MAX_POOL_SIZE, 3);


    // CreateShared creates a pool connection shared among Verticles known to the vertx instance
    JsonObject config = new JsonObject()
      .put("url", url)
      .put("driver_class", jdbcDriver)
      .put("max_pool_size", jdbcPoolSize);
    JDBCClient dbClient = JDBCClient.createShared(vertx, config);

    // Register the service
    DatabaseServiceInterface.create(dbClient, config, ready -> {
      if (ready.succeeded()) {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder
          .setAddress(IP_QUEUE_NAME)
          .register(DatabaseServiceInterface.class, ready.result());
        promise.complete();
      } else {
        promise.fail(ready.cause());
      }
    });


  }


}
