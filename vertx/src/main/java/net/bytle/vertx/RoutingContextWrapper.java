package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.UriEnhanced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Wrap a {@link io.vertx.ext.web.RoutingContext} to extract information
 */
public class RoutingContextWrapper {

  public static final String CONTEXT_REROUTED_PATH = "REROUTE_ORIGINAL_REQUEST_PATH";
  protected static final Logger LOGGER = LoggerFactory.getLogger(RoutingContextWrapper.class);
  private static final String ROUTING_CONTEXT_WRAPPER_CTX_KEY = "RoutingContextWrapper";


  private final RoutingContext ctx;
  private final RequestParameters requestParameters;
  private UriEnhanced requestUri;

  public RoutingContextWrapper(RoutingContext routingContext) {

    this.ctx = routingContext;
    this.requestParameters = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);

  }

  public static RoutingContextWrapper createFrom(RoutingContext routingContext) {
    /**
     * Cache ... :) Could not stand to get the request uri multiple time as uri
     * {@link #getOriginalRequestAsUri()}
     */
    RoutingContextWrapper routingContextWrapper = routingContext.get(ROUTING_CONTEXT_WRAPPER_CTX_KEY);
    if (routingContextWrapper == null) {
      routingContextWrapper = new RoutingContextWrapper(routingContext);
      routingContext.put(ROUTING_CONTEXT_WRAPPER_CTX_KEY, routingContextWrapper);
    }
    return routingContextWrapper;
  }

  public String getRealRemoteClientIp() throws NotFoundException {

    return HttpRequestUtil.getRealRemoteClientIp(this.ctx.request());
  }

  public URI getReferer() throws NotFoundException {

    String refererValue = this.ctx.request().getHeader(HttpHeaders.REFERER);
    if (refererValue == null) {
      throw new NotFoundException("No referer");
    }
    try {
      return UriEnhanced.createFromString(refererValue).toUri();
    } catch (IllegalStructure e) {
      if (JavaEnvs.IS_DEV) {
        throw ValidationException.create("The referer header is not a valid uri. Error:" + e.getMessage(), HttpHeaders.REFERER, refererValue);
      } else {
        String msg = "The referer header is not a valid uri (" + refererValue + "). Error:" + e.getMessage();
        LOGGER.warn(msg);
        throw new NotFoundException(msg);
      }
    }
  }

  /**
   * A {@link TowerFailureTypeEnum#REDIRECT_302 302} redirects the POST
   * The browser tries to perform the post on the redirected URI.
   * <p>
   * When you want to redirect to a GET page after a successful POST,
   * you should use a 303
   * <p>
   * Note that it does not work on Chrome
   *
   * @param redirectUri - the redirect URI
   */
  @SuppressWarnings("unused")
  public void seeOtherUriRedirect(String redirectUri) {
    this.ctx.response()
      .setStatusCode(TowerFailureTypeEnum.REDIRECT_SEE_OTHER_URI_303.getStatusCode())
      .putHeader(HttpHeaders.LOCATION, redirectUri)
      .putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
      .end("Redirecting to " + redirectUri + ".");
  }

  /**
   * @return the original request before {@link #reroute(String) rerouting} if any
   */
  @SuppressWarnings("unused")
  public UriEnhanced getOriginalRequestAsUri() {
    if (this.requestUri != null) {
      return this.requestUri;
    }
    String url = this.ctx.request().absoluteURI();
    try {
      this.requestUri = UriEnhanced.createFromString(url);
    } catch (IllegalStructure e) {
      // should not happen
      throw new InternalException("The request URI is not valid (" + url + ")", e);
    }
    String originalPath = this.getOriginalRequestPath();
    if (originalPath != null) {
      this.requestUri.setPath(originalPath);
    }
    return this.requestUri;
  }

  /**
   * @return the path of the original request before any rerouting
   */
  private String getOriginalRequestPath() {
    String pathBeforeRerouting = ctx.get(CONTEXT_REROUTED_PATH, null);
    if (pathBeforeRerouting != null) {
      return pathBeforeRerouting;
    }
    return ctx.request().path();
  }


  /**
   * A {@link RoutingContext#reroute(String) Reroute } should happen only once
   * This utility helps with it.
   */
  public boolean isReRouteOccurring() {
    String oldPath = ctx.get(CONTEXT_REROUTED_PATH, null);
    return oldPath != null;
  }

  public void reroute(String reRouteString) {
    String oldPath = ctx.request().path();
    ctx
      .put(RoutingContextWrapper.CONTEXT_REROUTED_PATH, oldPath)
      .reroute(reRouteString);
  }


  public Vertx getVertx() {
    return this.ctx.vertx();
  }

  public RoutingContext getRoutingContext() {
    return this.ctx;
  }


  public void respond(Object data) {
    HttpServerResponse response = ctx.response();
    if (response.headWritten()) {
      if (data == null) {
        if (!response.ended()) {
          ctx.end();
        }
        return;
      }
      ctx.fail(new HttpException(500, "Response already written"));
    }

    if (data == null) {
      int statusCode = response.getStatusCode();
      if (statusCode == 200) {
        response.setStatusCode(204);  // No Content success status response
      }
      response.end();
      return;
    }

    final boolean hasContentType = response.headers().contains(io.vertx.core.http.HttpHeaders.CONTENT_TYPE);
    if (data instanceof Buffer) {
      if (!hasContentType) {
        response.putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE, "application/octet-stream");
      }
      ctx.end((Buffer) data);
      return;
    }

    if (data instanceof String) {
      if (!hasContentType) {
        response.putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE, "text/html");
      }
      ctx.end((String) data);
      return;
    }

    ctx.json(data);

  }

  public HttpServerResponse response() {
    return this.ctx.response();
  }

  private RequestParameter getRequestQueryParameter(String parameterName) {
    if (requestParameters == null) {
      return null;
    }
    return requestParameters.queryParameter(parameterName);
  }

  /**
   * The order is in an openapi spec file may change but not the signature of the function
   * leading to error. We asks then every time, the value of the parameter again
   * @param parameterName - the parameter name
   * @return the value
   */
  public Long getRequestQueryParameterAsLong(String parameterName, Long defaultValue) {
    RequestParameter requestQueryParameter = getRequestQueryParameter(parameterName);
    return requestQueryParameter != null ? requestQueryParameter.getLong() : defaultValue;
  }



}
