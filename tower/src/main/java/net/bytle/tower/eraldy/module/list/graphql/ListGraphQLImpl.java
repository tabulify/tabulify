package net.bytle.tower.eraldy.module.list.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
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
       * Mutation
       */
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("listCreate", this::createList)
          .build()
      );
  }



  public Future<ListObject> createList(DataFetchingEnvironment dataFetchingEnvironment) {
    String appGuid = dataFetchingEnvironment.getArgument("appGuid");
    Map<String, Object> listPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    ListInputProps mailingInputProps = new JsonObject(listPropsMap).mapTo(ListInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return listProvider.insertListRequestHandler(appGuid, mailingInputProps, routingContext);
  }


}
