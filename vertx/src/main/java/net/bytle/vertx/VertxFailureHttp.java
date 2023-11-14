package net.bytle.vertx;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.Exceptions;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NotLoggedInException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.MediaTypes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A failure utility data class to:
 * * create the failure data, and fail the context with {@link VertxFailureHttp#failContextAsHtml(RoutingContext)} that send it to {@link VertxFailureHttpHandler}
 * * be used in {@link VertxFailureHttpHandler} to create an adequate response
 * <p>
 */
public class VertxFailureHttp {


  private String description;
  private String name;
  private HttpStatus status = HttpStatusEnum.INTERNAL_ERROR_500;


  private MediaTypes mime = MediaTypes.TEXT_HTML;
  /**
   * The exception that has occurred
   */
  private Exception exception;

  public VertxFailureHttp() {

  }


  public static VertxFailureHttp create() {
    return new VertxFailureHttp();
  }


  public VertxFailureHttp setDescription(String message) {
    this.description = message;
    return this;
  }

  public VertxFailureHttp setStatus(HttpStatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Failing the context returning an HTML page
   * <p>
   * Note: it will send the {@link VertxFailureHttpException}
   * to the {@link VertxFailureHttpHandler}
   */
  public void failContextAsHtml(RoutingContext routingContext) {

    this.mime = MediaTypes.TEXT_HTML;
    this.failContext(routingContext);

  }

  public VertxFailureHttp failContext(RoutingContext routingContext) {

    routingContext.fail(this.status.getStatusCode(), (new VertxFailureHttpException(this)));
    return this;

  }

  /**
   * @param name - the name of the error (used as title in a HTML page)
   */
  public VertxFailureHttp setName(String name) {
    this.name = name;
    return this;
  }


  public HttpStatus getStatus() {
    return this.status;
  }

  /**
   * @param context - the context in the {@link VertxFailureHttpHandler}
   */
  private VertxFailureHttp setStatusCodeFromFailureContext(RoutingContext context) {
    int httpStatusCode = context.statusCode();
    if (status.getStatusCode() == -1) {
      status = HttpStatusEnum.INTERNAL_ERROR_500;
    } else {
      try {
        status = HttpStatusEnum.fromHttpStatusCode(httpStatusCode);
      } catch (NotFoundException e) {
        status = () -> httpStatusCode;
      }
    }
    return this;
  }

  /**
   * Set the status code if the status code is the internal error (the default)
   *
   * @param code - the code
   */
  private VertxFailureHttp setStatusCodeOnlyIfValueIsInternalError(HttpStatus code) {
    if (status == HttpStatusEnum.INTERNAL_ERROR_500) {
      this.status = code;
    }
    return this;
  }

  public VertxFailureHttp buildFromFailureContext(RoutingContext context) {

    this.setStatusCodeFromFailureContext(context);

    Throwable thrown = context.failure();
    /**
     * Failure may be not given
     */
    if (thrown == null) {
      return this;
    }

    String message = thrown.getMessage();
    String stackTraceAsString = Exceptions.getStackTraceAsString(thrown);
    if (JavaEnvs.IS_DEV) {
      message += "\n" + stackTraceAsString;
    }


    /**
     * BodyProcessorException may wrap a validation exception
     */
    if (thrown instanceof BodyProcessorException) {
      if (thrown.getCause() instanceof ValidationException) {
        thrown = thrown.getCause();
      }
    }

    if (thrown instanceof ValidationException) {
      this.setStatusCodeOnlyIfValueIsInternalError(HttpStatusEnum.BAD_REQUEST_400);
      String keyWord = ((ValidationException) thrown).keyword();
      message += " (Keyword: " + keyWord + ", ";

      String input = "unknown";
      Object inputObject = ((ValidationException) thrown).input();
      if (inputObject != null) {
        // for what ever reason it may be null
        input = inputObject.toString();
      }
      message += "Input: " + input + ", ";

      JsonPointer inputScopeJson = ((ValidationException) thrown).inputScope();
      String inputScope = "unknown";
      if (inputScopeJson != null) {
        // for what ever reason it may be null
        inputScope = inputScopeJson.toString();
      }
      message += "InputScope: " + inputScope + ")";
    } else if (thrown instanceof NotFoundException) {
      this.setStatusCodeOnlyIfValueIsInternalError(HttpStatusEnum.BAD_REQUEST_400);
    } else if (thrown instanceof ParameterProcessorException) {
      this.setStatusCodeOnlyIfValueIsInternalError(HttpStatusEnum.BAD_REQUEST_400);
    } else if (thrown instanceof NotLoggedInException) {
      if (!context.request().path().contains("api")) {
        String redirectEndpoint = "/login";
        context.response()
          .putHeader(io.vertx.core.http.HttpHeaders.LOCATION, redirectEndpoint)
          .setStatusCode(HttpResponseStatus.FOUND.code())
          .end();
      } else {
        context.response()
          .setStatusCode(HttpResponseStatus.FORBIDDEN.code())
          .end();
      }
      return this;
    } else if (thrown instanceof HttpException) {
      int statusCode = ((HttpException) thrown).getStatusCode();
      try {
        this.setStatusCodeOnlyIfValueIsInternalError(HttpStatusEnum.fromHttpStatusCode(statusCode));
      } catch (NotFoundException e) {
        this.setStatusCodeOnlyIfValueIsInternalError(()->statusCode);
      }
    } else if (thrown instanceof IllegalArgumentException) {
      this.setStatusCodeOnlyIfValueIsInternalError(HttpStatusEnum.BAD_REQUEST_400);
    } else if (thrown instanceof NoSuchElementException) {
      this.setStatusCodeOnlyIfValueIsInternalError(HttpStatusEnum.NOT_FOUND_404);
    }

    this.setDescription(message);

    /**
     * Csrf Validation Error
     * It can happen if there is an error on a post and that the server is reloaded
     * within 60 seconds (ie the new csrf token was not flushed on the disk)
     * We delete the cookie and ask the user to reload the page
     */
    if (this.getStatus() == HttpStatusEnum.FORBIDDEN_403 && message.toLowerCase().contains("token")) {
      context.session().remove(VertxCsrf.getCsrfName());
      context.response().removeCookie(VertxCsrf.getCsrfCookieName());
    }

    if (JavaEnvs.IS_DEV) {
      System.out.println(stackTraceAsString);
    }
    return this;

  }


  public VertxFailureHttp setMimeToJson() {
    this.mime = MediaTypes.TEXT_JSON;
    return this;
  }

  public VertxFailureHttp setMimeToHtml() {
    this.mime = MediaTypes.TEXT_HTML;
    return this;
  }
  public MediaTypes getMime() {
    return this.mime;
  }

  public String getDescription() {
    return this.description;
  }

  public String getName() {
    return this.name;
  }

  /**
   * Return the error in an HTML format
   * It's used when there is problem in a redirect link
   * (ie the user clicks on a confirmation link, if the link is expired
   * it gets a meaningful informational page and not a json)
   */
  public String toHtml(RoutingContext context) {

    Map<String, Object> variables = new HashMap<>();
    variables.put("title", this.getName());
    variables.put("message", this.getDescription());
    if (JavaEnvs.IS_DEV && exception != null) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      exception.printStackTrace(printWriter);
      variables.put("stacktrace", stringWriter.toString());
    }
    return TemplateEngine.getLocalHtmlEngine(context.vertx())
      .compile("Error.html")
      .applyVariables(variables)
      .getResult();

  }

  public ExitStatusResponse toJsonObject() {
    ExitStatusResponse exitStatusResponse = new ExitStatusResponse();
    exitStatusResponse.setCode(status.getStatusCode());
    exitStatusResponse.setMessage(this.getDescription());
    return exitStatusResponse;
  }

  public VertxFailureHttpException getFailedException() {
    return new VertxFailureHttpException(this);
  }


  public VertxFailureHttp setException(Exception e) {
    this.exception = e;
    return this;
  }
}
