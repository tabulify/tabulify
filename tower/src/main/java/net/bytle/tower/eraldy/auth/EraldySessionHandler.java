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
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.vertx.HttpsCertificateUtil;
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
 */
public class EraldySessionHandler implements SessionHandler {

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
   * (On heavy trusted domain, we allow cross-session cookie
   */
  public static final String SESSION_COOKIE_SAME_SITE_KEY = "cs-session-cross-cookie";

  private static final Logger LOG = LoggerFactory.getLogger(EraldySessionHandler.class);
  private static EraldySessionHandler eraldySessionHandler;

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


  public EraldySessionHandler(SessionStore sessionStore, EraldyDomain eraldyDomain) {
    this.sessionStore = sessionStore;
    this.eraldyDomain = eraldyDomain;
  }

  public static EraldySessionHandler createWithDomain(SessionStore sessionStore, EraldyDomain eraldyDomain) {
    if (EraldySessionHandler.eraldySessionHandler == null) {
      EraldySessionHandler.eraldySessionHandler = new EraldySessionHandler(sessionStore, eraldyDomain);
    }
    return EraldySessionHandler.eraldySessionHandler;
  }

  public static EraldySessionHandler get() {
    return eraldySessionHandler;
  }

  /**
   * The amount of time in ms, after which the session will expire, if not accessed.
   * The timeout delete the user on the session if it's not accessed within this time
   * See {@link Session#timeout()}
   * @param timeout the timeout, in ms.
   */
  @Override
  public EraldySessionHandler setSessionTimeout(long timeout) {
    this.sessionTimeout = timeout;
    return this;
  }

  @Override
  public EraldySessionHandler setNagHttps(boolean nag) {
    this.nagHttps = nag;
    return this;
  }

  @Override
  public EraldySessionHandler setCookieSecureFlag(boolean secure) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public EraldySessionHandler setCookieHttpOnlyFlag(boolean httpOnly) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public EraldySessionHandler setSessionCookieName(String sessionCookieName) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public EraldySessionHandler setSessionCookiePath(String sessionCookiePath) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public EraldySessionHandler setMinLength(int minLength) {
    this.minLength = minLength;
    return this;
  }

  @Override
  public EraldySessionHandler setCookieSameSite(CookieSameSite policy) {
    throw new InternalException("Cookie settings are dynamic");
  }

  @Override
  public EraldySessionHandler setLazySession(boolean lazySession) {
    this.lazySession = lazySession;
    return this;
  }

  @Override
  public EraldySessionHandler setCookieMaxAge(long cookieMaxAge) {
    this.cookieMaxAge = cookieMaxAge;
    return this;
  }

  @Override
  public EraldySessionHandler setCookieless(boolean cookieless) {
    /**
     * Cookie less sends the session via HTTP query parameters
     */
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Future<Void> flush(RoutingContext context, boolean ignoreStatus) {
    return flushSessionToStore(context, false, ignoreStatus);
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
     * If the realm of the request is Eraldy,
     * we create a cross domain cookie
     */
    String realmHandle = AuthRealmHandler.getFromRoutingContextKeyStore(context).getHandle();
    if (realmHandle.equals(eraldyDomain.getRealmHandle())) {
      cookie.setDomain(eraldyDomain.getApexNameWithoutPort());
    }

    /**
     * Over Https
     */
    cookie.setSecure(HttpsCertificateUtil.createOrGet().isHttpsEnable());

    if (!expired) {
      // set max age if user requested it - else it's a session cookie
      if (cookieMaxAge >= 0) {
        cookie.setMaxAge(cookieMaxAge);
      }
    }

  }

  /**
   * Store the session (called via the {@link #addStoreSessionHandler(RoutingContext)}
   */
  private Future<Void> flushSessionToStore(RoutingContext context, boolean skipCrc, boolean ignoreStatus) {
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
            session.put(SESSION_USER_HOLDER_KEY, new UserHolder(context));
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
          final Cookie cookie = sessionCookie(context, session);
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
          sessionCookie(context, session);
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
    final Cookie expiredCookie = context.response().removeCookie(this.getSessionCookieName(context));
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

  private String getSessionCookieName(RoutingContext context) {


    String realmHandle = AuthRealmHandler.getFromRoutingContextKeyStore(context).getHandle();
    return eraldyDomain.getPrefixName() + "-session-id" + "-" + realmHandle;

  }


  @Override
  public void handle(RoutingContext context) {


    HttpServerRequest request = context.request();
    if (nagHttps && LOG.isDebugEnabled()) {
      String uri = request.absoluteURI();
      if (!uri.startsWith("https:")) {
        LOG.debug(
          "Using session cookies without https could make you susceptible to session hijacking: " + uri);
      }
    }

    // Look for existing session id
    String sessionID = getSessionId(context);
    if (sessionID != null && sessionID.length() > minLength) {
      // this handler is asynchronous, we need to pause the request
      // if we want to be able to process it later, during a body handler or protocol upgrade
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
            addStoreSessionHandler(context);
          } else {
            // Cannot find session - either it timed out, or was explicitly destroyed at the
            // server side on a
            // previous request.

            // OWASP clearly states that we shouldn't recreate the session as it allows
            // session fixation.
            // create a new anonymous session.
            createNewSession(context);
          }
          if (!context.request().isEnded()) {
            context.request().resume();
          }
          context.next();
        });

      /**
       * Session id known block done
       * Return to avoid the context.next() just below
       * at then end of the function block when the session id is unknown
       */
      return;
    }

    /**
     * session id is unknown
     * requirements were not met, so an anonymous session is created.
     */
    createNewSession(context);
    context.next();

  }

