package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.Log;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AnalyticsApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.analytics.model.AnalyticsEvent;
import net.bytle.vertx.analytics.model.AnalyticsEventState;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * The analytics event log are done with log4j for historical reason.
 * <p>
 * We use the <a href="http://logging.apache.org/log4j/2.x/manual/appenders.html#RoutingAppender">Routes</a>
 * in the log4j2
 * <p>
 * We could also add them in postgress, see
 * <a href="https://docs.fluentbit.io/manual/pipeline/outputs/postgresql">...</a>
 */
public class AnalyticsApiImpl implements AnalyticsApi {


  // Static field
  public static final String ANALYTICS_NAME = "analytics";
  public static final String ANALYTICS_ENDPOINT = "/" + ANALYTICS_NAME + "/event";
  public static final Path ANALYTICS_LOG_PATH = Paths.get(Log.LOG_DIR_NAME, ANALYTICS_NAME);
  private final EraldyApiApp apiApp;


  public AnalyticsApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }


  @Override
  public Future<ApiResponse<List<AnalyticsEvent>>> analyticsEventNameGet(RoutingContext routingContext, String eventName, String realmIdentifier, String appIdentifier) {

    return Future.failedFuture("To do");
//      return this.apiApp.getRealmProvider()
//        .getRealmFromIdentifierNotNull(realmIdentifier, Realm.class)
//        .compose(realm -> this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthScope.ANALYTICS_EVENT_GET)
//          .compose(realm1 -> apiApp.getUserProvider()
//            .getRecentUsersCreatedFromRealm(realm)
//            .compose(users -> Future.succeededFuture(new ApiResponse<>(users).setMapper(apiApp.getUserProvider().getApiMapper())))
//          ));


  }

  @Override
  public Future<ApiResponse<Void>> analyticsEventPost(RoutingContext routingContext, Map<String, Object> requestBody) {

    AnalyticsEvent analyticsEvent = JsonObject.mapFrom(requestBody).mapTo(AnalyticsEvent.class);

    /**
     * Reception Tiem
     */
    AnalyticsEventState state = analyticsEvent.getState();
    if (state == null) {
      state = new AnalyticsEventState();
      analyticsEvent.setState(state);
    }
    state.setEventReceptionTime(DateTimeUtil.getNowInUtc());

    /**
     * We don't path the Realm Object but the realm id and organization id
     * because Analytics Tracker does not know the realm object (only the ids)
     * as it's independent of the Eraldy Api App
     */
    Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(routingContext);
    String organizationId = authRealm.getOrganization().getGuid();
    String realmId = authRealm.getGuid();

    /**
     * Pass the event
     */
    this.apiApp.getApexDomain().getHttpServer().getServer().getTrackerAnalytics()
      .eventBuilder(analyticsEvent)
      .setRoutingContext(routingContext)
      .setOrganizationId(organizationId)
      .setRealmId(realmId)
      .addToDeliveryQueue();
    return Future.succeededFuture(new ApiResponse<>());

  }


}
