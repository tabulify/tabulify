package net.bytle.vertx;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class OpenApiDoc {

  static Logger LOGGER = LogManager.getLogger(OpenApiDoc.class);

  public static final String DOC_OPERATION_PATH = "/doc";

  /**
   * Add the doc
   * (virtual static website that process and server the openapi.yml file)
   * <a href="https://vertx.io/docs/vertx-web/java/#_serving_static_resources">...</a>
   */
  public static void addHandler(Router router, TowerApp towerApp) {


    String localPath = towerApp.getPathMount();
    if (localPath.equals("")) {
      localPath = "/" + towerApp.getAppName();
    }
    String docLocalPath = localPath + DOC_OPERATION_PATH;

    /**
     * Serve the doc
     */
    StaticHandler staticHandler = StaticResourcesUtil.getStaticHandlerForRelativeResourcePath("openapi-doc");
    String allFilesJavascriptIncluded = "*";
    router.get(docLocalPath + allFilesJavascriptIncluded).handler(staticHandler);
    LOGGER.info("Serving API doc at " + towerApp.getOperationUriForLocalhost(docLocalPath) + " and " + towerApp.getOperationUriForPublicHost(docLocalPath));

    /**
     * Serve the spec (root OpenAPI document) at the root
     * as specified by the [spec](https://spec.openapis.org/oas/v3.1.0#document-structure)
     */
    String resourceOpenApiFile = towerApp.getRelativeSpecFileResourcesPath();
    router.get(docLocalPath + TowerApp.OPENAPI_YAML_PATH).handler(ctx -> ctx.response().sendFile(resourceOpenApiFile));
    LOGGER.info("Serving open API file at " + towerApp.getOperationUriForLocalhost(docLocalPath + TowerApp.OPENAPI_YAML_PATH) + " and " + towerApp.getOperationUriForPublicHost(docLocalPath + TowerApp.OPENAPI_YAML_PATH));
    router.get(localPath + TowerApp.OPENAPI_YAML_PATH).handler(ctx -> ctx.response().sendFile(resourceOpenApiFile));
    LOGGER.info("Serving open API file at " + towerApp.getOperationUriForLocalhost(localPath + TowerApp.OPENAPI_YAML_PATH) + " and " + towerApp.getOperationUriForPublicHost(localPath + TowerApp.OPENAPI_YAML_PATH));

  }

}
