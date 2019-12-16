

package net.bytle.api.db;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

/**
 */

@ProxyGen
public interface DatabaseService {

  @Fluent
  DatabaseService getIp(String ip, Handler<AsyncResult<JsonObject>> resultHandler);

  @GenIgnore
  static DatabaseService create(JDBCClient dbClient, Handler<AsyncResult<DatabaseService>> readyHandler) {
    return new DatabaseServiceImpl(dbClient, readyHandler);
  }

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    //return new DatabaseServiceVertxEBProxy(vertx, address);
    return null;
  }

}
