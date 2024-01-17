package net.bytle.vertx.future;

import io.vertx.core.Future;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.util.Collection;

/**
 * Execute futures one after the other (sequentially)
 * @param <T>
 */
public class TowerFuturesSequentialScheduler<T> {

  /**
   * Execute them all one after the other, fail at the first failure
   */
  public Future<TowerFutureComposite<T>> all(Collection<Future<T>> futures, TowerCompositeFutureListener listener) {
    TowerFuturesSequentialComposite<T> towerComposite = new TowerFuturesSequentialComposite<>(futures, TowerFutureCoordination.ALL, listener);
    return Future.future(towerComposite);
  }

  public Future<TowerFutureComposite<T>> join(Collection<Future<T>> futures, TowerCompositeFutureListener listener) {
    TowerFuturesSequentialComposite<T> towerComposite = new TowerFuturesSequentialComposite<>(futures, TowerFutureCoordination.JOIN, listener);
    return Future.future(towerComposite);
  }
}
