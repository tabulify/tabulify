package net.bytle.tower.util;

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
import net.bytle.tower.eraldy.model.openapi.ExitStatusResponse;
import net.bytle.type.MediaTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A failure utility data class to:
 * * create the failure data, and fail the context with {@link ContextFailureData#failContextAsHtml(RoutingContext)} that send it to {@link ContextFailureHandler}
 * * be used in {@link ContextFailureHandler} to create an adequate response
 * <p>
 */
public class ContextFailureData {


  private String description;
  private String name;
  private int statusCode = HttpStatus.INTERNAL_ERROR;


  private MediaTypes mime = MediaTypes.TEXT_HTML;

  public ContextFailureData() {

  }


  public static ContextFailureData create() {
    return new ContextFailureData();
  }


  public ContextFailureData setDescription(String message) {
    this.description = message;
    return this;
  }

  public ContextFailureData setStatusCode(int statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  /**
   * Failing the context returning an HTML page
   * <p>
   * Note: it will send the {@link ContextFailureException}
   * to the {@link ContextFailureHandler}
   */
  public void failContextAsHtml(RoutingContext routingContext) {

    this.mime = MediaTypes.TEXT_HTML;
    this.failContext(routingContext);

  }

  public void failContext(RoutingContext routingContext) {

    routingContext.fail(this.statusCode, (new ContextFailureException(this)));

  }

  /**
   * @param name - the name of the error (used as title in a HTML page)
   */
  public ContextFailureData setName(String name) {
    this.name = name;
    return this;
  }


  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * @param context - the context in the {@link ContextFailureHandler}
   */
  private ContextFailureData setStatusCodeFromFailureContext(RoutingContext context) {
    statusCode = context.statusCode();
    if (statusCode == -1) {
      statusCode = HttpStatus.INTERNAL_ERROR;
    }
    return this;
  }

  /**
   * Set the status code if the status code is the internal error (the default)
   *
   * @param code - the code
   */
  private ContextFailureData setStatusCodeIfInternalError(int code) {
    if (statusCode == HttpStatus.INTERNAL_ERROR) {
      this.statusCode = code;
    }
    return this;
  }

  public ContextFailureData buildFromFailureContext(RoutingContext context) {

    this.setStatusCodeFromFailureContext(context);

    Throwable thrown = context.failure();

    String message = thrown.getMessage();
    String stackTraceAsString = Exceptions.getStackTraceAsString(thrown);
    if (Env.IS_DEV) {
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
      this.setStatusCodeIfInternalError(HttpResponseStatus.BAD_REQUEST.code());
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
      this.setStatusCodeIfInternalError(HttpResponseStatus.BAD_REQUEST.code());
    } else if (thrown instanceof ParameterProcessorException) {
      this.setStatusCodeIfInternalError(HttpResponseStatus.BAD_REQUEST.code());
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
      this.setStatusCodeIfInternalError(((HttpException) thrown).getStatusCode());
    } else if (thrown instanceof IllegalArgumentException) {
      this.setStatusCodeIfInternalError(HttpResponseStatus.BAD_REQUEST.code());
    } else if (thrown instanceof NoSuchElementException) {
      this.setStatusCodeIfInternalError(HttpResponseStatus.NOT_FOUND.code());
    }

    this.setDescription(message);
    int statusCode = this.getStatusCode();

    /**
     * Csrf Validation Error
     * It can happen if there is an error on a post and that the server is reloaded
     * within 60 seconds (ie the new csrf token was not flushed on the disk)
     * We delete the cookie and ask the user to reload the page
     */
    if (statusCode == HttpStatus.FORBIDDEN && message.toLowerCase().contains("token")) {
      context.session().remove(CsrfUtil.getCsrfName());
      context.response().removeCookie(CsrfUtil.getCsrfCookieName());
    }


    if (Env.IS_DEV) {
      System.out.println(stackTraceAsString);
    }
    return this;

  }



  public ContextFailureData setMimeToJson() {
    this.mime = MediaTypes.TEXT_JSON;
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
   */
  public String toHtml(RoutingContext context) {

    Map<String, Object> variables = new HashMap<>();
    variables.put("title", this.getName());
    variables.put("message", this.getDescription());
    return TemplateEngine.getLocalHtmlEngine(context.vertx())
      .compile("Error.html")
      .applyVariables(variables)
      .getResult();

  }

  public ExitStatusResponse toJsonObject() {
    ExitStatusResponse exitStatusResponse = new ExitStatusResponse();
    exitStatusResponse.setCode(statusCode);
    exitStatusResponse.setMessage(this.getDescription());
    return exitStatusResponse;
  }
}
