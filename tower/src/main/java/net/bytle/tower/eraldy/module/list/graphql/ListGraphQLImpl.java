package net.bytle.tower.eraldy.module.list.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.module.list.inputs.ListInputProps;

import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class ListGraphQLImpl {


  private final ListProvider listProvider;

  public ListGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder typeWiringBuilder) {

    this.listProvider = eraldyGraphQL.getApp().getListProvider();

    /**
     * Map type to function
     */
    typeWiringBuilder
      /**
       * Query
       */
      .type(
        newTypeWiring("Query")
          .dataFetcher("list", this::getList)
          .build()
      )
      /**
       * Mutation
       */
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("listCreate", this::createList)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("listUpdate", this::updateList)
          .build()
      );
  }

  private Future<ListObject> getList(DataFetchingEnvironment dataFetchingEnvironment) {
    String listGuid = dataFetchingEnvironment.getArgument("listGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return listProvider.getByGuidRequestHandler(listGuid, routingContext, AuthUserScope.LIST_GET);
  }


  public Future<ListObject> createList(DataFetchingEnvironment dataFetchingEnvironment) {
    String appGuid = dataFetchingEnvironment.getArgument("appGuid");
    Map<String, Object> listPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    ListInputProps mailingInputProps = new JsonObject(listPropsMap).mapTo(ListInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return listProvider.insertListRequestHandler(appGuid, mailingInputProps, routingContext);
  }

  public Future<ListObject> updateList(DataFetchingEnvironment dataFetchingEnvironment) {
    String listGuid = dataFetchingEnvironment.getArgument("listGuid");
    Map<String, Object> listPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    ListInputProps mailingInputProps = new JsonObject(listPropsMap).mapTo(ListInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return listProvider.updateListRequestHandler(listGuid, mailingInputProps, routingContext);
  }


}
