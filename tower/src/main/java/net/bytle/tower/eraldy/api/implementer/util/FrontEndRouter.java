package net.bytle.tower.eraldy.api.implementer.util;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.AuthApiImpl;
import net.bytle.tower.eraldy.api.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.util.Env;
import net.bytle.tower.util.OAuthResponseType;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.TowerApexDomain;
import net.bytle.vertx.VertxCsrf;
import net.bytle.vertx.VertxRoutingFailureData;
import net.bytle.vertx.auth.OAuthInternalSession;
import net.bytle.vertx.auth.OAuthQueryProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility Static function that redirects the request
 * to another port or to the static resource
 */
public class FrontEndRouter {


  /**
   * Proxy the request
   *
   * @param routingContext - the context
   */
  private static Future<ApiResponse<String>> proxyGetRequest(EraldyApiApp apiApp , RoutingContext routingContext) {
    /**
     * If you want to see the proxy request in fiddler,
     * set this value to true
     */
    boolean useFiddler = false;
    return apiApp
      .getProxy()
      .proxyRequest(routingContext, useFiddler)
      .compose(buffer -> Future.succeededFuture(new ApiResponse<>(buffer.toString())));
  }

  /**
   * Reroute to non OAuth pages such as list registration)
   *
   * @param routingContext - the routing context
   * @param redirectUri    - optional because the {@link ListRegistrationFlow list registration flow} does not require it
   */
  public static Future<ApiResponse<String>> toPublicNonOAuthPage(EraldyApiApp apiApp, RoutingContext routingContext, UriEnhanced redirectUri) {

    /**
     * Valid redirect uri
     */
    if (redirectUri != null) {
      OAuthInternalSession.addRedirectUri(routingContext, redirectUri);
    }

    if (Env.IS_DEV) {
      return proxyGetRequest(apiApp, routingContext);
    }
    throw new RuntimeException("Not yet implemented");

  }

  /**
   * Redirect to a private page such as confirmation, password update
   *
   * @param routingContext - the routing context
   * @param mandatoryRedirectUri - by default, the redirect uri is mandatory but it may not
   * @return the response
   */
  public static Future<ApiResponse<String>> toPrivatePage(EraldyApiApp apiApp, RoutingContext routingContext, Boolean mandatoryRedirectUri) {

    if (routingContext.user() == null) {
      String message = "You should be logged in.";
      String redirectUri = routingContext.request().getParam(OAuthQueryProperty.REDIRECT_URI.toString());
      if (redirectUri != null) {
        message += " Click <a href=\"" + redirectUri + "\">here</a> to log in.";
      }
      VertxRoutingFailureData.create()
        .setDescription(message)
        .setName(message)
        .failContextAsHtml(routingContext);
      return Future.succeededFuture(new ApiResponse<>(HttpStatus.NOT_AUTHORIZED.httpStatusCode()));
    }


    if(mandatoryRedirectUri) {
      /**
       * Redirect URI is mandatory
       */
      try {
        AuthApiImpl.getRedirectUri(routingContext);
      } catch (NotFoundException e) {
        VertxRoutingFailureData.create()
          .setName("Redirect Uri is mandatory")
          .setDescription("The redirect URI is mandatory and was not found")
          .failContextAsHtml(routingContext);
        return Future.succeededFuture(new ApiResponse<>(HttpStatus.BAD_REQUEST.httpStatusCode()));
      }
    }


    /**
     * Session is mandatory
     */
    final Session session = routingContext.session();
    if (session == null) {
      throw new InternalException("A session is required. Did you place a session handler before the API builder?");
    }


    /**
     * Dev Proxy request
     */
    if (Env.IS_DEV) {
      return proxyGetRequest(apiApp, routingContext);
    }
    throw new RuntimeException("Not implemented yet");

  }

