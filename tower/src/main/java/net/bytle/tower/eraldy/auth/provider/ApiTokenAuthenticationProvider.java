package net.bytle.tower.eraldy.auth.provider;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.Router;
import net.bytle.exception.InternalException;
import net.bytle.tower.util.OpenApiUtil;

/**
 * An authentication provider for the Combo Api
 * <p>
 * The binding of the open api scheme name and the handler is done in the {@link OpenApiUtil.config#mountOpenApi(Router)}
 * with the `
 * <a href="https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document">Doc</a>
 */
public class ApiTokenAuthenticationProvider implements AuthenticationProvider {

  /**
   * The security auth name used in the spec file
   */
  public static final String BASIC_AUTH_SECURITY_SCHEME = "basicAuth";
  public static final String APIKEY_AUTH_SECURITY_SCHEME = "apiKeyAuth";
  public static final String BEARER_AUTH_SECURITY_SCHEME = "bearerAuth";

  public static final String SUPERUSER_TOKEN_CONF = "superuser.token";

  private final String superToken;

  public ApiTokenAuthenticationProvider(JsonObject jsonConfig) {
    String superToken = (String) jsonConfig.getValue(SUPERUSER_TOKEN_CONF);
    if (superToken == null) {
      throw new InternalException("The super token should not be null. You can set in the configuration with the key (" + SUPERUSER_TOKEN_CONF + ")");
    }
    this.superToken = superToken;
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
      User user = User.create(new JsonObject());

      resultHandler.handle(Future.succeededFuture(user));
      return;
    }

    resultHandler.handle(Future.failedFuture("Bad Api Key"));

  }


}
