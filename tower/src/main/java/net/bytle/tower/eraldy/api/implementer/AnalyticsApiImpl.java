package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.Log;
import net.bytle.tower.eraldy.api.openapi.interfaces.AnalyticsApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.AnalyticsEvent;
import net.bytle.vertx.AnalyticsLogger;
import net.bytle.vertx.TowerApp;

import java.nio.file.Path;
import java.nio.file.Paths;
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

  @SuppressWarnings("unused")
  public AnalyticsApiImpl(TowerApp towerApp) {
  }


  @Override
  public Future<ApiResponse<Void>> analyticsEventPost(RoutingContext routingContext, Map<String, Object> requestBody) {

    AnalyticsEvent analyticsEvent = JsonObject.mapFrom(requestBody).mapTo(AnalyticsEvent.class);
    Realm authRealm = AuthRealmHandler.getFromRoutingContextKeyStore(routingContext);
    AnalyticsLogger.log(analyticsEvent, routingContext, authRealm.getGuid());
    return Future.succeededFuture(new ApiResponse<>());

  }


}
