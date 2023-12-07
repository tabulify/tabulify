package net.bytle.vertx;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.Exceptions;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NotLoggedInException;
import net.bytle.exception.NullValueException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.MediaTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A failure exception and utility data class to:
 * * create the failure data, and fail the context with {@link VertxFailureHttpExceptionBuilder#buildWithContextFailing(RoutingContext)} that send it to {@link TowerFailureHttpHandler}
 * * be used in {@link TowerFailureHttpHandler} to create an adequate response
 */
public class TowerFailureException extends Exception {


  private final VertxFailureHttpExceptionBuilder builder;

  public TowerFailureException(VertxFailureHttpExceptionBuilder vertxFailureHttpExceptionBuilder) {
    super(vertxFailureHttpExceptionBuilder.message, vertxFailureHttpExceptionBuilder.causeException);
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
  public String toHtml(RoutingContext context, boolean withStackTrace) {

    Map<String, Object> variables = new HashMap<>();
    variables.put("title", this.builder.name);
    variables.put("message", this.getMessage());
    if (withStackTrace) {
      variables.put("stacktrace", Exceptions.getStackTraceAsString(this));
    }
    return TemplateEngine.getLocalHtmlEngine(context.vertx())
      .compile("Error.html")
      .applyVariables(variables)
      .getResult();

  }

  public JsonObject toJsonObject(boolean withStackTrace) {
    JsonObject res = new JsonObject()
      .put("code", this.builder.status.getStatusCode())
      .put("type", this.builder.status.getType())
      .put("message", this.getMessage());
    if (this.builder.causeException != null) {
      res
        .put("causeType", this.builder.causeException.getClass().getSimpleName())
        .put("causeMessage", this.builder.causeException.getMessage());
    }
    if (withStackTrace) {
      String stackTraceAsString = Exceptions.getStackTraceAsString(this);
      res.put("stackTrace", stackTraceAsString);
    }
    return res;

  }

  public TowerFailureType getStatus() {
    return this.builder.status;
  }

  public MediaTypes getMime() {
    return this.builder.mime;
  }


  public static class VertxFailureHttpExceptionBuilder {

    private TowerFailureType status = TowerFailureTypeEnum.INTERNAL_ERROR_500;
    private String message;
    private String name;

    private MediaTypes mime = MediaTypes.TEXT_JSON;

    /**
     * The exception that has occurred
     */
    private Throwable causeException;

    public VertxFailureHttpExceptionBuilder setCauseException(Throwable e) {
      this.causeException = e;
      return this;
    }

    /**
     * @param name - the name of the error (used as title in an HTML page)
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
    private VertxFailureHttpExceptionBuilder setStatusCodeOnlyIfValueIsInternalError(TowerFailureType code) {
      if (status == TowerFailureTypeEnum.INTERNAL_ERROR_500) {
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

    public VertxFailureHttpExceptionBuilder setType(TowerFailureTypeEnum status) {
      this.status = status;
      return this;
    }

    public TowerFailureException build() {
      if (message == null && status != null) {
        try {
          message = status.getMessage();
        } catch (NullValueException e) {
          //
        }
      }
      return new TowerFailureException(this);
    }

    /**
     * @param context - the context in the {@link TowerFailureHttpHandler}
     */
    private VertxFailureHttpExceptionBuilder setStatusCodeFromFailureContext(RoutingContext context) {
      int httpStatusCode = context.statusCode();
      if (status.getStatusCode() == -1) {
        status = TowerFailureTypeEnum.INTERNAL_ERROR_500;
      } else {
        try {
          status = TowerFailureTypeEnum.fromHttpStatusCode(httpStatusCode);
        } catch (NotFoundException e) {
          status = new TowerFailureType() {
            @Override
            public int getStatusCode() {
              return httpStatusCode;
            }

            @Override
            public String getMessage() throws NullValueException {
              throw new NullValueException();
            }

            @Override
            public String getType() {
              return "unknown";
            }

          };
        }
      }
      return this;
    }

    public VertxFailureHttpExceptionBuilder setPropertiesFromFailureContext(RoutingContext context) {

      this.setStatusCodeFromFailureContext(context);

      Throwable exception = context.failure();
      this.causeException = exception;

      /**
       * Failure may be not given
       */
      if (exception == null) {
        return this;
      }

      String message = exception.getMessage();


      /**
       * Vertx Validation Exception
       * https://vertx.io/docs/vertx-web-validation/java/#_manage_the_failures
       * The possible subclasses of BadRequestException are:
       * * ParameterProcessorException: To manage a parameter failure
       * * BodyProcessorException: To manage a body failure
       * * RequestPredicateException: To manage a request predicate failure
       */
      if (exception instanceof BadRequestException) {
        this.setStatusCodeOnlyIfValueIsInternalError(TowerFailureTypeEnum.BAD_REQUEST_400);
        this.message = exception.getMessage();
        this.causeException = exception.getCause();
      }

      /**
       * Here and there we still use it directly
       */
      if (exception instanceof ValidationException) {
        this.setStatusCodeOnlyIfValueIsInternalError(TowerFailureTypeEnum.BAD_REQUEST_400);
        String keyWord = ((ValidationException) exception).keyword();
        message += " (Keyword: " + keyWord + ", ";

        String input = "unknown";
        Object inputObject = ((ValidationException) exception).input();
        if (inputObject != null) {
          // for what ever reason it may be null
          input = inputObject.toString();
        }
        message += "Input: " + input + ", ";

        JsonPointer inputScopeJson = ((ValidationException) exception).inputScope();
        String inputScope = "unknown";
        if (inputScopeJson != null) {
          // for what ever reason it may be null
          inputScope = inputScopeJson.toString();
        }
        message += "InputScope: " + inputScope + ")";
      } else if (exception instanceof NotFoundException) {
        this.setStatusCodeOnlyIfValueIsInternalError(TowerFailureTypeEnum.BAD_REQUEST_400);
      } else if (exception instanceof NotLoggedInException) {
        context.response()
          .setStatusCode(HttpResponseStatus.FORBIDDEN.code())
          .end();
        return this;
      } else if (exception instanceof HttpException) {
        int statusCode = ((HttpException) exception).getStatusCode();
        try {
          this.setStatusCodeOnlyIfValueIsInternalError(TowerFailureTypeEnum.fromHttpStatusCode(statusCode));
        } catch (NotFoundException e) {
          this.setStatusCodeOnlyIfValueIsInternalError(
            new TowerFailureType() {
              @Override
              public int getStatusCode() {
                return statusCode;
              }

              @Override
              public String getMessage() throws NullValueException {
                throw new NullValueException();
              }

              @Override
              public String getType() {
                return "unknown";
              }
            }
          );
        }
      } else if (exception instanceof IllegalArgumentException) {
        this.setStatusCodeOnlyIfValueIsInternalError(TowerFailureTypeEnum.BAD_REQUEST_400);
      } else if (exception instanceof NoSuchElementException) {
        this.setStatusCodeOnlyIfValueIsInternalError(TowerFailureTypeEnum.NOT_FOUND_404);
      }

      this.message = message;

      /**
       * Csrf Validation Error
       * It can happen if there is an error on a post and that the server is reloaded
       * within 60 seconds (ie the new csrf token was not flushed on the disk)
       * We delete the cookie and ask the user to reload the page
       */
      if (this.status == TowerFailureTypeEnum.NOT_AUTHORIZED_403 && message.toLowerCase().contains("token")) {
        context.session().remove(VertxCsrf.getCsrfName());
        context.response().removeCookie(VertxCsrf.getCsrfCookieName());
      }

      if (JavaEnvs.IS_DEV) {
        System.out.println(Exceptions.getStackTraceAsString(exception));
      }
      return this;

    }

    public TowerFailureException buildWithContextFailing(RoutingContext routingContext) {

      TowerFailureException httpException = build();
      routingContext.fail(this.status.getStatusCode(), httpException);
      return httpException;

    }

    /**
     * This function fail without returning the exception
     * to not get a warning from the IDE
     *
     * @param ctx - the context
     */
    public void buildWithContextFailingTerminal(RoutingContext ctx) {
      TowerFailureException httpException = build();
      ctx.fail(this.status.getStatusCode(), httpException);
    }

  }
}
