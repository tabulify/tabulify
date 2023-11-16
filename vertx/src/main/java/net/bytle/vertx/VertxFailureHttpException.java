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
 * A failure exception and utility data class to:
 * * create the failure data, and fail the context with {@link VertxFailureHttpExceptionBuilder#buildWithContextFailingAsHtml(RoutingContext)} that send it to {@link VertxFailureHttpHandler}
 * * be used in {@link VertxFailureHttpHandler} to create an adequate response
 */
public class VertxFailureHttpException extends Exception {


  private final VertxFailureHttpExceptionBuilder builder;

  public VertxFailureHttpException(VertxFailureHttpExceptionBuilder vertxFailureHttpExceptionBuilder) {
    super(vertxFailureHttpExceptionBuilder.message, vertxFailureHttpExceptionBuilder.exception);
    this.builder = vertxFailureHttpExceptionBuilder;
  }


  public static VertxFailureHttpExceptionBuilder builder() {
    return new VertxFailureHttpExceptionBuilder();
  }







  /**
   * Return the error in an HTML format
   * It's used when there is problem in a redirect link
   * (ie the user clicks on a confirmation link, if the link is expired
   * it gets a meaningful informational page and not a json)
   */
  public String toHtml(RoutingContext context) {

    Map<String, Object> variables = new HashMap<>();
    variables.put("title", this.builder.name);
    variables.put("message", this.getMessage());
    if (JavaEnvs.IS_DEV && this.getCause() != null) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      this.getCause().printStackTrace(printWriter);
      variables.put("stacktrace", stringWriter.toString());
    }
    return TemplateEngine.getLocalHtmlEngine(context.vertx())
      .compile("Error.html")
      .applyVariables(variables)
      .getResult();

  }

  public ExitStatusResponse toJsonObject() {
    ExitStatusResponse exitStatusResponse = new ExitStatusResponse();
    exitStatusResponse.setCode(this.builder.status.getStatusCode());
    exitStatusResponse.setMessage(this.getMessage());
    return exitStatusResponse;
  }

  public HttpStatus getStatus() {
    return this.builder.status;
  }

  public MediaTypes getMime() {
    return this.builder.mime;
  }


  public static class VertxFailureHttpExceptionBuilder {

    private HttpStatus status = HttpStatusEnum.INTERNAL_ERROR_500;
    private String message;
    private String name;

    private MediaTypes mime = MediaTypes.TEXT_HTML;

    /**
     * The exception that has occurred
     */
    private Exception exception;

    public VertxFailureHttpExceptionBuilder setException(Exception e) {
      this.exception = e;
      return this;
    }

    /**
     * @param name - the name of the error (used as title in a HTML page)
     */
    public VertxFailureHttpExceptionBuilder setName(String name) {
      this.name = name;
      return this;
    }

    public VertxFailureHttpExceptionBuilder setMessage(String message) {
      this.message = message;
      return this;
    }

    /**
     * Set the status code if the status code is the internal error (the default)
     *
     * @param code - the code
     */
    private VertxFailureHttpExceptionBuilder setStatusCodeOnlyIfValueIsInternalError(HttpStatus code) {
      if (status == HttpStatusEnum.INTERNAL_ERROR_500) {
        this.status = code;
      }
      return this;
    }

    public VertxFailureHttpExceptionBuilder setMimeToJson() {
      this.mime = MediaTypes.TEXT_JSON;
      return this;
    }

    public VertxFailureHttpExceptionBuilder setMimeToHtml() {
      this.mime = MediaTypes.TEXT_HTML;
      return this;
    }
    public VertxFailureHttpExceptionBuilder setStatus(HttpStatusEnum status) {
      this.status = status;
      return this;
    }

    public VertxFailureHttpException build(){
      return new VertxFailureHttpException(this);
    }

    /**
     * @param context - the context in the {@link VertxFailureHttpHandler}
     */
    private VertxFailureHttpExceptionBuilder setStatusCodeFromFailureContext(RoutingContext context) {
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

    public VertxFailureHttpExceptionBuilder setPropertiesFromFailureContext(RoutingContext context) {

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

      this.message = message;

      /**
       * Csrf Validation Error
       * It can happen if there is an error on a post and that the server is reloaded
       * within 60 seconds (ie the new csrf token was not flushed on the disk)
       * We delete the cookie and ask the user to reload the page
       */
      if (this.status == HttpStatusEnum.FORBIDDEN_403 && message.toLowerCase().contains("token")) {
        context.session().remove(VertxCsrf.getCsrfName());
        context.response().removeCookie(VertxCsrf.getCsrfCookieName());
      }

      if (JavaEnvs.IS_DEV) {
        System.out.println(stackTraceAsString);
      }
      return this;

    }

    public VertxFailureHttpException buildWithContextFailing(RoutingContext routingContext) {

      VertxFailureHttpException httpException = build();
      routingContext.fail(this.status.getStatusCode(), httpException);
      return httpException;

    }
    /**
     * Failing the context returning an HTML page
     * <p>
     * Note: it will send the {@link VertxFailureHttpException}
     * to the {@link VertxFailureHttpHandler}
     *
     */
    public VertxFailureHttpException buildWithContextFailingAsHtml(RoutingContext routingContext) {

      this.mime = MediaTypes.TEXT_HTML;
      return this.buildWithContextFailing(routingContext);

    }

    /**
     * This function fail without returning the exception
     * to not get a warning from the IDE
     * @param ctx - the context
     */
    public void buildWithContextFailingTerminal(RoutingContext ctx) {
      VertxFailureHttpException httpException = build();
      ctx.fail(this.status.getStatusCode(), httpException);
    }

  }
}
