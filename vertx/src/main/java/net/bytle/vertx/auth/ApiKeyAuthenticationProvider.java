package net.bytle.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import net.bytle.exception.InternalException;
import net.bytle.vertx.ConfigAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An authentication provider for a token / session id.
 * <p>
 * It handles token:
 * * in header
 * * in cookie
 * <p>
 * You still need to create a {@link io.vertx.ext.web.handler.APIKeyHandler} and mount it on the router
 * with the <a href="https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document">Doc</a>
 */
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

  public static final String SUPERUSER_TOKEN_CONF = "superuser.token";
  public static final String API_KEY_PROVIDER_ID = "apiKey";
  public static final RoleBasedAuthorization ROOT_AUTHORIZATION = RoleBasedAuthorization.create("root");
  static Logger LOGGER = LogManager.getLogger(ApiKeyAuthenticationProvider.class);

  private final String superToken;

  public ApiKeyAuthenticationProvider(ConfigAccessor configAccessor) {
    /**
     * For now, only a super token.
     */
    String superToken = configAccessor.getString(SUPERUSER_TOKEN_CONF);
    if (superToken == null) {
      throw new InternalException("The super token should not be null. You can set in the configuration with the key (" + SUPERUSER_TOKEN_CONF + ")");
    }
    this.superToken = superToken;
    LOGGER.info("ApiKey Authentication Provider created with the conf (" + SUPERUSER_TOKEN_CONF + ")");
  }

  @Override
  public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {

    /**
     * If API Token is used
     */
    String token = credentials.getString("token");
    if (token == null) {
      /**
       * If basic auth is used
       */
      token = credentials.getString("password");
      if (token == null) {
        throw new InternalException("The credentials does not have a token or password property");
      }
    }

    if (superToken == null) {
      resultHandler.handle(Future.failedFuture("No super token"));
      return;
    }

    if (superToken.equals(token)) {
      User user = AuthUser.builder()
        .setSubject("root")
        .setSubjectHandle("root")
        .addAuthorization(API_KEY_PROVIDER_ID, ROOT_AUTHORIZATION)
        .build()
        .getVertxUser();
      resultHandler.handle(Future.succeededFuture(user));
      return;
    }

    resultHandler.handle(Future.failedFuture("Bad Api Key"));

  }


  public String getSuperToken() {
    return this.superToken;
  }


  /**
   * @return the header name to search in the handler
   */
  public String getHeader() {
    /**
     * This is the default header where {@link io.vertx.ext.web.handler.APIKeyHandler}
     * is searching
     */
    return "X-API-KEY";
  }
}
