package net.bytle.vertx.future;

import io.vertx.core.Future;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.util.Collection;

public class TowerFuturesSequentialScheduler<T> {

  /**
   * Execute them all, fail at the first failure
   */
  public Future<TowerFutureComposite<T>> all(Collection<Future<T>> futures, TowerCompositeFutureListener listener) {
    TowerFuturesSequentialComposite<T> towerComposite = new TowerFuturesSequentialComposite<>(futures, TowerFutureCoordination.ALL, listener);
    return Future.future(towerComposite);
  }

}
