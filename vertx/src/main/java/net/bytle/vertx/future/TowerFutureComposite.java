package net.bytle.vertx.future;

import java.util.List;

public interface TowerFutureComposite<T> {
  List<T> getResults();

  boolean hasFailed();

  Throwable getFailure();

  Integer getFailureIndex();
}
