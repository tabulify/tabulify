package net.bytle.tower.eraldy.api;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.Operation;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.auth.ApiKeyAndSessionUserAuthenticationHandler;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;

public class EraldyOpenApi implements OpenApiInstance {

  private final ApiKeyAuthenticationProvider apiKeyUserProvider;
  private final EraldyApiApp apiApp;

  public EraldyOpenApi(EraldyApiApp towerApp) throws ConfigIllegalException {
    /**
     * Add the API Key authentication handler
     * on the router to fill the user in the context
     * as Api Key is supported
     */
    try {
      this.apiKeyUserProvider = towerApp.getHttpServer().getServer().getApiKeyAuthProvider();
    } catch (NullValueException e) {
      throw new ConfigIllegalException("Api Key should be enabled", e);
    }
    this.apiApp = towerApp;
  }

  @Override
  public EraldyOpenApi openApiMount(RouterBuilder builder) {
    ApiVertxSupport.mount(builder, apiApp);
    return this;
  }

  @Override
  public EraldyOpenApi openApiAddSecurityHandlers(RouterBuilder routerBuilder, OpenApiService openApiService) {

    /**
     * We trick the open api security scheme apiKey define in the openapi file
     * to support a cookie authentication by realm
     * This scheme below is implemented by
     * the {@link ApiAuthenticationHandler} that just check if the user is on the vertx context.
     * <p>
     * We to add the needed Authentication handler to fill the user
     * {@link #mountSessionHandlers()}
     */
    routerBuilder
      .securityHandler(OpenApiSecurityNames.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(jsonObject -> {
        String type = jsonObject.getString("type");
        if (!type.equals("apiKey")) {
          throw new InternalException("The security scheme type should be apiKey, not " + type);
        }
        String in = jsonObject.getString("in");
        if (!in.equals("header")) {
          throw new InternalException("The security scheme in should be a header, not " + in);
        }
        String headerName = jsonObject.getString("name");
        return new ApiKeyAndSessionUserAuthenticationHandler(apiApp,headerName, apiKeyUserProvider);
      });

    Handler<RoutingContext> authorizationHandler = openApiService.authorizationCheckHandler();
    for (Operation operation : routerBuilder.operations()) {
      routerBuilder.operation(operation.getOperationId())
        .handler(authorizationHandler);
    }
    return this;

  }

  @Override
  public boolean requireSecurityHandlers() {
    return true;
  }

  @Override
  public TowerApp getApp() {
    return this.apiApp;
  }


}
