package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.Log;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.CspPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.CspObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The logs are done with log4j
 * <p>
 * This handler will receive CSP report
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy-Report-Only">...</a>
 * and log them
 */
public class CspPublicapiImpl implements CspPublicapi {


  /**
   * The name of the logger
   */
  public static final String LOGGER_NAME = "csp";
  /**
   * The path of the url
   */
  public static final String REPORT_ENDPOINT = "/" + LOGGER_NAME+"/"+"report";
  /**
   * Where the log files should be
   */
  public static final Path LOG_DIR_PATH = Paths.get(Log.LOG_DIR_NAME, LOGGER_NAME);

  /**
   * The log files
   */
  public static final Path LOG_FILE_PATH = LOG_DIR_PATH.resolve(LOGGER_NAME + ".jsonl");

  private final Logger logger = LogManager.getLogger(this.getClass());



  /**
   * No singleton because we may start several Http Server Verticle
   * for test purpose
   * Use the below constructor please
   */
  public CspPublicapiImpl() {
  }


  @Override
  public Future<ApiResponse<Void>> cspReportPost(RoutingContext routingContext, CspObject cspObject) {


    String msg = routingContext.body().asString();
    logger.info(msg);

    ApiResponse<Void> ipInfoApiResponse = new ApiResponse<>(200);
    return Future.succeededFuture(ipInfoApiResponse);

  }
}
