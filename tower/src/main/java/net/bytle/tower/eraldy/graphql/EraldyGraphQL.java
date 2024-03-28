package net.bytle.tower.eraldy.graphql;

import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;
import io.vertx.ext.web.handler.graphql.instrumentation.VertxFutureAdapter;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.implementer.MailingGraphQLImpl;
import net.bytle.tower.eraldy.graphql.implementer.UserGraphQLImpl;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.graphql.GraphQLDef;
import net.bytle.vertx.graphql.GraphQLLocalDate;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class EraldyGraphQL implements GraphQLDef {
  private final EraldyApiApp app;
  private final GraphQLHandler graphQLHandler;


  public EraldyGraphQL(EraldyApiApp eraldyApiApp) {
    this.app = eraldyApiApp;

    /**
     * Schema Parsing
     */
    String schema = eraldyApiApp.getHttpServer().getServer().getVertx()
      .fileSystem().readFileBlocking("graphql/eraldy.graphqls").toString();
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    /**
     * Our implementation
     */
    MailingGraphQLImpl mailingImpl = new MailingGraphQLImpl(this);
    UserGraphQLImpl userImpl = new UserGraphQLImpl(this);

    /**
     * Wiring
     * Attach the type to our implementation
     */
    RuntimeWiring runtimeWiring = newRuntimeWiring()
      .type(
        newTypeWiring("Query")
          .dataFetcher("mailing", mailingImpl::getMailing)
          .build()
      )
      .type(
        newTypeWiring("Query")
          .dataFetcher("mailingsOfList", mailingImpl::getMailingsOfList)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("mailingUpdate", mailingImpl::patchMailing)
          .build()
      )
      .type(
        newTypeWiring("Mailing")
          .dataFetcher("emailAuthor", mailingImpl::getMailingEmailAuthor)
          .build()
      )
      .type(
        newTypeWiring("UserI")
          .typeResolver(this::getUserInterfaceTypeResolver)
          .build()
      )
      .directive("dateFormat", new GraphQLLocalDate())
      .build();

    /**
     * Generate the runtime schema
     */
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    /**
     * Add vertx instrumentation to change the behavior of GraphQL
     * (Ie allow Future as returned value and Json as returned raw value (along map and pojo))
     */
    ChainedInstrumentation vertxInstrumentation = getVertxInstrumentation();

    /**
     * GraphQL build
     */
    GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema)
      .instrumentation(vertxInstrumentation)
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
        DataLoader<String, User> userDataLoader = DataLoaderFactory.newDataLoader(userImpl::batchLoadUsers);
        DataLoaderRegistry userDataLoaderRegistry = new DataLoaderRegistry().register("user", userDataLoader);
        builderWithContext.builder().dataLoaderRegistry(userDataLoaderRegistry);
      })
      .build();
  }

  @NotNull
  private static ChainedInstrumentation getVertxInstrumentation() {

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
    return new ChainedInstrumentation(chainedList);

  }

  /**
   * User is an interface
   * We need a resolver then
   * <a href="https://www.graphql-java.com/documentation/schema/#datafetcher-and-typeresolver">...</a>
   */
  private GraphQLObjectType getUserInterfaceTypeResolver(TypeResolutionEnvironment env) {
    Object javaObject = env.getObject();
    if (javaObject instanceof OrganizationUser) {
      return env.getSchema().getObjectType("OrganizationUser");
    }
    return env.getSchema().getObjectType("User");
  }


  @Override
  public EraldyApiApp getApp() {
    return this.app;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return this.graphQLHandler;
  }



}
