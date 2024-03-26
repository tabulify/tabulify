package net.bytle.tower.eraldy.graphql;

import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;
import io.vertx.ext.web.handler.graphql.instrumentation.VertxFutureAdapter;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Mailing;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.GraphQLDef;
import net.bytle.vertx.TowerApp;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;

import java.util.ArrayList;
import java.util.List;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class EraldyGraphQL implements GraphQLDef {
  private final EraldyApiApp app;
  private final GraphQLHandler graphQLHandler;


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
      .type("Mailing", typeWiring -> typeWiring.dataFetcher("emailAuthor", this::getMailingEmailAuthor))
      .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    /**
     * Allow to return Future (in place of a CompletionStage) in a data fetcher
     * https://vertx.io/docs/vertx-web-graphql/java/#_fetching_data
     */
    VertxFutureAdapter completableToFuture = VertxFutureAdapter.create();

    /**
     * Allow to return a JSON Vertx object as result (ie map)
     * https://vertx.io/docs/vertx-web-graphql/java/#_json_data_results
     */
    JsonObjectAdapter jsonObjectAdapter = new JsonObjectAdapter();

    /**
     * Instrumentation must be chained
     * otherwise, the data may be null for field
     */
    List<Instrumentation> chainedList = new ArrayList<>();
    chainedList.add(completableToFuture);
    chainedList.add(jsonObjectAdapter);
    ChainedInstrumentation chainedInstrumentation = new ChainedInstrumentation(chainedList);

    /**
     * GraphQL build
     */
    GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema)
      .instrumentation(chainedInstrumentation)
      .build();

    /**
     * Handler
     */
    this.graphQLHandler = GraphQLHandler
      .builder(graphQL)
      .beforeExecute(builderWithContext -> {
        /**
         * Batch the get of users by guid
         * The below code creates the User DataLoaderRegistry for each request
         * (The data fetchers call the register and register a Guid.
         * At the end, the data loader function implementation is called with the list of guid.
         * Configuration: https://vertx.io/docs/vertx-web-graphql/java/#_batch_loading
         * Usage: https://www.graphql-java.com/documentation/batching
         * In a fetcher:
         * String guid = environment.getArgument("guid");
         * DataLoader<String, Object> userLoader = environment.getDataLoader("userLoader");
         * userLoader.load(guid);
         * The data loader function `usersBatchLoader` is then called at the end with all guids.
         */
        DataLoader<String, User> userDataLoader = DataLoaderFactory.newDataLoader(usersBatchLoader);
        DataLoaderRegistry userDataLoaderRegistry = new DataLoaderRegistry().register("user", userDataLoader);
        builderWithContext.builder().dataLoaderRegistry(userDataLoaderRegistry);
      })
      .build();
  }

  private Future<User> getMailingEmailAuthor(DataFetchingEnvironment dataFetchingEnvironment) {
    User user = new User();
    user.setGuid("123");
    return Future.succeededFuture(user);
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
  public Handler<RoutingContext> getHandler() {
    return this.graphQLHandler;
  }


  /**
   * Minimal example that returns empty users
   */
  BatchLoaderWithContext<String, User> usersBatchLoader = (ids, env) -> {
    // A list of ids and returns a CompletionStage for a list of users
    Future<List<User>> future = Future.succeededFuture(new ArrayList<>());
    return future.toCompletionStage();
  };

}
