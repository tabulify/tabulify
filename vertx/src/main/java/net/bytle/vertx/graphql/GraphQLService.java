package net.bytle.vertx.graphql;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import net.bytle.exception.NullValueException;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.TowerService;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;
import net.bytle.vertx.auth.ApiKeyHandlerRelaxed;

/**
 * A service to implement a GraphQL endpoint
 */
public class GraphQLService extends TowerService {

  private final GraphQLDef graphQL;

  public GraphQLService(GraphQLDef graphQL) {
    super(graphQL.getApp().getHttpServer().getServer());
    this.graphQL = graphQL;
  }

  @Override
  public Future<Void> mount() {


    /**
     * Server
     * (Get and post)
     */
    Route graphQLRoute = this.graphQL.getApp()
      .getHttpServer()
      .getRouter()
      .route("/graphql");

    /**
     * GraphiQL Client
     */
    if (JavaEnvs.IS_DEV) {
      /**
       * Add key authentication
       */
      ApiKeyAuthenticationProvider authProvider;
      try {
        authProvider = this.getServer().getApiKeyAuthProvider();
      } catch (NullValueException e) {
        throw new RuntimeException("ApiKeyAuthProvider is not enabled but required by GraphiQL to authenticate");
      }
      ApiKeyHandlerRelaxed apiKeyHandler = new ApiKeyHandlerRelaxed(authProvider).header(authProvider.getHeader());
      graphQLRoute.handler(apiKeyHandler);

      /**
       * GraphiQL is disabled by default
       */
      boolean enabledGraphiQL = true;
      /**
       * GraphiQL building
       */
      GraphiQLHandlerOptions options = new GraphiQLHandlerOptions().setEnabled(enabledGraphiQL);
      GraphiQLHandler graphiQLHandler = GraphiQLHandler
        .builder(this.getServer().getVertx())
        .with(options)
        .addingHeaders(rc ->
          {
            // no idea how it works but this will add the header to each GraphiQL browser request
            // it's also possible to add it on the GUI Header panel
            // { "x-api-key":"secret" }
            // but if you do that, you get the value "secret, secret"
            return MultiMap.caseInsensitiveMultiMap().add(authProvider.getHeader(), authProvider.getSuperToken());
          })
        .build();
      /**
       * GraphiQL Handler
       */
      this.graphQL
        .getApp()
        .getHttpServer()
        .getRouter()
        .route("/graphiql*")
        .subRouter(graphiQLHandler.router());
    }

    /**
     * We add the GraphQL handler
     * after GraphiQL because
     * the GraphiQL block add conditionally the Api Key Authentication handler
     */
    graphQLRoute.handler(this.graphQL.getHandler());

    return super.mount();
  }
}
