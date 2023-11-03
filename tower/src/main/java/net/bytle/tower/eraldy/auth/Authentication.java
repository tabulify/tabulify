package net.bytle.tower.eraldy.auth;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.sqlclient.SqlAuthentication;
import io.vertx.ext.auth.sqlclient.SqlAuthenticationOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.ChainAuthHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.pgclient.PgPool;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.JdbcPostgresPool;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;

/**
 * Authentication in Vertx
 * <p>
 * All handler are already implemented here
 * <a href="https://github.com/vert-x3/vertx-web/tree/master/vertx-web/src/main/java/io/vertx/ext/web/handler">...</a>
 * You just have to give a provider.
 * <p>
 * If an authentication handler failed, the whole request fails.
 * With OpenApi, the {@link ApiKeyAuthenticationProvider handler} is bind in the {@link TowerApp#openApiBindSecurityScheme(RouterBuilder, ConfigAccessor)}
 * <p>
 * username / password
 * <a href="https://vertx.io/docs/vertx-web/java/#_handling_authentication_in_your_application">...</a>
 * <a href="https://vertx.io/docs/vertx-web/java/#_redirect_authentication_handler">...</a>
 * use with a {@link io.vertx.ext.web.handler.impl.FormLoginHandlerImpl}
 */
public class Authentication {

  /**
   * Not yet used, code is here for reference
   */
  @SuppressWarnings("unused")
  public static void addAuthentication(Vertx vertx, Router router) {

    /**
     * Form authentication
     * where credentials are stored in a SQL database
     * <a href="https://vertx.io/docs/vertx-auth-sql-client/java/">...</a>
     */
    SqlAuthenticationOptions options = new SqlAuthenticationOptions()
      .setAuthenticationQuery("");
    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();
    AuthenticationProvider authenticationProvider = SqlAuthentication.create(jdbcPool, options);
    FormAuthenticationHandler formAuthHandler = FormAuthenticationHandler.create(authenticationProvider);
    router.route("/login").handler(formAuthHandler);



    /**
     * Api Authentication
     */
    AuthenticationProvider authProvider = new ApiKeyAuthenticationProvider(ConfigAccessor.empty());
    APIKeyHandler apiKeyHandler = APIKeyHandler.create(authProvider);
    BasicAuthHandler basicAuthHandler = BasicAuthHandler.create(authProvider);
    RedirectAuthHandler redirectAuthHandler = RedirectAuthHandler.create(authProvider);

    ChainAuthHandler chain = ChainAuthHandler
      .any()
      .add(basicAuthHandler)
      .add(apiKeyHandler)
      .add(redirectAuthHandler);
    router.route("/myapp").handler(chain);

  }

}
