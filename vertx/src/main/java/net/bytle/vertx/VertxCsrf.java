package net.bytle.vertx;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.CSRFHandler;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;

public class VertxCsrf {


  private static final String CSRF_SECRET_KEY_CONF = "csrf.secret";

  /**
   * Add a handler that creates a csrf token and add it in a cookie
   * <p>
   * This handler adds a CSRF token to requests which mutate state.
   * In order change the state a (XSRF-TOKEN) cookie is set with a unique token,
   * that is expected to be sent back in a (X-XSRF-TOKEN) header.
   * The behavior is to check the request body header and cookie for validity.
   * This Handler requires session support, thus should be added somewhere below Session and Body handlers.
   */
  public static void addCsrfCookieToken(Router rootRouter, TowerApp app) {

    /**
     * Not a dynamic secret
     * (ie String secret = RandomSecret.config().setSize(10).build().generate();)     *
     * Because otherwise, we get the error `token signature does not match`
     * at {@link io.vertx.ext.web.handler.impl.CSRFHandlerImpl#isValidRequest(RoutingContext)}
     * after server restart
     */
    String secret = app.getApexDomain().getHttpServer().getConfigAccessor().getString(CSRF_SECRET_KEY_CONF);
    if (secret == null) {
      throw new InternalException("The secret CSRF configuration was not found (" + CSRF_SECRET_KEY_CONF + ").");
    }
    String path = app.getAbsoluteLocalPathWithDomain() + "/*";
    rootRouter.route(path).handler(
      CSRFHandler.create(app.getVertx(), secret)
        .setCookieName(getCsrfCookieName())
        .setHeaderName(getCsrfName())
    );
  }

  /**
   * @return The csrf token to be added in the meta or in a form
   * <p>
   * Example from <a href="https://vertx.io/docs/vertx-web/java/#_using_ajax">...</a>:
   * ```
   * <meta name="csrf-token" th:content="${X-XSRF-TOKEN}">
   * <meta name="csrf-param" content="authenticity_token">
   * <p></p>
   * <input type="hidden" name="X-XSRF-TOKEN" th:value="${X-XSRF-TOKEN}">
   * ```
   */
  public static String getCsrfToken(RoutingContext routingContext) throws NotFoundException {

    Session session = routingContext.session();
    if (session == null) {
      throw new RuntimeException("A session is mandatory");
    }
    /**
     * Get the value `session/csrfToken` from the session
     */
    String sessionToken = session.get(getCsrfName());
    if (sessionToken != null) {
      /**
       * Extract the token
       */
      int idx = sessionToken.indexOf('/');
      if (idx != -1 && session.id() != null && session.id().equals(sessionToken.substring(0, idx))) {
        return sessionToken.substring(idx + 1);
      }
    }
    throw new NotFoundException("No Csrf Token found");
  }

  /**
   * @return the name of the cookie where the CSRF token is stored
   * This is fixed as we can have problem with any app, we need to be able to delete it
   */
  public static String getCsrfCookieName() {
    return "tower-csrf-token";
  }

  /**
   * @return the name of the HTTP header where the CSRF token is:
   * * expected
   * * is stored in the {@link io.vertx.ext.web.Session#get(String)}
   */
  public static String getCsrfName() {
    return "x-" + getCsrfCookieName();
  }


}
