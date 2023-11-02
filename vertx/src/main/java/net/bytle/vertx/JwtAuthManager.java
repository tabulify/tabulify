package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NoSecretException;

/**
 * Jwt Authentication wrapper around the {@link JWTAuth}
 * <p>
 * We instantiate the {@link JWTAuth} at application start and use it afterward for
 * * to {@link JWTAuth#generateToken(JsonObject) create token}
 * *to {@link JWTAuth#authenticate(Credentials)}  authenticate}
 * <a href="https://vertx.io/docs/vertx-auth-jwt/java/">...</a>
 */
public class JwtAuthManager {


  public static final String JWT_AUTH_SECRET = "jwt.auth.secret";
  public static final String BEARER_TOKEN_TYPE = "bearer";

  private static JwtAuthManager jwtAuthManager;


  private JWTAuth provider;


  public static JwtAuthManager get() {

    return jwtAuthManager;
  }

  public static void init(Vertx vertx, ConfigAccessor jsonConfig) throws NoSecretException {

    String secret = jsonConfig.getString(JWT_AUTH_SECRET);
    if (secret == null) {
      throw new NoSecretException("JwtAuth: A secret is mandatory to sign the JWT. Add one in the conf file with the attribute (" + JWT_AUTH_SECRET + ")");
    }

    jwtAuthManager = new JwtAuthManager();
    JWTAuthOptions config = new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer(secret));

    jwtAuthManager.provider = JWTAuth.create(vertx, config);

  }


  public OAuthAccessTokenResponse generateOAuthAccessTokenResponseFromAuthorization(OAuthAuthorization authorization, RoutingContext routingContext) {


    String accessToken = generateTokenFromAuthorization(authorization, routingContext);

    OAuthAccessTokenResponse oauthAccessTokenResponse = new OAuthAccessTokenResponse();
    oauthAccessTokenResponse.setAccessToken(accessToken);
    oauthAccessTokenResponse.setTokenType(BEARER_TOKEN_TYPE);
    /**
     * No scope for now
     * Grants read-only access to public information
     */
    oauthAccessTokenResponse.setScope("");
    return oauthAccessTokenResponse;

  }

  private String generateTokenFromAuthorization(OAuthAuthorization authorization, RoutingContext routingContext) {
    AuthUser user = authorization.getUser();
    int delay60daysInMinutes = 60 * 60 * 24;
    return generateTokenFromUser(user, delay60daysInMinutes, routingContext);
  }

  public String generateTokenFromUser(AuthUser user, Integer expirationMinutes, RoutingContext routingContext) {
    JsonObject claims = JwtClaimsObject.createFromUser(user, routingContext)
      .toClaimsWithExpiration(expirationMinutes);
    JWTOptions jwtOptions = new JWTOptions();
    return provider.generateToken(claims, jwtOptions);
  }


  public OAuthAccessTokenResponse generateOAuthAccessTokenResponseFromUser(AuthUser user, RoutingContext routingContext) {
    OAuthAuthorization authorization = new OAuthAuthorization();
    authorization.setUser(user);
    return generateOAuthAccessTokenResponseFromAuthorization(authorization, routingContext);
  }

  public JWTAuth getProvider() {
    return this.provider;
  }

  public OAuthAccessTokenResponse generateOAuthAccessTokenResponseFromUser(AuthUser authUser) {
    return generateOAuthAccessTokenResponseFromUser(authUser, null);
  }

}
