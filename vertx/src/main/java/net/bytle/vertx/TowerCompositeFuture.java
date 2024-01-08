package net.bytle.vertx;

import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;

public class TowerCompositeFuture<T> {

  private final List<Future<T>> futures;
  private final List<T> results = new ArrayList<>();
  private Throwable failure;
  private final TowerCompositeFutureListener listener;
  private Integer failureIndex;


  public TowerCompositeFuture(List<Future<T>> futures, TowerCompositeFutureListener listener) {
    this.futures = futures;
    this.listener = listener;
    this.listener.setCountTotal(futures.size());
  }

  /**
   * @param futures - execute future sequentially (stop when one failed)
   * @return void when finished
   */
  public static <T> Future<TowerCompositeFuture<T>> allSequentially(List<Future<T>> futures, TowerCompositeFutureListener listener) {

    TowerCompositeFuture<T> towerCompositeFuture = new TowerCompositeFuture<T>(futures, listener);
    return towerCompositeFuture
      .executeSequentially();

  }

  private Future<TowerCompositeFuture<T>> executeSequentially() {
    return executeSequentiallyRecursively(0);
  }

  private Future<TowerCompositeFuture<T>> executeSequentiallyRecursively(int index) {


    if (index >= futures.size()) {
      return Future.succeededFuture(this);
    }

    return futures.get(index)
      .compose(
        res -> {
          T castResult;
          try {
            //noinspection unchecked
            castResult = (T) res;
          } catch (Exception e) {
            this.failure = e;
            this.failureIndex = index;
            return Future.succeededFuture(this);
          }
          if (this.listener != null) {
            this.listener.setCountComplete(index + 1);
          }
          this.results.add(index, castResult);
          return executeSequentiallyRecursively(index + 1);
        },
        err -> {
          this.failure = err;
          this.failureIndex = index;
          return Future.succeededFuture(this);
        }
      );
  }

  public List<T> getResults() {
    return this.results;
  }

  public boolean hasFailed() {
    return this.failure != null;
  }

  public Throwable getFailure() {
    return this.failure;
  }

  public Integer getFailureIndex() {
    return this.failureIndex;
  }

}