  public Session newSession(RoutingContext context) {
    Session session = sessionStore.createSession(sessionTimeout, minLength);
    ((RoutingContextInternal) context).setSession(session);
    context.response().removeCookie(this.getSessionCookieName(context), false);
    // it's a new session we must store the user too otherwise it won't be linked
    context.put(SESSION_STORE_USER_KEY, true);

    flushSessionToStore(context, true, true)
      .onFailure(err -> LOG.warn("Failed to flush the session to the underlying store", err));

    return session;
  }

  public Future<Void> setUser(RoutingContext context, User user) {
    context.response().removeCookie(this.getSessionCookieName(context), false);
    context.setUser(user);
    // signal we must store the user to link it to the session
    context.put(SESSION_STORE_USER_KEY, true);
    return flushSessionToStore(context, true, true);
  }

  private String getSessionId(RoutingContext context) {

    // only pick the first cookie, when multiple sessions are used:
    // https://www.rfc-editor.org/rfc/rfc6265#section-5.4
    // The user agent SHOULD sort the cookie-list in the following order:
    // Cookies with longer paths are listed before cookies with shorter paths.
    Cookie cookie = context.request().getCookie(this.getSessionCookieName(context));
    if (cookie != null) {
      // Look up sessionId
      return cookie.getValue();
    }

    return null;

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
   * @param context - the actual context
   */
  private void addStoreSessionHandler(RoutingContext context) {
    context.addHeadersEndHandler(v -> {
      // skip flush if we already flushed
      Boolean flushed = context.get(SESSION_WAS_FLUSHED_KEY);
      if (flushed == null || !flushed) {
        flushSessionToStore(context, true, false)
          .onFailure(err -> LOG.warn("Failed to flush the session to the underlying store", err));
      }
    });
  }

  private void createNewSession(RoutingContext context) {
    Session session = sessionStore.createSession(sessionTimeout, minLength);
    ((RoutingContextInternal) context).setSession(session);
    context.response().removeCookie(this.getSessionCookieName(context), false);
    // it's a new session we must store the user too otherwise it won't be linked
    context.put(SESSION_STORE_USER_KEY, true);
    addStoreSessionHandler(context);
  }

  private Cookie sessionCookie(final RoutingContext context, final Session session) {
    // only pick the first cookie, when multiple sessions are used:
    // https://www.rfc-editor.org/rfc/rfc6265#section-5.4
    // The user agent SHOULD sort the cookie-list in the following order:
    // Cookies with longer paths are listed before cookies with shorter paths.
    String sessionCookieName = getSessionCookieName(context);
    Cookie cookie = context.request().getCookie(sessionCookieName);
    if (cookie != null) {
      return cookie;
    }
    cookie = Cookie.cookie(sessionCookieName, session.value());
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
  public EraldySessionHandler upgradeSessionCookieToCrossCookie(RoutingContext ctx) {
    ctx.put(SESSION_COOKIE_SAME_SITE_KEY, CookieSameSite.NONE);
    return this;
  }

}
