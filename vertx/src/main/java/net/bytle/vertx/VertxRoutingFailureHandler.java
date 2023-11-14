package net.bytle.vertx;

import io.micrometer.core.instrument.Counter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.Exceptions;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.InternalException;
import net.bytle.type.MediaTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * General Routing Failure handler
 * <p>
 * Example of error from CSRF:
 * ```
 * ctx.fail(403, new IllegalArgumentException("Token has been used or is outdated"));
 * ```
 */
public class VertxRoutingFailureHandler implements Handler<RoutingContext> {


  public static final String ERROR_NAME = "error";
  private static final Map<Vertx, VertxRoutingFailureHandler> ErrorHandlersByVertx = new HashMap<>();
  private final Counter failureCounter;


  public VertxRoutingFailureHandler(ConfigAccessor config) throws IllegalConfiguration {
    Boolean sendEmailOnErrorConfig = config.getBoolean(SYS_ERROR_EMAIL_CONF, false);
    this.setSendMailOnError(sendEmailOnErrorConfig);
    failureCounter = VertxPrometheusMetrics
      .getRegistry()
      .counter("router_failure");
  }

  public static VertxRoutingFailureHandler createOrGet(Vertx vertx, ConfigAccessor config) throws IllegalConfiguration {
    VertxRoutingFailureHandler vertxRoutingFailureHandlerVertx = ErrorHandlersByVertx.get(vertx);
    if (vertxRoutingFailureHandlerVertx != null) {
      return vertxRoutingFailureHandlerVertx;
    }
    vertxRoutingFailureHandlerVertx = new VertxRoutingFailureHandler(config);
    ErrorHandlersByVertx.put(vertx, vertxRoutingFailureHandlerVertx);
    return vertxRoutingFailureHandlerVertx;
  }


  public void handle(RoutingContext context) {

    Throwable thrown = context.failure();

    /**
     * Own failure mechanism
     */
    if (thrown instanceof VertxRoutingFailureException) {
      VertxRoutingFailureException contextFailureException = (VertxRoutingFailureException) thrown;
      VertxRoutingFailureData vertxRoutingFailureData = contextFailureException.getFailureContext();
      this.sendResponse(context, vertxRoutingFailureData);
      return;
    }

    /**
     * Other type of exception
     */
    this.sendResponse(context,
      VertxRoutingFailureData.create()
        .buildFromFailureContext(context)
        .setMimeToJson()
    );


  }

  private void sendResponse(RoutingContext context, VertxRoutingFailureData vertxRoutingFailureData) {

    HttpServerResponse response = context.response();
    if (response.headWritten()) {
      // for whatever reason, we may get it multiple time
      // for an unknown throw error for instance ?
      return;
    }

    HttpStatus statusCode = vertxRoutingFailureData.getStatus();
    /**
     * Internal error or forbidden request ({@link io.vertx.ext.web.handler.CSRFHandler problem})
     */
    if (statusCode == HttpStatusEnum.INTERNAL_ERROR_500 || statusCode == HttpStatusEnum.FORBIDDEN_403) {
      this.logUnExpectedError(context);
    }


    MediaTypes format = vertxRoutingFailureData.getMime();
    switch (format) {
      case TEXT_HTML:
      default:
        String html = vertxRoutingFailureData.toHtml(context);
        context
          .response()
          .setStatusCode(statusCode.getStatusCode())
          .putHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.TEXT_HTML.toString())
          .send(html);
      case TEXT_JSON:
        context
          .response()
          .setStatusCode(statusCode.getStatusCode());
        ExitStatusResponse exitStatusResponse = vertxRoutingFailureData.toJsonObject();
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


  static final String SYS_ERROR_EMAIL_CONF = "sys.on.error.send.email";

  private Boolean sendEmailOnError;

  public VertxRoutingFailureHandler setSendMailOnError(boolean b) {
    this.sendEmailOnError = b;
    return this;
  }

  /**
   * Log an unexpected error and send it if configured
   *
   */
  protected void logUnExpectedError(RoutingContext context) {

    this.failureCounter.increment();

    /**
     * <p>
     * For info, note that Log4j may also send email
     * <a href="https://logging.apache.org/log4j/log4j-2.7/manual/appenders.html#SMTPAppender">...</a>
     */

    Throwable thrown = context.failure();
    /**
     * Log
     */
    String stackTraceAsString = Exceptions.getStackTraceAsString(thrown);
    ContextFailureLogger.CONTEXT_FAILURE_LOGGER.error(stackTraceAsString);

    /**
     * Send the email
     */
    if (this.sendEmailOnError) {
      MailServiceSmtpProvider mailServiceSmtpProvider = MailServiceSmtpProvider.get(context.vertx());

      MailMessage mailMessage = mailServiceSmtpProvider
        .createVertxMailMessage()
        .setFrom(SysAdmin.getEmail())
        .setTo(SysAdmin.getEmail())
        .setSubject("Tower: An error has occurred. " + thrown.getMessage())
        .setText(stackTraceAsString);
      mailServiceSmtpProvider
        .getVertxMailClientForSenderWithSigning(SysAdmin.getEmail())
        .sendMail(mailMessage)
        .onFailure(t -> ContextFailureLogger.CONTEXT_FAILURE_LOGGER.error("Error while sending the error email. Message:" + t.getMessage(), t));
    }
  }

}
