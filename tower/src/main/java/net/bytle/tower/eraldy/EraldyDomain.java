package net.bytle.tower.eraldy;

import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.SessionHandler;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.app.comboapp.ComboAppApp;
import net.bytle.tower.eraldy.app.comboprivateapi.ComboPrivateApiApp;
import net.bytle.tower.eraldy.app.combopublicapi.ComboPublicApiApp;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.auth.EraldySessionHandler;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.eraldy.schedule.SqlAnalytics;
import net.bytle.tower.util.BrowserCorsUtil;
import net.bytle.tower.util.Env;
import net.bytle.tower.util.PersistentLocalSessionStore;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.HttpServer;
import net.bytle.vertx.TowerApexDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * This class represents the eraldy domain
 */
public class EraldyDomain extends TowerApexDomain {

  protected static final Logger LOGGER = LoggerFactory.getLogger(EraldyDomain.class);

  private static final String NAME = "eraldy";
  private static final String DEFAULT_VHOST = "eraldy.com";
  private static final String COMBO_APEX_DOMAIN_CONFIG_KEY = "eraldy.apex.domain";
  private static final long ERALDY_ORGANIZATION_ID = 1L;
  private static final long ERALDY_REALM_ID = ERALDY_ORGANIZATION_ID;
  private static final String REALM_HANDLE = "eraldy";
  private static final String REALM_NAME = "Eraldy";

  private static final String USER_OWNER_NAME = "Nico";
  private static final String USER_OWNER_EMAIL = "nico@eraldy.com";


  private static EraldyDomain eraldyDomain;
  private Realm realm;
  private User ownerUser;

  public EraldyDomain(String publicHost, HttpServer httpServer) {
    super(publicHost, httpServer);
  }

  public static EraldyDomain getOrCreate(HttpServer httpServer, ConfigAccessor configAccessor) {
    if (eraldyDomain != null) {
      return eraldyDomain;
    }
    String publicHost = configAccessor.getString(COMBO_APEX_DOMAIN_CONFIG_KEY, DEFAULT_VHOST);
    eraldyDomain = new EraldyDomain(publicHost, httpServer);
    return eraldyDomain;
  }

  public static EraldyDomain get() {
    if (eraldyDomain == null) {
      throw new InternalException("The Eraldy domain should have been build");
    }
    return eraldyDomain;
  }

  public Realm getEraldyRealm() {
    return realm;
  }


  @Override
  public String getPrefixName() {
    return "ey";
  }

  @Override
  public String getPathName() {
    return NAME;
  }

  @Override
  public String getRealmHandle() {
    return realm.getHandle();
  }


  public List<Future<?>> mount() {

    /**
     * Update the Eraldy realm
     * Note: The eraldy realm already exists thanks to the database migration
     */
    Organization organization = new Organization();
    organization.setId(ERALDY_ORGANIZATION_ID);
    realm = new Realm();
    realm.setHandle(REALM_HANDLE);
    realm.setName(REALM_NAME);
    realm.setLocalId(ERALDY_REALM_ID);
    realm.setOrganization(organization);
    // TODO: create the user and role on organization level
    ownerUser = new User();
    ownerUser.setName(USER_OWNER_NAME);
    ownerUser.setEmail(USER_OWNER_EMAIL);
    Realm clone = JsonObject.mapFrom(realm).mapTo(Realm.class);
    ownerUser.setRealm(clone); // to avoid recursion on com.fasterxml.jackson.databind
    try {
      ownerUser.setAvatar(new URI("https://2.gravatar.com/avatar/cbc56a3848d90024bdc76629a1cfc1d9"));
    } catch (URISyntaxException e) {
      throw new InternalException("The eraldy owner URL is not valid", e);
    }
    Future<Realm> realmFuture = UserProvider.createFrom(this.getVertx())
      .upsertUser(ownerUser)
      .onFailure(t -> {
        throw new InternalException("Error while creating the eraldy owner realm", t);
      })
      .compose(ownerDb -> {
        realm.setOwnerUser(ownerDb);
        return RealmProvider.createFrom(this.getVertx())
          .upsertRealm(realm)
          .onFailure(t -> {
              throw new InternalException("Error while creating the eraldy realm", t);
            }
          );
      })
      .compose(realmCompo -> {
        /**
         * To update the guid
         */
        realm = realmCompo;
        return Future.succeededFuture(realmCompo);
      });

    Router rootRouter = this.getHttpServer().getRouter();
    /**
     * Realm of the request
     */
    AuthRealmHandler.createFrom(rootRouter, this);

    /**
     * Allow Browser cross-origin request in the domain
     */
    BrowserCorsUtil.allowCorsForApexDomain(rootRouter, this);

    /**
     * Add the session handler cross domain, cross realm
     */
    this.addBrowserSessionHandler(rootRouter);


    /**
     * Add the apps
     */
    Future<Void> privateApiFuture = ComboPrivateApiApp.create(this)
      .addToRouter(rootRouter);
    Future<Void> publicApiFuture = ComboPublicApiApp.create(this)
      .addToRouter(rootRouter);
    Future<Void> memberApiFuture = EraldyMemberApp.create(this)
      .addToRouter(rootRouter);
    Future<Void> appAppFuture = ComboAppApp.create(this)
      .addToRouter(rootRouter);

    /**
     * Add the scheduled task
     */
    SqlAnalytics.create(this);


    return Lists.newArrayList(privateApiFuture, publicApiFuture, memberApiFuture, appAppFuture, realmFuture);

  }

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
  private void addBrowserSessionHandler(Router rootRouter) {


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
    if (Env.IS_DEV) {
      syncInterval = PersistentLocalSessionStore.INTERVAL_5_SEC;
    }
    PersistentLocalSessionStore sessionStore = PersistentLocalSessionStore
      .create(this.getVertx(), syncInterval);
    /**
     * Reconnect once every
     */
    int cookieMaxAgeOneWeekInSec = 60 * 60 * 24 * 7;
    /**
     * Delete the session if not accessed within this timeout
     */
    int idleSessionTimeoutMs = cookieMaxAgeOneWeekInSec * 1000;
    SessionHandler requestHandler = EraldySessionHandler
      .createWithDomain(sessionStore, this)
      .setSessionTimeout(idleSessionTimeoutMs)
      .setCookieMaxAge(cookieMaxAgeOneWeekInSec);

    String routePath = this.getAbsoluteLocalPath() + "/*";
    rootRouter.route(routePath).handler(requestHandler);

  }


  public boolean isEraldyUser(User user) {
    return user.getRealm().getLocalId().equals(this.getEraldyRealm().getLocalId());
  }

  public void assertIsEraldyUser(User user) {
    EraldyDomain eraldyDomain = EraldyDomain.get();
    boolean isEraldyUser = eraldyDomain.isEraldyUser(user);
    if (!isEraldyUser) {
      throw new IllegalArgumentException("This is not a " + eraldyDomain.getEraldyRealm().getHandle() + " user");
    }
  }

}
