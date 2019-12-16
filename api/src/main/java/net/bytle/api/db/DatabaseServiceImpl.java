package net.bytle.api.db;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class DatabaseServiceImpl implements DatabaseService {

  public DatabaseServiceImpl(JDBCClient dbClient, Handler<AsyncResult<DatabaseService>> readyHandler) {
  }

  @Override
  public DatabaseService getIp(String ip, Handler<AsyncResult<JsonObject>> resultHandler) {
    return null;
  }

}
