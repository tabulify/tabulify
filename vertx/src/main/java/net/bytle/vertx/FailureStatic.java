package net.bytle.vertx;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.InternalException;

public class FailureStatic {
  /**
   * If we throw the error directly, the error is
   * seen as 'unhandled error'.
   * This is a utility function that wraps the error
   * to get a trace in our code and return a failed future
   */
  public static void failFutureWithTrace(Throwable t) {
    InternalException error = new InternalException(t);
    error.printStackTrace();
  }

  /**
   * When the handler has a non-recoverable problem,
   * the best is to throw an exception that is caught {@link VertxRoutingFailureHandler}
   * <p>
   * This approach works well when it's your code but
   * when it's a library code, we lose the position in the stack trace
   * (as this is vertx that's calling)
   * <p>
   * This method will add an exception to see where the exception
   * has occurred in our code.
   * Throwing a vertx error directly will not add any trace to the stack trace,
   * and we will not know where the error originates from.
   *
   * <p>
   * Example:
   * if the error is reported only with `onFailure(routingContext::fail)`, we get
   * <code>
   * org.postgresql.util.PSQLException: The column index is out of range: 1, number of columns: 0.
   * at org.postgresql.core.v3.SimpleParameterList.bind(SimpleParameterList.java:69)
   * at org.postgresql.core.v3.SimpleParameterList.setStringParameter(SimpleParameterList.java:132)
   * at org.postgresql.jdbc.PgPreparedStatement.bindString(PgPreparedStatement.java:1055)
   * at org.postgresql.jdbc.PgPreparedStatement.setString(PgPreparedStatement.java:356)
   * at org.postgresql.jdbc.PgPreparedStatement.setString(PgPreparedStatement.java:342)
   * at org.postgresql.jdbc.PgPreparedStatement.setObject(PgPreparedStatement.java:945)
   * at io.vertx.jdbcclient.impl.actions.JDBCPreparedQuery.fillStatement(JDBCPreparedQuery.java:134)
   * at io.vertx.jdbcclient.impl.actions.JDBCPreparedQuery.execute(JDBCPreparedQuery.java:72)
   * at io.vertx.jdbcclient.impl.actions.JDBCPreparedQuery.execute(JDBCPreparedQuery.java:39)
   * at io.vertx.ext.jdbc.impl.JDBCConnectionImpl.lambda$schedule$3(JDBCConnectionImpl.java:219)
   * </code>
   *
   * @param throwable      - the error to log
   * @param routingContext - the routing context
   */
  public static void failRoutingContextWithTrace(Throwable throwable, RoutingContext routingContext) {
    VertxRoutingFailureHandler.failRoutingContextWithTrace(throwable, routingContext, null);
  }
}
