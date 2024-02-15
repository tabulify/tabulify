package net.bytle.vertx.future;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import net.bytle.vertx.TowerCompositeFutureListener;

import java.util.List;

/**
 * Execute futures one after the other (sequentially)
 */
public class TowerFuturesSequentialScheduler {



  public static class Config<T> {


    TowerCompositeFutureListener listener;
    TowerFutureCoordination coordination;
    int maxFatalErrorCount = Integer.MAX_VALUE;
    List<Handler<Promise<T>>> handlers;
    int batchSize = 1;
    WorkerExecutor workerExecutor;

    public Config<T> setListener(net.bytle.vertx.TowerCompositeFutureListener listener) {
      this.listener = listener;
      return this;
    }

    @SuppressWarnings("unused")
    public Config<T> setMaxErrorCount(int maxFatalErrorCount) {
      this.maxFatalErrorCount = maxFatalErrorCount;
      return this;
    }

    /**
     * Execute them all one after the other,
     * Return at first failure
     * fail if one failure
     */
    public Future<TowerFutureComposite<T>> all(List<Handler<Promise<T>>> handlers) {
      this.coordination = TowerFutureCoordination.ALL;
      this.maxFatalErrorCount = 1;
      this.handlers= handlers;
      return Future.future(new TowerFuturesSequentialComposite<>(this));
    }

    /**
     * Execute them all (up to maxFatalFailureCount if defined),
     * Return when they are all executed,
     * Fail if one failure
     */
    public Future<TowerFutureComposite<T>> join(List<Handler<Promise<T>>> handlers) {
      this.coordination = TowerFutureCoordination.JOIN;
      this.handlers= handlers;
      return Future.future(new TowerFuturesSequentialComposite<>(this));
    }

    public Config<T> setBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Config<T> setExecutorContext(WorkerExecutor workerExecutor) {
      this.workerExecutor = workerExecutor;
      return this;
    }
  }
}
