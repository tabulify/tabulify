package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.Log;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.AnalyticsPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.AnalyticsEvent;
import net.bytle.tower.util.AnalyticsLogger;

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
public class AnalyticsPublicapiImpl implements AnalyticsPublicapi {




  // Static field
  public static final String ANALYTICS_NAME = "analytics";
  public static final String ANALYTICS_ENDPOINT = "/" + ANALYTICS_NAME + "/event";
  public static final Path ANALYTICS_LOG_PATH = Paths.get(Log.LOG_DIR_NAME, ANALYTICS_NAME);


  @Override
  public Future<ApiResponse<Void>> analyticsEventPost(RoutingContext routingContext, Map<String, Object> requestBody) {

    AnalyticsEvent analyticsEvent = JsonObject.mapFrom(requestBody).mapTo(AnalyticsEvent.class);
    AnalyticsLogger.log(analyticsEvent, routingContext);
    return Future.succeededFuture(new ApiResponse<>());

  }


}
