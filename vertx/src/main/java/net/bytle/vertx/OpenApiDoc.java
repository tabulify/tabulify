package net.bytle.vertx;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;


public class OpenApiDoc {


  public static final String DOC_OPERATION_PATH = "/doc";

  /**
   * Add the doc
   * (virtual static website that process and server the openapi.yml file)
   * <a href="https://vertx.io/docs/vertx-web/java/#_serving_static_resources">...</a>
   */
  public static void addHandler(Router router, TowerApp towerApp) {


    String localPath = towerApp.getAbsoluteLocalPathWithDomain();
    String docLocalPath = localPath + DOC_OPERATION_PATH;

    /**
     * Serve the doc
     */
    StaticHandler staticHandler = StaticResourcesUtil.getStaticHandlerForRelativeResourcePath("openapi-doc");
    String allFilesJavascriptIncluded = "*";
    router.get(docLocalPath + allFilesJavascriptIncluded).handler(staticHandler);

    /**
     * Serve the spec (root OpenAPI document) at the root
     * as specified by the [spec](https://spec.openapis.org/oas/v3.1.0#document-structure)
     */
    String resourceOpenApiFile = towerApp.getRelativeSpecFileResourcesPath();
    router.get(docLocalPath + TowerApp.OPENAPI_YAML_PATH).handler(ctx -> ctx.response().sendFile(resourceOpenApiFile));
    router.get(localPath + TowerApp.OPENAPI_YAML_PATH).handler(ctx -> ctx.response().sendFile(resourceOpenApiFile));


  }

}
