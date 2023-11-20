package net.bytle.vertx;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.InternalException;
import net.bytle.type.MediaTypes;

import java.util.NoSuchElementException;

/**
 * General Routing Failure handler
 * <p>
 * Example of error from CSRF:
 * ```
 * ctx.fail(403, new IllegalArgumentException("Token has been used or is outdated"));
 * ```
 */
public class TowerFailureHttpHandler implements Handler<RoutingContext> {


  public static final String ERROR_NAME = "error";
  private final VertxFailureHandler failureHandler;


  public TowerFailureHttpHandler(Server server) throws IllegalConfiguration {
    this.failureHandler = server.getFailureHandler();

  }

  public static TowerFailureHttpHandler createOrGet(Server server) throws IllegalConfiguration {


    return new TowerFailureHttpHandler(server);

  }


  public void handle(RoutingContext context) {

    Throwable thrown = context.failure();

    /**
     * Own failure mechanism
     */
    if (thrown instanceof TowerFailureException) {
      TowerFailureException contextFailureException = (TowerFailureException) thrown;
      this.sendResponse(context, contextFailureException);
      return;
    }

    /**
     * Other type of exception
     */
    this.sendResponse(context,
      TowerFailureException.builder()
        .setPropertiesFromFailureContext(context)
        .setMimeToJson()
        .build()
    );


  }

  private void sendResponse(RoutingContext context, TowerFailureException towerFailureException) {

    HttpServerResponse response = context.response();
    if (response.headWritten()) {
      // for whatever reason, we may get it multiple time
      // for an unknown throw error for instance ?
      return;
    }

    TowerFailureStatus statusCode = towerFailureException.getStatus();

    /**
     * Internal error
     * <p>
     * We don't log forbidden request (ie {@link TowerFailureStatusEnum.NOT_AUTHORIZED_403})
     * Note that a {@link io.vertx.ext.web.handler.CSRFHandler problem} will log a 403 ...
     */
    if (statusCode == TowerFailureStatusEnum.INTERNAL_ERROR_500) {
      this.logUnExpectedFailure(context);
    }


    MediaTypes format = towerFailureException.getMime();
    switch (format) {
      case TEXT_HTML:
      default:
        String html = towerFailureException.toHtml(context);
        context
          .response()
          .setStatusCode(statusCode.getStatusCode())
          .putHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.TEXT_HTML.toString())
          .send(html);
      case TEXT_JSON:
        context
          .response()
          .setStatusCode(statusCode.getStatusCode());
        ExitStatusResponse exitStatusResponse = towerFailureException.toJsonObject();
        context.json(exitStatusResponse);
    }
  }


  public static void failRoutingContextWithTrace(Throwable throwable, RoutingContext routingContext, String message) {

    Throwable l;
    if (throwable instanceof NoSuchElementException) {
      l = throwable;
    } else {
      if (message != null) {
        l = new InternalException(message, throwable);
      } else {
        l = new InternalException(throwable);
      }
    }
    routingContext.fail(l);
  }


  /**
   * Log an unexpected error and send it if configured
   */
  protected void logUnExpectedFailure(RoutingContext context) {

    Throwable thrown = context.failure();
    this.failureHandler.handle(thrown);

  }

}
