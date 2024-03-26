package net.bytle.tower.eraldy.graphql;

import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Future;
import io.vertx.ext.web.handler.graphql.instrumentation.VertxFutureAdapter;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Mailing;
import net.bytle.vertx.GraphQLDef;
import net.bytle.vertx.TowerApp;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class EraldyGraphQL implements GraphQLDef {
  private final EraldyApiApp app;
  private final GraphQL graphQL;

  public EraldyGraphQL(EraldyApiApp eraldyApiApp) {
    this.app = eraldyApiApp;

    String schema = eraldyApiApp.getHttpServer().getServer().getVertx()
      .fileSystem().readFileBlocking("graphql/eraldy.graphqls").toString();

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = newRuntimeWiring()
      .type("Query", builder -> {
        // https://vertx.io/docs/vertx-web-graphql/java/#_fetching_data
        return builder.dataFetcher("mailing", this::getMailing);
      })
      .type("Mutation", builder -> builder.dataFetcher("mailing", this::patchMailing))
      .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    /**
     * Return Future (in place of CompletionStage)
     * Instead of converting Vert.x Future to java.util.concurrent.CompletionStage manually
     * for GraphQL Java in every data fetcher implementation,
     * the VertxFutureAdapter instrumentation do it for you and you can return Future
     * https://vertx.io/docs/vertx-web-graphql/java/#_fetching_data
     */
    VertxFutureAdapter instrumentation = VertxFutureAdapter.create();

    /**
     * GraphQL build
     */
    this.graphQL = GraphQL.newGraphQL(graphQLSchema)
      .instrumentation(instrumentation)
      .build();
  }

  private Future<Boolean> patchMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    return Future.succeededFuture(true);
  }

  private Future<Mailing> getMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    Mailing mailing = new Mailing();
    mailing.setGuid(guid);
    return Future.succeededFuture(mailing);
  }


  @Override
  public TowerApp getApp() {
    return this.app;
  }

  @Override
  public GraphQL getGraphQL() {
    return this.graphQL;
  }
}
