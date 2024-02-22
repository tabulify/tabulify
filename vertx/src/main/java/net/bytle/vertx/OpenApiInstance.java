package net.bytle.vertx;

import io.vertx.ext.web.openapi.RouterBuilder;

public interface OpenApiInstance {


  /**
   * Mount all openApi operations described in the spec
   *
   * @param builder - the open api builder
   */
  OpenApiInstance openApiMount(RouterBuilder builder);

  /**
   * Bind a security key to a handler
   * @param routerBuilder - the router builder
   * @param openApiService - the open api service
   * To add security handlers for the openApi Security handler
   * Configuring `AuthenticationHandler`s defined in the OpenAPI document
   * <a href="https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document">...</a>
   */
  OpenApiInstance openApiAddSecurityHandlers(RouterBuilder routerBuilder, OpenApiService openApiService);

  /**
   * If security handlers are required when mounting
   * If true, the {@link #openApiAddSecurityHandlers(RouterBuilder, OpenApiService)}
   * is called to bind them
   */
  boolean requireSecurityHandlers();

  TowerApp getApp();
}
