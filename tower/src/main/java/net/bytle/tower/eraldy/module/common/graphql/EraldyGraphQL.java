package net.bytle.tower.eraldy.module.common.graphql;

import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
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
import net.bytle.tower.eraldy.model.manual.Status;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.app.graphql.AppGraphQLImpl;
import net.bytle.tower.eraldy.module.auth.graphql.AuthGraphQLImpl;
import net.bytle.tower.eraldy.module.list.graphql.ListGraphQLImpl;
import net.bytle.tower.eraldy.module.mailing.graphql.MailingGraphQLImpl;
import net.bytle.tower.eraldy.module.organization.graphql.OrgaGraphQLImpl;
import net.bytle.tower.eraldy.module.realm.graphql.RealmGraphQLImpl;
import net.bytle.vertx.graphql.GraphQLDef;
import net.bytle.vertx.graphql.GraphQLLocalDate;
import net.bytle.vertx.graphql.scalar.*;
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
     * Wiring Builder
     * (The builder to attach the type to our implementation)
     */
    RuntimeWiring.Builder wiringBuilder = newRuntimeWiring();

    /**
     * Common Scalar
     */
    addCommonTypeAsScalar(wiringBuilder);


    /**
     * Our implementations
     */
    new MailingGraphQLImpl(this, wiringBuilder);
    new ListGraphQLImpl(this, wiringBuilder);
    RealmGraphQLImpl realmGraphQL = new RealmGraphQLImpl(this, wiringBuilder);
    new OrgaGraphQLImpl(this,wiringBuilder);
    new AppGraphQLImpl(this, wiringBuilder);
    new AuthGraphQLImpl(this, wiringBuilder);

    /**
     * Wiring final object
     */
    RuntimeWiring runtimeWiring = wiringBuilder
      .type(
        newTypeWiring("UserI")
          .typeResolver(this::getUserInterfaceTypeResolver)
          .build()
      )
      .type(
        newTypeWiring("Status")
          .dataFetcher("name", this::getStatusName)
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
        DataLoader<String, User> userDataLoader = DataLoaderFactory.newDataLoader(realmGraphQL::batchLoadUsers);
        DataLoaderRegistry userDataLoaderRegistry = new DataLoaderRegistry().register("user", userDataLoader);
        builderWithContext.builder().dataLoaderRegistry(userDataLoaderRegistry);
      })
      .build();
  }

  private static void addCommonTypeAsScalar(RuntimeWiring.Builder wiringBuilder) {
    final GraphQLScalarType EMAIL = GraphQLScalarType
      .newScalar()
      .name("EmailAddress")
      .description("Email Address")
      .coercing(new GraphQLEmailCoercing())
      .build();
    wiringBuilder.scalar(EMAIL);
    final GraphQLScalarType HANDLE = GraphQLScalarType
      .newScalar()
      .name("Handle")
      .description("An unique name identifier")
      .coercing(new GraphQLHandleCoercing())
      .build();
    wiringBuilder.scalar(HANDLE);
    final GraphQLScalarType URI = GraphQLScalarType
      .newScalar()
      .name("Uri")
      .description("An URI")
      .coercing(new GraphQLUriCoercing())
      .build();
    wiringBuilder.scalar(URI);
    final GraphQLScalarType URL = GraphQLScalarType
      .newScalar()
      .name("Url")
      .description("An URL")
      .coercing(new GraphQLUrlCoercing())
      .build();
    wiringBuilder.scalar(URL);
    final GraphQLScalarType TIMEZONE = GraphQLScalarType
      .newScalar()
      .name("TimeZone")
      .description("A timezone")
      .coercing(new GraphQLHandleCoercing())
      .build();
    wiringBuilder.scalar(TIMEZONE);
    final GraphQLScalarType COLOR = GraphQLScalarType
      .newScalar()
      .name("Color")
      .description("A color")
      .coercing(new GraphQLColorCoercing())
      .build();
    wiringBuilder.scalar(COLOR);
  }

  /**
   * By default, GraphQL returns the Enum.name()
   * (ie the name of the enum, not the output of the function getName)
   */
  private Future<String> getStatusName(DataFetchingEnvironment dataFetchingEnvironment) {
    Status status = dataFetchingEnvironment.getSource();
    return Future.succeededFuture(status.getName());
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
    if (javaObject instanceof OrgaUser) {
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
