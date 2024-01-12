package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TowerFutureRateLimitedComposite<T> {


  private final Iterator<Future<T>> futureIterator;
  private final TowerCompositeFutureListener listener;

  private final int batchSize;
  private final Duration rateDuration;
  private final int rateCount;
  private final Vertx vertx;
  /**
   * The id starting at 0 of the element in the collection
   */
  private int elementId;

  private final List<T> results = new ArrayList<>();
  private Throwable failure;
  private Integer failureIndex;
  private LocalDateTime actualPeriodEndTime;
  private int actualPeriodExecutedAmount;
  private Promise<TowerFutureRateLimitedComposite<T>> actualPeriodPromise;

  public TowerFutureRateLimitedComposite(Collection<Future<T>> futureCollection, TowerCompositeFutureListener listener, Vertx vertx) {
    this.futureIterator = futureCollection.iterator();
    this.listener = listener;
    this.elementId = -1;
    this.rateCount = 150;
    int ratePeriodAmount = 5;
    TimeUnit ratePeriodUnit = TimeUnit.SECONDS;
    this.rateDuration = Duration.of(ratePeriodAmount, ratePeriodUnit.toChronoUnit());
    this.batchSize = 5;
    this.vertx = vertx;
  }

  Future<TowerFutureRateLimitedComposite<T>> execute() {
    return Future.future(this::handler);
  }

  private void handler(Promise<TowerFutureRateLimitedComposite<T>> promise) {
    this.actualPeriodEndTime = LocalDateTime.now().plus(rateDuration);
    this.actualPeriodExecutedAmount = 0;
    this.actualPeriodPromise = promise;
    handleRecursively();
  }

  private void handleRecursively() {

    List<Future<T>> next = new ArrayList<>();
    try {
      for (int i = 0; i < this.batchSize; i++) {
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
      this.actualPeriodEndTime = LocalDateTime.now().plus(rateDuration);
    }
    if (this.actualPeriodExecutedAmount > this.rateCount) {
      long delay = Duration.between(LocalDateTime.now(), this.actualPeriodEndTime).toMillis();
      vertx.setTimer(delay, (id) -> executeBatch(next));
      return;
    }
    executeBatch(next);

  }

  private void executeBatch(List<Future<T>> next) {
    Future.all(next)
      .onComplete(
        asyncResult -> {
          if (asyncResult.failed()) {
            this.failure = asyncResult.cause();
            for (int i = 0; i < this.batchSize; i++) {
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
