package net.bytle.vertx.future;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.util.Collection;

/**
 * Execute futures one after the other (sequentially)
 */
public class TowerFuturesSequentialScheduler<T extends Handler<Promise<T>>> {

  private final Server server;

  public TowerFuturesSequentialScheduler(Server server) {
    this.server = server;
  }

  /**
   * Execute them all one after the other,
   * Return at first failure
   * fail at the first failure
   */
  public  Future<TowerFutureComposite<T>> all(Collection<Handler<Promise<T>>> handlers, TowerCompositeFutureListener listener) {
    TowerFuturesSequentialComposite<T> towerComposite = new TowerFuturesSequentialComposite<>(server, handlers, TowerFutureCoordination.ALL, listener, 1);
    return Future.future(towerComposite);
  }

  /**
   * Execute them all until maxFatalFailureCount,
   * Return when they are all executed,
   * Fail at the first failure
   */
  public Future<TowerFutureComposite<T>> join(Collection<Handler<Promise<T>>> futures, TowerCompositeFutureListener listener, int maxFatalFailureCount) {
    TowerFuturesSequentialComposite<T> towerComposite = new TowerFuturesSequentialComposite<>(server, futures, TowerFutureCoordination.JOIN, listener, maxFatalFailureCount);
    return Future.future(towerComposite);
  }
}
