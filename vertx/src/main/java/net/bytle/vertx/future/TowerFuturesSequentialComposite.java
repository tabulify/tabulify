package net.bytle.vertx.future;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bytle.exception.InternalException;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.util.*;

public class TowerFuturesSequentialComposite<T> implements TowerFutureComposite<T>, Handler<Promise<TowerFutureComposite<T>>> {

  private final Iterator<Future<T>> futureIterator;
  private final TowerFutureCoordination coordinatation;
  private Integer index;
  private final TowerCompositeFutureListener listener;
  private final List<T> results = new ArrayList<>();
  private Throwable failure;
  private Integer failureIndex;
  private Promise<TowerFutureComposite<T>> promise;


  public TowerFuturesSequentialComposite(Collection<Future<T>> futures, TowerFutureCoordination towerFutureCoordination, TowerCompositeFutureListener listener) {
    this.futureIterator = futures.iterator();
    this.listener = listener;
    this.index = -1;
    this.coordinatation = towerFutureCoordination;
  }


  void executeSequentially() {
    if (this.coordinatation != TowerFutureCoordination.ALL) {
      this.failure = new InternalException("The coordination (" + this.coordinatation + ") is not implemented");
      this.promise.complete(this);
      return;
    }
    Future<T> next;
    try {
      next = futureIterator.next();
      this.index++;
    } catch (NoSuchElementException e) {
      this.promise.complete(this);
      return;
    }
    next
      .onComplete(
        res -> {
          if (res.failed()) {
            this.failure = res.cause();
            this.failureIndex = index;
            promise.complete(this);
            return;
          }
          T castResult;
          try {
            //noinspection unchecked
            castResult = (T) res;
          } catch (Exception e) {
            this.failure = e;
            this.failureIndex = index;
            this.promise.complete(this);
            return;
          }
          if (this.listener != null) {
            this.listener.setCountComplete(this.listener.getCountComplete() + 1);
          }
          this.results.add(index, castResult);
          executeSequentially();
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

  @Override
  public void handle(Promise<TowerFutureComposite<T>> event) {
    this.promise = event;
    executeSequentially();
  }
}
