package net.bytle.vertx.future;

import io.vertx.core.*;
import net.bytle.exception.InternalException;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TowerFuturesSequentialComposite<T> implements TowerFutureComposite<T>, Handler<Promise<TowerFutureComposite<T>>> {


  private final TowerFutureCoordination coordination;
  private final int maxFatalErrorCount;
  private final Iterator<Handler<Promise<T>>> handlers;
  private final int batchSize;
  private final WorkerExecutor workerExecutor;
  private final TowerCompositeFutureListener listener;
  private final List<T> results = new ArrayList<>();
  private Throwable failureCause;
  private Promise<TowerFutureComposite<T>> promise;
  private int failureCounter = 0;
  private final List<Throwable> failures = new ArrayList<>();


  TowerFuturesSequentialComposite(TowerFuturesSequentialScheduler.Config<T> config) {
    this.handlers = config.handlers.iterator();
    this.listener = config.listener;
    this.coordination = config.coordination;
    this.maxFatalErrorCount = config.maxFatalErrorCount;
    this.batchSize = config.batchSize;
    this.workerExecutor = config.workerExecutor;
  }


  /**
   * This method executes a batch and call itself until all handlers
   * have been executed
   */
  void handleRecursively() {

    if (!(this.coordination == TowerFutureCoordination.ALL || this.coordination == TowerFutureCoordination.JOIN)) {
      this.failureCause = new InternalException("The coordination (" + this.coordination + ") is not implemented");
      this.promise.complete(this);
      return;
    }

    List<Future<T>> nextBatch = new ArrayList<>();
    for (int i = 0; i < this.batchSize; i++) {
      Handler<Promise<T>> handler;
      try {
        handler = handlers.next();
      } catch (NoSuchElementException e) {
        break;
      }
      Promise<T> promise = Promise.promise();
      if (this.workerExecutor != null) {
        /**
         * In the context of a worker
         */
        this.workerExecutor.executeBlocking(() -> {
          try {
            handler.handle(promise);
          } catch (Throwable e) {
            promise.tryFail(e);
          }
          return null;
        });
      } else {
        /**
         * In the context of the verticle
         */
        try {
          handler.handle(promise);
        } catch (Throwable e) {
          promise.tryFail(e);
        }
      }
      nextBatch.add(promise.future());

    }
    if (nextBatch.isEmpty()) {
      this.promise.complete(this);
      return;
    }

    // Await completion
    Future
      .all(nextBatch)
      .onComplete(
        res -> {
          if (res.failed()) {
            this.failureCause = res.cause();
            if (this.coordination == TowerFutureCoordination.ALL) {
              promise.complete(this);
              return;
            }
          }
          CompositeFuture compositeFuture = res.result();
          int actualBatchSizeExecution = compositeFuture.size();
          for(int i = 0; i< actualBatchSizeExecution; i++){
            if( compositeFuture.failed()){
              /**
               * Bad execution
               */
              this.results.add(null);
              this.failures.add(compositeFuture.cause(i));
              this.failureCounter++;
              if (this.failureCounter >= this.maxFatalErrorCount) {
                this.failureCause = new InternalException("Too much fatal errors (" + this.failureCounter + ") on execution", this.failureCause);
                promise.complete(this);
                return;
              }
              continue;
            }
            /**
             * Good execution
             */
            this.results.add(compositeFuture.resultAt(i));
            this.failures.add(null);
          }
          if (this.listener != null) {
            this.listener.setCountComplete(this.listener.getCountComplete() + actualBatchSizeExecution);
          }
          handleRecursively();
        }
      );

  }

  public List<T> getResults() {
    return this.results;
  }

  public boolean hasFailed() {
    return this.failureCause != null;
  }

  public Throwable getFailureCause() {
    return this.failureCause;
  }

  @Override
  public int size() {
    return this.results.size();
  }

  @Override
  public boolean failed(int i) {
    return this.failures.get(i)!=null;
  }

  @Override
  public Throwable cause(int i) {
    return this.failures.get(i);
  }

  @Override
  public T resultAt(int i) {
    return this.results.get(i);
  }


  @Override
  public void handle(Promise<TowerFutureComposite<T>> event) {
    this.promise = event;
    handleRecursively();
  }

}
