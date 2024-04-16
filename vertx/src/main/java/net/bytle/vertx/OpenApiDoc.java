package net.bytle.vertx;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class OpenApiDoc {

  static Logger LOGGER = LogManager.getLogger(OpenApiDoc.class);

  public static final String DOC_OPERATION_PATH = "/doc";
  private final OpenApiService openApiService;
  private final String routerDirPath;
  private final String routerYamlRootPath;
  private final String routerYamlDocPath;

  public OpenApiDoc(OpenApiService openApiService) {
    String tempLocalPath;
    this.openApiService = openApiService;
    TowerApp towerApp = this.openApiService.getOpenApiInstance().getApp();
    tempLocalPath = towerApp.getPathMount();
    if (tempLocalPath.isEmpty()) {
      tempLocalPath = "/" + towerApp.getAppHandle().toLowerCase();
    }
    String rootPath = tempLocalPath;

    this.routerDirPath = rootPath + DOC_OPERATION_PATH;
    this.routerYamlRootPath = rootPath + OpenApiService.OPENAPI_YAML_PATH;
    this.routerYamlDocPath = routerDirPath + OpenApiService.OPENAPI_YAML_PATH;

  }

  /**
   * Add the doc
   * (virtual static website that process and server the openapi.yml file)
   * <a href="https://vertx.io/docs/vertx-web/java/#_serving_static_resources">...</a>
   */
  public void addHandler(Router router) {


    /**
     * Serve the doc
     */
    StaticHandler staticHandler = StaticResourcesUtil.getStaticHandlerForRelativeResourcePath("openapi-doc");
    String allFilesJavascriptIncluded = "*";
    router.get(routerDirPath + allFilesJavascriptIncluded).handler(staticHandler);
    TowerApp towerApp = this.openApiService.getOpenApiInstance().getApp();
    LOGGER.info("Serving API doc at " + towerApp.getOperationUriForLocalhost(routerDirPath) + " and " + towerApp.getOperationUriForPublicHost(routerDirPath));

    /**
     * Serve the spec (root OpenAPI document) at the root
     * as specified by the [spec](https://spec.openapis.org/oas/v3.1.0#document-structure)
     */
    String resourceOpenApiFile = openApiService.getRelativeOpenApiSpecFileResourcesPath();
    router.get(routerYamlDocPath).handler(ctx -> ctx.response().sendFile(resourceOpenApiFile));
    LOGGER.info("Serving open API file at " + towerApp.getOperationUriForLocalhost(routerYamlDocPath) + " and " + towerApp.getOperationUriForPublicHost(routerYamlDocPath));
    router.get(routerYamlRootPath).handler(ctx -> ctx.response().sendFile(resourceOpenApiFile));
    LOGGER.info("Serving open API file at " + towerApp.getOperationUriForLocalhost(routerYamlRootPath) + " and " + towerApp.getOperationUriForPublicHost(routerYamlRootPath));

  }


}
