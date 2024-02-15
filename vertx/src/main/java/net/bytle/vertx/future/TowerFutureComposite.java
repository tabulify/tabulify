package net.bytle.vertx.future;

import java.util.List;

public interface TowerFutureComposite<T> {

  /**
   * @return the results (null if the results has failed)
   */
  List<T> getResults();

  /**
   * The whole execution may have failed
   * (Too much error, ...)
   */
  boolean hasFailed();

  /**
   * @return the failure cause of the execution
   */
  Throwable getFailureCause();

  /**
   *
   * @return the size of the number of handlers executed
   * It should be equal to the list passed
   */
  int size();

  /**
   * @param i - the index on the list of the handler
   * @return if the handler has failed
   */
  boolean failed(int i);

  /**
   * @param i - the index of the handler
   * @return - the cause or null if the handler has not failed
   */
  Throwable cause(int i);

  /**
   * @param i - the index of the handler
   * @return - the result of null if the handler has failed
   */
  T resultAt(int i);
}
