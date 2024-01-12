package net.bytle.vertx;

import io.vertx.core.Future;

import java.util.*;

public class TowerFutureSequentialComposite<T> {

  private final Iterator<Future<T>> futureIterator;
  private Integer index;
  private final TowerCompositeFutureListener listener;
  private final List<T> results = new ArrayList<>();
  private Throwable failure;
  private Integer failureIndex;


  public TowerFutureSequentialComposite(Collection<Future<T>> futureCollection, TowerCompositeFutureListener listener) {
    this.futureIterator = futureCollection.iterator();
    this.listener = listener;
    this.index = -1;
  }


  Future<TowerFutureSequentialComposite<T>> executeSequentially() {

    Future<T> next;
    try {
      next = futureIterator.next();
      this.index++;
    } catch (NoSuchElementException e) {
      return Future.succeededFuture(this);
    }

    return next
      .compose(
        res -> {
          T castResult;
          try {
            //noinspection RedundantCast
            castResult = (T) res;
          } catch (Exception e) {
            this.failure = e;
            this.failureIndex = index;
            return Future.succeededFuture(this);
          }
          if (this.listener != null) {
            this.listener.setCountComplete(this.listener.getCountComplete() + 1);
          }
          this.results.add(index, castResult);
          return executeSequentially();
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
