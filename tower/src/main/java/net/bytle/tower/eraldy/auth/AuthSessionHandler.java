package net.bytle.tower.eraldy.auth;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import net.bytle.java.JavaEnvs;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.util.PersistentLocalSessionStore;
import net.bytle.vertx.TowerApexDomain;

public class AuthSessionHandler {

  /**
   * A handler that maintains a {@link io.vertx.ext.web.Session} for each browser session
   * with a cookie
   * <p>
   * It looks up the session for each request based on a session cookie called `vertx-web.session`
   * which contains a session ID. It stores the session when the response is ended in the session store.
   * <p>
   * The session is available on the routing context with {@link RoutingContext#session()}
   * <p>
   * Get: Integer cnt = session.get("hitcount");
   * Put: session.put("hitcount", cnt);
   * <p>
   * Doc: <a href="https://vertx.io/docs/vertx-web/java/#_handling_sessions">...</a>
   * <a href="https://vertx.io/docs/vertx-web/java/#_creating_the_session_handler">...</a>
   * <p>
   * Sessions last between HTTP requests for the length of a browser session
   * and give you a place where you can add session-scope information, such as a shopping basket.
   * <p>
   * Sessions can’t work if browser doesn’t support cookies.
   */
  public static void addAuthCookieSessionHandler(Router rootRouter, EraldyApiApp apiApp) {

    /**
     * Determine the auth realm and put it on the routingContext
     * As a user can log in to only on realm, the session cookie has the name
     * of the realm in its name
     * This handler should then be before the session handler
     */
    AuthRealmHandler.createFrom(rootRouter, apiApp);

    /**
     * This is not a cookie store. Cookie store does not work well with CSRF
     * because on post, the session cookie `cs-session-id` is not sent back with new value
     * A lot of problem with this way of working because the data is the session id
     * CookieSessionStore sessionStore = CookieSessionStore.create(towerDomain.getVertx(), secret);
     */
    /**
     * This is a {@link io.vertx.ext.web.sstore.LocalSessionStore} that
     * was adapted to persist the session
     */
    long syncInterval = PersistentLocalSessionStore.INTERVAL_60_SEC;
    if (JavaEnvs.IS_DEV) {
      syncInterval = PersistentLocalSessionStore.INTERVAL_5_SEC;
    }
    TowerApexDomain apexDomain = apiApp.getApexDomain();
    PersistentLocalSessionStore sessionStore = PersistentLocalSessionStore
      .create(apexDomain.getHttpServer().getServer().getVertx(), syncInterval);
    /**
     * Reconnect once every
     */
    int cookieMaxAgeOneWeekInSec = 60 * 60 * 24 * 7;
    /**
     * Delete the session if not accessed within this timeout
     */
    int idleSessionTimeoutMs = cookieMaxAgeOneWeekInSec * 1000;
    SessionHandler requestHandler = EraldySessionHandler
      .createWithDomain(sessionStore, apexDomain)
      .setSessionTimeout(idleSessionTimeoutMs)
      .setCookieMaxAge(cookieMaxAgeOneWeekInSec);

    rootRouter.route().handler(requestHandler);

  }

}
