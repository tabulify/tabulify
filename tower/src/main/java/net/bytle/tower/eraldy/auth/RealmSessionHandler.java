package net.bytle.tower.eraldy.auth;

import io.vertx.core.*;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.ContextInternal;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.impl.SessionHandlerImpl;
import io.vertx.ext.web.handler.impl.UserHolder;
import io.vertx.ext.web.impl.RoutingContextInternal;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.sstore.impl.SessionInternal;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.vertx.TowerApexDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptation of the {@link io.vertx.ext.web.handler.impl.SessionHandlerImpl default session handler} that:
 * <p>
 * * allow to set the cookie to the Domain scope in {@link #setCookieProperties(Cookie, boolean, RoutingContext)}
 * * create a session with a realm scope
 * <p>
 * The default Vertx implementation is based on
 * <a href="https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html#session-id-properties">owasp</a>
 * recommendation, but because we use a central authentication mechanism and not different authentication by application
 * we can (github, google, ...) do it
 * <p>
 * A handler that maintains a {@link io.vertx.ext.web.Session} for each browser session
 * with a cookie based on a realm key (See {@link #getSessionCookieName(RoutingContext)}
 * <p>
 * It stores the session when the response is ended in the session store.
 * <p>
 * The session is available on the routing context with {@link RoutingContext#session()}
 * <p>
 * Get: Integer cnt = session.get("hitcount");
 * Put: session.put("hitcount", cnt);
 * <p>
 * Doc: <a href="https://vertx.io/docs/vertx-web/java/#_handling_sessions">...</a>
 * <a href="https://vertx.io/docs/vertx-web/java/#_creating_the_session_handler">...</a>
 * <p>
 * They last between HTTP requests for the length of a browser session
 * and give you a place where you can add session-scope information, such as a shopping basket.
 * <p>
 * Sessions can’t work if the browser doesn’t support cookies or if the realm key is not set
 */
public class RealmSessionHandler implements SessionHandler {

  /**
   * The key on the session data where to retrieve the user
   */
  public static final String SESSION_USER_HOLDER_KEY = SessionHandlerImpl.SESSION_USER_HOLDER_KEY;

  /**
   * Indicator to see if the session data was flushed to the store
   */
  public static final String SESSION_WAS_FLUSHED_KEY = SessionHandlerImpl.SESSION_FLUSHED_KEY;
  /**
   * A signal we must store the user to link it to the session as it wasn't found
   */
  public static final String SESSION_STORE_USER_KEY = SessionHandlerImpl.SESSION_STOREUSER_KEY;

  /**
   * Cross Session Cookie
   * (On heavy trusted domain, we may allow cross-session cookie)
   */
  public static final String SESSION_COOKIE_SAME_SITE_KEY = "cs-session-cross-cookie";

  private static final Logger LOG = LoggerFactory.getLogger(RealmSessionHandler.class);
  private static RealmSessionHandler realmSessionHandler;

  private final SessionStore sessionStore;
  private final TowerApexDomain eraldyDomain;

  private long sessionTimeout = DEFAULT_SESSION_TIMEOUT;
  private boolean nagHttps = DEFAULT_NAG_HTTPS;


  /**
   * OWASP min length requirements
   * <a href="https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html#session-id-length">Session Id Length</a>
   */
  private int minLength = DEFAULT_SESSIONID_MIN_LENGTH;
  private boolean lazySession = DEFAULT_LAZY_SESSION;
  private long cookieMaxAge = -1;

  /**
   * the context key where to find the realm handle
   */
  private String realmHandleContextKey;


  public RealmSessionHandler(TowerApexDomain apexDomain) {
    this.sessionStore = apexDomain.getHttpServer().getPersistentSessionStore();
    this.eraldyDomain = apexDomain;
  }

  public static RealmSessionHandler createWithDomain(TowerApexDomain eraldyDomain) {
    if (RealmSessionHandler.realmSessionHandler == null) {
      RealmSessionHandler.realmSessionHandler = new RealmSessionHandler(eraldyDomain);
    }
    return RealmSessionHandler.realmSessionHandler;
  }

  public static RealmSessionHandler get() {
    return realmSessionHandler;
  }

  /**
   * The amount of time in ms, after which the session will expire, if not accessed.
   * The timeout delete the user on the session if it's not accessed within this time
   * See {@link Session#timeout()}
   *
   * @param timeout the timeout, in ms.
   */
  @Override
  public RealmSessionHandler setSessionTimeout(long timeout) {
    this.sessionTimeout = timeout;
    return this;
  }

  @Override
  public RealmSessionHandler setNagHttps(boolean nag) {
    this.nagHttps = nag;
    return this;
  }

  @Override
  public RealmSessionHandler setCookieSecureFlag(boolean secure) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public RealmSessionHandler setCookieHttpOnlyFlag(boolean httpOnly) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public RealmSessionHandler setSessionCookieName(String sessionCookieName) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public RealmSessionHandler setSessionCookiePath(String sessionCookiePath) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public RealmSessionHandler setMinLength(int minLength) {
    this.minLength = minLength;
    return this;
  }

  @Override
  public RealmSessionHandler setCookieSameSite(CookieSameSite policy) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public RealmSessionHandler setLazySession(boolean lazySession) {
    this.lazySession = lazySession;
    return this;
  }

  @Override
  public RealmSessionHandler setCookieMaxAge(long cookieMaxAge) {
    this.cookieMaxAge = cookieMaxAge;
    return this;
  }

  @Override
  public RealmSessionHandler setCookieless(boolean cookieless) {
    /**
     * Cookie less sends the session via HTTP query parameters
     */
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Future<Void> flush(RoutingContext context, boolean ignoreStatus) {
    String cookieName;
    try {
      cookieName = this.getSessionCookieName(context);
    } catch (NotFoundException e) {
      return Future.succeededFuture();
    }
    return flushSessionToStore(context, false, ignoreStatus, cookieName);
  }


  /**
   * Ensure that the cookie properties are always set the same way
   * on generation and on update.
   *
   * @param cookie  the cookie to set
   * @param context the actual request context
   */
  private void setCookieProperties(Cookie cookie, boolean expired, RoutingContext context) {

    cookie.setPath(SessionHandler.DEFAULT_SESSION_COOKIE_PATH);
    cookie.setHttpOnly(true);

    /**
     * Allow login on click from third email
     */
    CookieSameSite crossCookie = context.get(SESSION_COOKIE_SAME_SITE_KEY, CookieSameSite.LAX);
    cookie.setSameSite(crossCookie);

    /**
     * Create a cross domain cookie
     */
    cookie.setDomain(eraldyDomain.getApexNameWithoutPort());

    /**
     * Over Https
     */
    cookie.setSecure(eraldyDomain.getHttpServer().isHttpsEnabled());

    if (!expired) {
      // set max age if user requested it - else it's a session cookie
      if (cookieMaxAge >= 0) {
        cookie.setMaxAge(cookieMaxAge);
      }
    }

  }

  /**
   * Store the session (called via the {@link #addStoreSessionHandler(RoutingContext, String)}
   */
  private Future<Void> flushSessionToStore(RoutingContext context, boolean skipCrc, boolean ignoreStatus, String cookieName) {
    final boolean sessionUsed = context.isSessionAccessed();
    final Session session = context.session();
    final ContextInternal ctx = (ContextInternal) context.vertx()
      .getOrCreateContext();

    if (session == null) {
      /**
       * No session in context, no need to store
       */
      return ctx.succeededFuture();

    }

    if (!session.isDestroyed()) {

      final int currentStatusCode = context.response().getStatusCode();
      // Store the session (only and only if there was no error)
      if (ignoreStatus || (currentStatusCode >= 200 && currentStatusCode < 400)) {
        // store the current user into the session
        Boolean storeUser = context.get(SESSION_STORE_USER_KEY);
        if (storeUser != null && storeUser) {
          // during the request the user might have been removed
          if (context.user() != null) {
            UserHolder userHolder = new UserHolder(context);
            // set the user: bug, if the session is written before, the user holder may exist
            // without any user
            userHolder.refresh(context);
            session.put(SESSION_USER_HOLDER_KEY, userHolder);
          }
        }


        if (session.isRegenerated()) {

          // this means that a session id has been changed, usually it means a session
          // upgrade
          // (e.g.: anonymous to authenticated) or that the security requirements have
          // changed
          // see:
          // https://www.owasp.org/index.php/Session_Management_Cheat_Sheet#Session_ID_Life_Cycle

          // the session cookie needs to be updated to the new id
          final Cookie cookie = sessionCookie(context, session, cookieName);
          // restore defaults
          session.setAccessed();
          cookie.setValue(session.value());
          setCookieProperties(cookie, false, context);

          // we must invalidate the old id
          return sessionStore.delete(session.oldId())
            .compose(delete -> {
              // we must wait for the result of the previous call in order to save the new one
              return sessionStore.put(session)
                .onSuccess(put -> {
                  context.put(SESSION_WAS_FLUSHED_KEY, true);
                  if (session instanceof SessionInternal) {
                    ((SessionInternal) session).flushed(skipCrc);
                  }
                });
            });
        } else if (!lazySession || sessionUsed) {
          // if lazy mode activated, no need to store the session nor to create the session cookie if not used.
          sessionCookie(context, session, cookieName);
          session.setAccessed();
          return sessionStore.put(session)
            .onSuccess(put -> {
              context.put(SESSION_WAS_FLUSHED_KEY, true);
              if (session instanceof SessionInternal) {
                ((SessionInternal) session).flushed(skipCrc);
              }
            });
        } else {
          // No-Op, just accept that the store skipped
          return ctx.succeededFuture();
        }
      }

      // No-Op, just accept that the store skipped
      return ctx.succeededFuture();

    }

    /**
     * Destroyed Session
     */
    // invalidate the cookie as the session has been destroyed
    final Cookie expiredCookie = context.response().removeCookie(cookieName);
    if (expiredCookie != null) {
      setCookieProperties(expiredCookie, true, context);
    }

    // if the session was regenerated in the request
    // the old id must also be removed
    if (session.isRegenerated()) {
      return sessionStore.delete(session.oldId())
        .compose(delete -> {
          // delete from the storage
          return sessionStore.delete(session.id())
            .onSuccess(delete2 -> context.put(SESSION_WAS_FLUSHED_KEY, true));
        });
    }

    // delete from the storage
    return sessionStore
      .delete(session.id())
      .onSuccess(delete -> context.put(SESSION_WAS_FLUSHED_KEY, true));

  }

  /**
   *
   * @param routingContext - the http routing context
   * @return the cookie name where the session id is stored
   * @throws NotFoundException - if the realm handle key has no value.
   * This is not a fatal error.
   * Because we don't control the setting of the context key.
   * For instance, a callback from oauth will not have information
   * that permits us to set it. In this case, there is no session.
   * The auth callback handler needs to create the session.
   */
  private String getSessionCookieName(RoutingContext routingContext) throws NotFoundException {

    /**
     * Session is at the realm level
     * Meaning that a user can be logged in on 2 differents realms
     * with the same browser
     */
    String realmHandle = routingContext.get(this.realmHandleContextKey);
    if (realmHandle == null) {
      throw new NotFoundException("The realm was not set on the context key " + this.realmHandleContextKey + ". We can't determine the session cookie name.");
    }
    return eraldyDomain.getPrefixName() + "-session-id-" + realmHandle;

  }


  @Override
  public void handle(RoutingContext context) {

    String tempCookieName;
    try {
      tempCookieName = this.getSessionCookieName(context);
    } catch (NotFoundException e) {
      /**
       * See {@link #getFutureCookieNameNotFound()}
       */
      context.next();
      return;
    }
    String cookieName = tempCookieName;

    HttpServerRequest request = context.request();
    if (nagHttps && LOG.isDebugEnabled()) {
      String uri = request.absoluteURI();
      if (!uri.startsWith("https:")) {
        LOG.debug(
          "Using session cookies without https could make you susceptible to session hijacking: " + uri);
      }
    }

    // Look for existing session id
    String sessionID;
    try {
      sessionID = getSessionId(context, cookieName);
    } catch (NotFoundException e) {
      /**
       * session id is unknown
       */
      createNewSession(context, cookieName);
      context.next();
      return;
    }

    if (sessionID.length() < minLength) {
      /**
       * requirment are not met, new session
       */
      createNewSession(context, cookieName);
      context.next();
      return;
    }

    /**
     * Getting the Session is asynchronous,
     * We need to pause the request handling
     * if we want to be able to process it later, during a body handler or protocol upgrade
     */
    if (!context.request().isEnded()) {
      context.request().pause();
    }
    final ContextInternal ctx = (ContextInternal) context.vertx().getOrCreateContext();
    getSession(ctx, sessionID)
      .onFailure(err -> {
        if (!context.request().isEnded()) {
          context.request().resume();
        }
        context.fail(err);
      })
      .onSuccess(session -> {
        if (session != null) {
          ((RoutingContextInternal) context).setSession(session);
          // attempt to load the user from the session
          UserHolder holder = session.get(SESSION_USER_HOLDER_KEY);
          if (holder != null) {
            holder.refresh(context);
          } else {
            // signal we must store the user to link it to the
            // session as it wasn't found
            context.put(SESSION_STORE_USER_KEY, true);
          }
          addStoreSessionHandler(context, cookieName);
        } else {
          // Cannot find session - either it timed out, or was explicitly destroyed at the
          // server side on a
          // previous request.

          // OWASP clearly states that we shouldn't recreate the session as it allows
          // session fixation.
          // create a new anonymous session.
          createNewSession(context, cookieName);
        }
        if (!context.request().isEnded()) {
          context.request().resume();
        }
        context.next();
      });

  }

  @Override
  public Session newSession(RoutingContext context) {
    String cookieName;
    try {
      cookieName = this.getSessionCookieName(context);
    } catch (NotFoundException e) {
      return null;
    }

    Session session = sessionStore.createSession(sessionTimeout, minLength);
    ((RoutingContextInternal) context).setSession(session);

    context.response().removeCookie(cookieName, false);
    // it's a new session we must store the user too otherwise it won't be linked
    context.put(SESSION_STORE_USER_KEY, true);

    flushSessionToStore(context, true, true, cookieName)
      .onFailure(err -> LOG.warn("Failed to flush the session to the underlying store", err));

    return session;
  }

  public Future<Void> setUser(RoutingContext context, User user) {
    String cookieName;
    try {
      cookieName = this.getSessionCookieName(context);
    } catch (NotFoundException e) {
      return Future.succeededFuture();
    }
    context.response().removeCookie(cookieName, false);
    context.setUser(user);
    // signal we must store the user to link it to the session
    context.put(SESSION_STORE_USER_KEY, true);
    return flushSessionToStore(context, true, true, cookieName);
  }

  private String getSessionId(RoutingContext context, String cookieName) throws NotFoundException {

    // only pick the first cookie, when multiple sessions are used:
    // https://www.rfc-editor.org/rfc/rfc6265#section-5.4
    // The user agent SHOULD sort the cookie-list in the following order:
    // Cookies with longer paths are listed before cookies with shorter paths.
    Cookie cookie = context.request().getCookie(cookieName);
    if (cookie == null) {
      throw new NotFoundException();
    }
    return cookie.getValue();


  }

  private Future<Session> getSession(ContextInternal context, String sessionID) {
    return doGetSession(context, System.currentTimeMillis(), sessionID);
  }

  private Future<Session> doGetSession(ContextInternal context, long startTime, String sessionID) {
    return sessionStore.get(sessionID)
      .compose(session -> {
        if (session == null) {
          // no session was found (yet), we will retry as callback to avoid stackoverflow
          final Promise<Session> retry = context.promise();
          doGetSession(context.owner(), startTime, sessionID, retry);
          return retry.future();
        }
        return context.succeededFuture(session);
      });
  }

  private void doGetSession(Vertx vertx, long startTime, String sessionID, Handler<AsyncResult<Session>> resultHandler) {
    sessionStore.get(sessionID)
      .onComplete(res -> {
        if (res.succeeded()) {
          if (res.result() == null) {
            // Can't find it so retry. This is necessary for clustered sessions as it can take sometime for the session
            // to propagate across the cluster so if the next request for the session comes in quickly at a different
            // node there is a possibility it isn't available yet.
            if (System.currentTimeMillis() - startTime < sessionStore.retryTimeout()) {
              vertx.setTimer(5L, v -> doGetSession(vertx, startTime, sessionID, resultHandler));
              return;
            }
          }
        }
        resultHandler.handle(res);
      });
  }


  /**
   * Add a handler for headers (ie done at the end)
   * on the actual context to flush the session
   * data to the store
   *
   * @param context    - the actual context
   * @param cookieName - the session cookie name
   */
  private void addStoreSessionHandler(RoutingContext context, String cookieName) {
    context.addHeadersEndHandler(v -> {
      // skip flush if we already flushed
      Boolean flushed = context.get(SESSION_WAS_FLUSHED_KEY);
      if (flushed == null || !flushed) {
        flushSessionToStore(context, true, false, cookieName)
          .onFailure(err -> LOG.warn("Failed to flush the session to the underlying store", err));
      }
    });
  }

  private void createNewSession(RoutingContext context, String cookieName) {
    Session session = sessionStore.createSession(sessionTimeout, minLength);
    ((RoutingContextInternal) context).setSession(session);
    context.response().removeCookie(cookieName, false);
    // it's a new session we must store the user too otherwise it won't be linked
    context.put(SESSION_STORE_USER_KEY, true);
    addStoreSessionHandler(context, cookieName);
  }

  private Cookie sessionCookie(final RoutingContext context, final Session session, String cookieName) {
    // only pick the first cookie, when multiple sessions are used:
    // https://www.rfc-editor.org/rfc/rfc6265#section-5.4
    // The user agent SHOULD sort the cookie-list in the following order:
    // Cookies with longer paths are listed before cookies with shorter paths.
    Cookie cookie = context.request().getCookie(cookieName);
    if (cookie != null) {
      return cookie;
    }
    cookie = Cookie.cookie(cookieName, session.value());
    setCookieProperties(cookie, false, context);
    context.response().addCookie(cookie);
    return cookie;
  }

  /**
   * Allow cross session cookie
   * (Not really secure because the CSRF token is not available)
   *
   * @param ctx the context
   * @return the domain session handler
   */
  @SuppressWarnings("unused")
  public RealmSessionHandler upgradeSessionCookieToCrossCookie(RoutingContext ctx) {
    ctx.put(SESSION_COOKIE_SAME_SITE_KEY, CookieSameSite.NONE);
    return this;
  }

  /**
   * @param realmHandleContextKey - the context key where to find the realm handle
   *                              it's used to create a unique session cookie id for each realm (one session by realm)
   */
  public RealmSessionHandler setRealmHandleContextKey(String realmHandleContextKey) {
    this.realmHandleContextKey = realmHandleContextKey;
    return this;
  }
}
