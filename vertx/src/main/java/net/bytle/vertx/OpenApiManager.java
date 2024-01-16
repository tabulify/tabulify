package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import io.vertx.ext.web.openapi.impl.OpenAPI3RouterBuilderImpl;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * An entry point for all OpenApi related code
 */
public class OpenApiManager {


  private static final Logger LOGGER = LogManager.getLogger(OpenApiManager.class);

  /**
   * The key in the {@link io.vertx.ext.web.RoutingContext#get(String)} to get access
   * to the operations properties on the routing context
   */
  private static final String CONTEXT_OPERATION_MODEL_KEY = "openApiOperationModel";
  private URI specFile;
  private TowerApp towerApp;

  public static OpenApiManager.config config(TowerApp towerApp) {
    return new OpenApiManager.config(towerApp);
  }

  /**
   * @param routingContext - the routing context
   * @return the Json Object of the operation path in the openapi spec
   */
  public JsonObject getOperationModel(RoutingContext routingContext) {
    return routingContext.get(CONTEXT_OPERATION_MODEL_KEY);
  }

  public Future<Void> mountOpenApi(TowerApp towerApp, Router rootRouter) {

    /**
     * Add the serving of the doc
     */
    OpenApiDoc.addHandler(rootRouter, towerApp);

    /**
     * Note: a router should not be shared between verticles.
     * https://vertx.io/docs/vertx-web-openapi/java/
     */
    String specFileString = specFile.toString();
    return RouterBuilder.create(towerApp.getApexDomain().getHttpServer().getServer().getVertx(), specFileString)
      .recover(err -> {
        InternalError error = new InternalError("Unable to build the openapi memory model for the spec file (" + specFileString + "). Check the inputScope to see where the error is.", err);
        return Future.failedFuture(TowerFailureException.builder()
          .setCauseException(error)
          .build()
        );
      })
      .compose(routerBuilder -> {

        /**
         * The order of the build of the router is important:
         * - bodyHandler, first
         * - securityHandler, second
         * - then operation mount (platform), third
         */

        /**
         * Custom Body Handler
         * (Open Api adds a default body handler
         * if there is no global handler (ie {@link RouterBuilder#rootHandler(Handler)}) defined
         * We add therefore ours to disable this behaviour
         * See {@link OpenAPI3RouterBuilderImpl#createRouter()}
         */
        try {
          BodyHandler bodyHandler = towerApp.getApexDomain().getHttpServer().getBodyHandler();
          routerBuilder.rootHandler(bodyHandler);
        } catch (NotFoundException e) {
          // default body handler of openapi is used
        }

        try {
          towerApp
            .openApiBindSecurityScheme(routerBuilder, towerApp.getApexDomain().getHttpServer().getServer().getConfigAccessor())
            .openApiMount(routerBuilder);
        } catch (IllegalConfiguration e) {
          return Future.failedFuture(e);
        }

        routerBuilder.setOptions(
          new RouterBuilderOptions()
            .setRequireSecurityHandlers(true)
            .setOperationModelKey(CONTEXT_OPERATION_MODEL_KEY)
        );

        /**
         * Mount all routes of the openapi specification
         * <p>
         * ***At the end***
         * To have the other directive taking place
         * before (otherwise, for instance, there would be no security handler
         * taking place such as the CSRF token)
         */

        Router openApiRouter;
        try {
          openApiRouter = routerBuilder
            .createRouter();
        } catch (Exception e) {
          return Future.failedFuture(
            TowerFailureException
              .builder()
              .setMessage("Error while building the openapi router for the app (" + this.towerApp + ")")
              .setCauseException(e)
              .build()
          );
        }
        /**
         * Sub-router tip from https://vertx.io/docs/vertx-web-openapi/java/#_generate_the_router
         */
        String localhostAbsolutePathMount = towerApp.getPathMount();
        if (!localhostAbsolutePathMount.isEmpty() && !localhostAbsolutePathMount.equals("/")) {
          String apiAbsolutePath = localhostAbsolutePathMount + "/*";
          rootRouter.route(apiAbsolutePath)
            .subRouter(openApiRouter);
        } else {
          rootRouter.route().subRouter(openApiRouter);
        }

        return Future.succeededFuture();

      });
  }

  public Set<String> getScopes(RoutingContext routingContext) {
    JsonObject operationJsonOpenApiData = getOperationModel(routingContext);
    String operationId = operationJsonOpenApiData.getString("operationId");
    Object securities = operationJsonOpenApiData.getMap().get("security");
    if (securities == null) {
      throw new InternalException("The OpenApi security was not found for the operation id (" + operationId + ")");
    }
    Set<String> scopes = new HashSet<>();
    if (!(securities instanceof JsonArray)) {
      throw new InternalException("The OpenApi security scopes should be in a array/list format for the operation id (" + operationId + ")");
    }
    JsonArray arraySecurities = (JsonArray) securities;
    for (int i = 0; i < arraySecurities.size(); i++) {
      JsonObject security = arraySecurities.getJsonObject(i);
      for (String securityKey : security.getMap().keySet()) {
        JsonArray securityScopes = security.getJsonArray(securityKey);
        for (int j = 0; j < securityScopes.size(); j++) {
          scopes.add(securityScopes.getString(j));
        }
      }
    }
    return scopes;

  }


  public static class config {
    private final TowerApp towerApp;

    public config(TowerApp towerApp) {

      this.towerApp = towerApp;

    }

    /**
     * Note that the openapi.yaml files may be generated by the OpenApi Generator
     * The source files are known as xxxx-openapi.yaml
     */
    public OpenApiManager build() {

      /**
       * Note that the openapi.yaml files may be generated by the OpenApi Generator
       * The source files are known as combo-xxxx-openapi.yaml
       */
      String resourceSpecFile = this.towerApp.getRelativeSpecFileResourcesPath();

      /**
       * The run in dev is not via a Jar archive
       */
      String specFilePathInDev = "src/main/resources/" + resourceSpecFile;
      Path mainSpecFile = Paths.get(specFilePathInDev);
      if (Files.exists(mainSpecFile)) {
        resourceSpecFile = specFilePathInDev;
      }

      OpenApiManager openApiManager = new OpenApiManager();
      openApiManager.specFile = URI.create(resourceSpecFile);
      openApiManager.towerApp = towerApp;
      return openApiManager;

    }
  }

  public Handler<RoutingContext> authorizationCheckHandler() {

    OpenApiManager openApi = this;
    return routingContext -> {
      Set<String> scopes = openApi.getScopes(routingContext);

      for (String scope : scopes) {
        User user = routingContext.user();
        if (!RoleBasedAuthorization.create(scope).match(user)) {
          TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
            .setMessage("The scope (" + scope + ") is not granted to the authenticated user (" + user.subject() + ")")
            .setMimeToJson()
            .buildWithContextFailingTerminal(routingContext);
          return;
        }
      }
      routingContext.next();

    };

  }
}
