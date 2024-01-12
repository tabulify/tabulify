package net.bytle.vertx.future;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bytle.exception.InternalException;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class TowerFuturesRateLimitedComposite<T> implements TowerFutureComposite<T>, Handler<Promise<TowerFutureComposite<T>>> {
  private final Iterator<Future<T>> futureIterator;
  private final TowerFuturesRateLimitedScheduler<T> rateLimitedMeta;
  private final TowerFutureCoordination coordinationType;

  /**
   * The id starting at 0 of the element in the collection
   */
  private int elementId;

  private final List<T> results = new ArrayList<>();
  private Throwable failure;
  private Integer failureIndex;
  private LocalDateTime actualPeriodEndTime;
  private int actualPeriodExecutedAmount;
  private Promise<TowerFutureComposite<T>> actualPeriodPromise;

  private final TowerCompositeFutureListener listener;

  public TowerFuturesRateLimitedComposite(
    TowerFuturesRateLimitedScheduler<T> rateLimitedMeta,
    Collection<Future<T>> futures,
    TowerFutureCoordination coordinationType,
    TowerCompositeFutureListener listener
  ) {
    this.futureIterator = futures.iterator();
    this.rateLimitedMeta = rateLimitedMeta;
    this.elementId = -1;
    this.actualPeriodEndTime = LocalDateTime.now().plus(rateLimitedMeta.getRatePeriodeDuration());
    this.actualPeriodExecutedAmount = 0;
    this.listener = listener;
    this.coordinationType = coordinationType;

  }

  @Override
  public void handle(Promise<TowerFutureComposite<T>> event) {
    this.actualPeriodPromise = event;
  }

  private void handleRecursively() {

    List<Future<T>> next = new ArrayList<>();
    try {
      for (int i = 0; i < this.rateLimitedMeta.getBatchSize(); i++) {
        next.add(futureIterator.next());
        this.actualPeriodExecutedAmount++;
      }
    } catch (NoSuchElementException e) {
      //
    }
    if (next.isEmpty()) {
      this.actualPeriodPromise.complete(this);
      return;
    }
    if (LocalDateTime.now().isAfter(this.actualPeriodEndTime)) {
      this.actualPeriodExecutedAmount = 0;
      this.actualPeriodEndTime = LocalDateTime.now().plus(this.rateLimitedMeta.getRatePeriodeDuration());
    }
    if (this.actualPeriodExecutedAmount > this.rateLimitedMeta.getRateCount()) {
      long delay = Duration.between(LocalDateTime.now(), this.actualPeriodEndTime).toMillis();
      this.rateLimitedMeta.getServer().getVertx().setTimer(delay, (id) -> executeBatch(next));
      return;
    }
    executeBatch(next);

  }

  private void executeBatch(List<Future<T>> next) {
    if (this.coordinationType != TowerFutureCoordination.ALL) {
      this.failure = new InternalException("The future coordination (" + coordinationType + ") is not yet implemented.");
      this.actualPeriodPromise.complete(this);
      return;
    }
    Future.all(next)
      .onComplete(
        asyncResult -> {
          if (asyncResult.failed()) {
            this.failure = asyncResult.cause();
            for (int i = 0; i < this.rateLimitedMeta.getBatchSize(); i++) {
              this.elementId++;
              if (asyncResult.result().failed(i)) {
                break;
              }
            }
            this.failureIndex = this.elementId;
            this.actualPeriodPromise.complete(this);
            return;
          }
          for (Object element : asyncResult.result().list()) {
            this.elementId++;
            T castResult;
            try {
              //noinspection unchecked
              castResult = (T) element;
            } catch (Exception e) {
              this.failure = e;
              this.failureIndex = elementId;
              this.actualPeriodPromise.complete(this);
              return;
            }
            if (this.listener != null) {
              this.listener.setCountComplete(this.listener.getCountComplete() + 1);
            }
            this.results.add(elementId, castResult);
          }
          handleRecursively();
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
