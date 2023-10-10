package net.bytle.tower.util;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.Exceptions;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.model.openapi.ExitStatusResponse;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.type.MediaTypes;
import net.bytle.vertx.MailServiceSmtpProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static net.bytle.tower.util.ContextFailureLogger.CONTEXT_FAILURE_LOGGER;

/**
 * General Failure handler
 * <p>
 * Example of error from CSRF:
 * ```
 * ctx.fail(403, new IllegalArgumentException("Token has been used or is outdated"));
 * ```
 */
public class ContextFailureHandler implements Handler<RoutingContext> {


  public static final String ERROR_NAME = "error";
  private static final Map<Vertx, ContextFailureHandler> ErrorHandlersByVertx = new HashMap<>();


  public ContextFailureHandler(JsonObject config) {
    Boolean sendEmailOnErrorConfig = config.getBoolean(ERROR_EMAIL_CONF, null);
    if (sendEmailOnErrorConfig == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The sys error configuration (" + ERROR_EMAIL_CONF + ") is mandatory", ERROR_EMAIL_CONF, null);
    }
    this.setSendMailOnError(sendEmailOnErrorConfig);
  }

  public static ContextFailureHandler createOrGet(Vertx vertx, JsonObject config) {
    ContextFailureHandler contextFailureHandlerVertx = ErrorHandlersByVertx.get(vertx);
    if (contextFailureHandlerVertx != null) {
      return contextFailureHandlerVertx;
    }
    contextFailureHandlerVertx = new ContextFailureHandler(config);
    ErrorHandlersByVertx.put(vertx, contextFailureHandlerVertx);
    return contextFailureHandlerVertx;
  }


  public void handle(RoutingContext context) {

    Throwable thrown = context.failure();

    /**
     * Own failure mechanism
     */
    if (thrown instanceof ContextFailureException) {
      ContextFailureException contextFailureException = (ContextFailureException) thrown;
      ContextFailureData contextFailureData = contextFailureException.getFailureContext();
      this.sendResponse(context, contextFailureData);
      return;
    }

    /**
     * Other type of exception
     */
    this.sendResponse(context,
      ContextFailureData.create()
        .buildFromFailureContext(context)
        .setMimeToJson()
    );


  }

  private void sendResponse(RoutingContext context, ContextFailureData contextFailureData) {

    HttpServerResponse response = context.response();
    if (response.headWritten()) {
      // for whatever reason, we may get it multiple time
      // for an unknown throw error for instance ?
      return;
    }

    int statusCode = contextFailureData.getStatusCode();
    /**
     * Internal error or forbidden request ({@link io.vertx.ext.web.handler.CSRFHandler problem})
     */
    if (statusCode == HttpStatus.INTERNAL_ERROR || statusCode == HttpStatus.FORBIDDEN) {
      this.logUnExpectedError(context);
    }


    MediaTypes format = contextFailureData.getMime();
    switch (format) {
      case TEXT_HTML:
      default:
        String html = contextFailureData.toHtml(context);
        context
          .response()
          .setStatusCode(statusCode)
          .putHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.TEXT_HTML.toString())
          .send(html);
      case TEXT_JSON:
        context
          .response()
          .setStatusCode(statusCode);
        ExitStatusResponse exitStatusResponse = contextFailureData.toJsonObject();
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


  static final String ERROR_EMAIL_CONF = "sys.on.error.send.email";

  private Boolean sendEmailOnError;

  public ContextFailureHandler setSendMailOnError(boolean b) {
    this.sendEmailOnError = b;
    return this;
  }

  /**
   * Log an unexpected error and send it if configured
   *
   */
  protected void logUnExpectedError(RoutingContext context) {

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
    CONTEXT_FAILURE_LOGGER.error(stackTraceAsString);

    /**
     * Send the email
     */
    if (this.sendEmailOnError) {
      MailServiceSmtpProvider mailServiceSmtpProvider = MailServiceSmtpProvider.get(context.vertx());
      User sysUser = SysAdmin.ADMIN_USER;
      MailMessage mailMessage = mailServiceSmtpProvider
        .createVertxMailMessage()
        .setFrom(sysUser.getEmail())
        .setTo(sysUser.getEmail())
        .setSubject("Tower: An error has occurred. " + thrown.getMessage())
        .setText(stackTraceAsString);
      mailServiceSmtpProvider
        .getTransactionalMailClientForUser(sysUser.getEmail())
        .sendMail(mailMessage)
        .onFailure(t -> CONTEXT_FAILURE_LOGGER.error("Error while sending the error email. Message:" + t.getMessage(), t));
    }
  }

}