  /**
   * Reroute all OAuth public page
   * This utility checks that all parameters are set
   *
   * @return the response
   */
  public static Future<ApiResponse<String>> toPublicOAuthPage(EraldyApiApp apiApp, RoutingContext routingContext) {

    /**
     * We take the value of the parameters here
     * because if there is a switch on position in the open api specification,
     * the data are still string and the api does not cry even if the data received
     * is not the good one
     */
    HttpServerRequest request = routingContext.request();
    String inTypeRequest = request.getParam(OAuthQueryProperty.RESPONSE_TYPE.toString());
    String inClientId = request.getParam(OAuthQueryProperty.CLIENT_ID.toString());
    String inState = request.getParam(OAuthQueryProperty.STATE.toString());

    /**
     * Session is mandatory
     */
    final Session session = routingContext.session();
    if (session == null) {
      throw new InternalException("A session is required. Did you place a session handler before the API builder?");
    }

    UriEnhanced redirectUriAsUri;
    try {
      redirectUriAsUri = AuthApiImpl.getRedirectUri(routingContext);
    } catch (ValidationException e) {
      return Future.failedFuture(e);
    } catch (NotFoundException e) {
      return Future.failedFuture(ValidationException.create("The redirect uri was not found", OAuthQueryProperty.REDIRECT_URI.toString(), null));
    }
    OAuthInternalSession.addRedirectUri(routingContext, redirectUriAsUri);

    /**
     * First party redirect?
     */
    TowerApexDomain firstPartyDomain = apiApp.getApexDomain();
    boolean isFirstPartyRequest = redirectUriAsUri.getApexWithoutPort().equals(firstPartyDomain.getApexNameWithoutPort());


    /**
     * Client Id
     */
    String outClientId;
    if (!isFirstPartyRequest && inClientId == null) {
      throw ValidationException.create("The client id cannot be null for a third party domain", OAuthQueryProperty.CLIENT_ID.toString(), null);
    } else {
      outClientId = redirectUriAsUri.getSubDomain();
    }
    if (!AuthApiImpl.SUPPORTED_CLIENT_IDS.contains(outClientId)) {
      throw ValidationException.create("The client id is not valid", "client_id", outClientId);
    }
    session.put(OAuthInternalSession.CLIENT_ID_KEY, outClientId);


    /**
     * Response Type
     */
    String outResponseType;
    if (!isFirstPartyRequest) {
      if (inTypeRequest == null) {
        throw ValidationException.create("The response type cannot be null for a third party domain", OAuthQueryProperty.RESPONSE_TYPE.toString(), null);
      }
      OAuthResponseType oAuthResponseType;
      try {
        oAuthResponseType = Casts.cast(inTypeRequest, OAuthResponseType.class);
      } catch (CastException e) {
        throw ValidationException.create("The responseType is not valid. It must be one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(OAuthResponseType.class), "response_type", inTypeRequest);
      }
      outResponseType = oAuthResponseType.toString();
    } else {
      outResponseType = OAuthResponseType.SESSION_COOKIE.toString();
    }
    session.put(OAuthInternalSession.RESPONSE_TYPE_KEY, outResponseType);

    /**
     * State validation
     */
    String outState = inState;
    if (inState == null) {
      if (!isFirstPartyRequest) {
        throw ValidationException.create("The state was not found and is mandatory for a third party redirection.", "state", null);
      }
      outState = "foobar";
    }
    session.put(OAuthInternalSession.STATE_KEY, outState);


    /**
     * Dev Proxy request
     */
    if (Env.IS_DEV) {
      return proxyGetRequest(apiApp, routingContext);
    }


    Map<String, Object> variables = new HashMap<>();
    variables.put("title", "The page title");
    variables.put("h1", "The h1 title");
    variables.put("description", "A login text");
    variables.put("action", "/login");


    try {
      String csrf = VertxCsrf.getCsrfToken(routingContext);
      variables.put("csrf", csrf);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }

    variables.put("termsurl", "");

    String formHtml = apiApp
      .getTemplate("login.html")
      .applyVariables(variables)
      .getResult();
    routingContext
      .response()
      .putHeader("Content-Type", "text/html")
      .end(formHtml);
    return Future.succeededFuture(new ApiResponse<>());
  }
}
