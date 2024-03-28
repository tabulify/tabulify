package net.bytle.vertx.graphql;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import net.bytle.exception.NullValueException;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.TowerService;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;

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

    this.graphQL.getApp()
      .getHttpServer()
      .getRouter()
      .route("/graphql")
      .handler(this.graphQL.getHandler());

    /**
     * GraphiQL Client
     * GraphiQL is disabled by default
     * and enabled automatically when Vert.x Web runs in development mode
     * with Root access.
     */
    if (JavaEnvs.IS_DEV) {
      GraphiQLHandlerOptions options = new GraphiQLHandlerOptions()
        .setEnabled(true);
      GraphiQLHandler handler = GraphiQLHandler.builder(this.graphQL.getApp().getHttpServer().getServer().getVertx())
        .with(options)
        .addingHeaders(rc -> {
          // Authenticate as Root
          ApiKeyAuthenticationProvider authProvider;
          try {
            authProvider = this.getServer().getApiKeyAuthProvider();
          } catch (NullValueException e) {
            throw new RuntimeException("ApiKeyAuthProvider is not enabled but required by GraphiQL to authenticate");
          }
          return MultiMap.caseInsensitiveMultiMap().add(authProvider.getHeader(), authProvider.getSuperToken());
        })
        .build();
      this.graphQL
        .getApp()
        .getHttpServer()
        .getRouter()
        .route("/graphiql*")
        .subRouter(handler.router());
    }

    return super.mount();
  }
}
