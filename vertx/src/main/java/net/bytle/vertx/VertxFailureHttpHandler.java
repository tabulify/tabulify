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
public class VertxFailureHttpHandler implements Handler<RoutingContext> {


  public static final String ERROR_NAME = "error";
  private final VertxFailureHandler failureHandler;


  public VertxFailureHttpHandler(Server server) throws IllegalConfiguration {
      this.failureHandler = server.getFailureHandler();

  }

  public static VertxFailureHttpHandler createOrGet(Server server) throws IllegalConfiguration {


    return new VertxFailureHttpHandler(server);

  }


  public void handle(RoutingContext context) {

    Throwable thrown = context.failure();

    /**
     * Own failure mechanism
     */
    if (thrown instanceof VertxFailureHttpException) {
      VertxFailureHttpException contextFailureException = (VertxFailureHttpException) thrown;
      this.sendResponse(context, contextFailureException);
      return;
    }

    /**
     * Other type of exception
     */
    this.sendResponse(context,
      VertxFailureHttpException.builder()
        .setPropertiesFromFailureContext(context)
        .setMimeToJson()
        .build()
    );


  }

  private void sendResponse(RoutingContext context, VertxFailureHttpException vertxFailureHttpException) {

    HttpServerResponse response = context.response();
    if (response.headWritten()) {
      // for whatever reason, we may get it multiple time
      // for an unknown throw error for instance ?
      return;
    }

    HttpStatus statusCode = vertxFailureHttpException.getStatus();
    /**
     * Internal error or forbidden request ({@link io.vertx.ext.web.handler.CSRFHandler problem})
     */
    if (statusCode == HttpStatusEnum.INTERNAL_ERROR_500 || statusCode == HttpStatusEnum.FORBIDDEN_403) {
      this.logUnExpectedFailure(context);
    }


    MediaTypes format = vertxFailureHttpException.getMime();
    switch (format) {
      case TEXT_HTML:
      default:
        String html = vertxFailureHttpException.toHtml(context);
        context
          .response()
          .setStatusCode(statusCode.getStatusCode())
          .putHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.TEXT_HTML.toString())
          .send(html);
      case TEXT_JSON:
        context
          .response()
          .setStatusCode(statusCode.getStatusCode());
        ExitStatusResponse exitStatusResponse = vertxFailureHttpException.toJsonObject();
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
