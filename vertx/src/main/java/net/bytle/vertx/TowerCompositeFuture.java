package net.bytle.vertx;

import io.vertx.core.Future;

import java.util.List;

public class TowerCompositeFuture {

  /**
   *
   * @param futures - execute future sequentially
   * @return void when finished
   */
  public Future<Void> executeSequentially(List<Future<?>> futures) {

    return executeSequentiallyRecursively(futures, 0);

  }

  private Future<Void> executeSequentiallyRecursively(List<Future<?>> futures, int index) {

    if (index >= futures.size()) {
      return Future.succeededFuture();
    }

    return futures.get(index)
      .compose(
        listImportJobRowStatus -> executeSequentiallyRecursively(futures, index + 1),
        Future::failedFuture
      );
  }

}
