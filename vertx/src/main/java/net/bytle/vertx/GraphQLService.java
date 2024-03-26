package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import net.bytle.java.JavaEnvs;

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
     * and enabled automatically when Vert.x Web runs in development mode.
     * ie with the VERTXWEB_ENVIRONMENT environment variable
     * or vertxweb.environment system property,set it to dev.
     */
    if (JavaEnvs.IS_DEV) {
      GraphiQLHandlerOptions options = new GraphiQLHandlerOptions()
        .setEnabled(true);
      GraphiQLHandler handler = GraphiQLHandler.builder(this.graphQL.getApp().getHttpServer().getServer().getVertx())
        .with(options)
        .addingHeaders(rc -> {
          // if protected by authentication, headers customization
          String token = rc.get("token");
          return MultiMap.caseInsensitiveMultiMap().add("Authorization", "Bearer " + token);
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
