package net.bytle.tower.eraldy.app;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.InternalException;

/**
 * This path will throw
 * It's used only in test
 */
public class ErrorFakeHandler implements Handler<RoutingContext> {

  public static final String URI_PATH = "/fail";
  public static final String FAILURE_MESSAGE = "An unexpected error that should be logged";


  @Override
  public void handle(RoutingContext event) {
    throw new InternalException(FAILURE_MESSAGE);
  }

}
