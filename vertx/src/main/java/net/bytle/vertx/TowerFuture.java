package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.List;

public class TowerFuture {

  /**
   * @param futures - execute future sequentially (stop when one failed)
   * @return void when finished
   */
  public static <T> Future<TowerFutureSequentialComposite<T>> allSequentially(List<Future<T>> futures, TowerCompositeFutureListener listener) {

    TowerFutureSequentialComposite<T> towerFutureSequentialComposite = new TowerFutureSequentialComposite<>(futures, listener);
    return towerFutureSequentialComposite
      .executeSequentially();

  }

  /**
   * @param futures - execute future sequentially (stop when one failed)
   * @return void when finished
   */
  public static <T> Future<TowerFutureRateLimitedComposite<T>> allRateLimited(List<Future<T>> futures, TowerCompositeFutureListener listener, Vertx vertx) {

    TowerFutureRateLimitedComposite<T> towerFutureSequentialComposite = new TowerFutureRateLimitedComposite<>(futures, listener, vertx);
    return towerFutureSequentialComposite
      .execute();

  }

}
