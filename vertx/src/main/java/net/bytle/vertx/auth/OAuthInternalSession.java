package net.bytle.vertx.auth;

import io.vertx.ext.web.RoutingContext;
import net.bytle.type.UriEnhanced;

/**
 * The class that contains the keys
 * for data stored in a session
 */
public class OAuthInternalSession {


  /**
   * A suffix to avoid naming conflict in the session
   * with the {@link OAuthExternal external Oauth functionality}
   */
  private static final String PREFIX = "oauth-internal-";


  /**
   * The redirect uri received is stored in a session with this key
   */
  public static final String REDIRECT_URI_KEY = PREFIX + "redirect-uri";
  /**
   * The client id received is stored in a session with this key
   */
  public static final String CLIENT_ID_KEY = PREFIX + "client-id";
  /**
   * The state received is stored in a session with this key
   */
  public static final String STATE_KEY = PREFIX + "state";
  /**
   * The response type received is stored in a session with this key
   */
  public static final String RESPONSE_TYPE_KEY = PREFIX + "response-type";

  public static void addRedirectUri(RoutingContext context, UriEnhanced uriEnhanced) {
    context.session().put(OAuthInternalSession.REDIRECT_URI_KEY, uriEnhanced.toUrl().toString());
  }

}
