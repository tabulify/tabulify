

package net.bytle.api.db;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

/**
 */

@ProxyGen
@VertxGen
public interface DatabaseServiceInterface {

  @Fluent
  DatabaseServiceInterface getIp(String ip, Handler<AsyncResult<JsonObject>> resultHandler);

  @GenIgnore
  static DatabaseServiceInterface create(JDBCClient dbClient, Handler<AsyncResult<DatabaseServiceInterface>> readyHandler) {
    return new DatabaseServiceInterfaceImpl(dbClient, readyHandler);
  }

  @GenIgnore
  static DatabaseServiceInterface createProxy(Vertx vertx, String address) {
    return new DatabaseServiceInterfaceVertxEBProxy(vertx, address);
  }

}
