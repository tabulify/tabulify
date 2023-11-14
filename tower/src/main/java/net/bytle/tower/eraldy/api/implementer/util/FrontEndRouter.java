package net.bytle.tower.eraldy.api.implementer.util;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.util.Env;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.HttpStatusEnum;
import net.bytle.vertx.VertxFailureHttp;
import net.bytle.vertx.auth.AuthQueryProperty;
import net.bytle.vertx.auth.OAuthExternalCodeFlow;
import net.bytle.vertx.auth.OAuthInternalSession;

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
      String redirectUri = routingContext.request().getParam(AuthQueryProperty.REDIRECT_URI.toString());
      if (redirectUri != null) {
        message += " Click <a href=\"" + redirectUri + "\">here</a> to log in.";
      }
      VertxFailureHttp.create()
        .setDescription(message)
        .setName(message)
        .failContextAsHtml(routingContext);
      return Future.succeededFuture(new ApiResponse<>(HttpStatusEnum.NOT_AUTHORIZED_401.getStatusCode()));
    }


    if(mandatoryRedirectUri) {
      /**
       * Redirect URI is mandatory
       */
      try {
        OAuthExternalCodeFlow.getRedirectUri(routingContext);
      } catch (NotFoundException e) {
        VertxFailureHttp.create()
          .setName("Redirect Uri is mandatory")
          .setDescription("The redirect URI is mandatory and was not found")
          .failContextAsHtml(routingContext);
        return Future.succeededFuture(new ApiResponse<>(HttpStatusEnum.BAD_REQUEST_400.getStatusCode()));
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

}
