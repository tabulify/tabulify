package net.bytle.vertx;

import io.vertx.core.Future;

public abstract class TowerService implements AutoCloseable, TowerServiceInterface {


  @Override
  public void close() throws Exception {

  }

  @Override
  public Future<Void> mount() throws Exception {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> start() throws Exception {
    return Future.succeededFuture();
  }

}
