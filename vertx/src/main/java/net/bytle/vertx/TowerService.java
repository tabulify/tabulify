package net.bytle.vertx;

import io.vertx.core.Future;

public abstract class TowerService implements TowerServiceInterface {

  private final Server server;

  public TowerService(Server server) {
    this.server = server;
    server.registerService(this);
  }

  @Override
  public void close() throws Exception {

  }

  @Override
  public Future<Void> mount() {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> start() {
    return Future.succeededFuture();
  }

  @Override
  public Server getServer(){
    return this.server;
  }

}
