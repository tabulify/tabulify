package net.bytle.tower.eraldy.module.list.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.common.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.module.list.inputs.ListInputProps;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;

import java.util.List;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class ListGraphQLImpl {


  private final ListProvider listProvider;
  private final EraldyApiApp app;

  public ListGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder typeWiringBuilder) {

    this.app = eraldyGraphQL.getApp();
    this.listProvider = app.getListProvider();

    /**
     * Scalars
     */
    final GraphQLScalarType LIST_GUID = GraphQLScalarType
      .newScalar()
      .name("ListGuid")
      .description("The Guid for a list")
      .coercing(new GraphQLListGuidCoercing(app.getJackson()))
      .build();
    typeWiringBuilder.scalar(LIST_GUID);

    /**
     * Scalars
     */
    final GraphQLScalarType LIST_USER_GUID = GraphQLScalarType
      .newScalar()
      .name("ListUserGuid")
      .description("The Guid for the user of list")
      .coercing(new GraphQLListGuidCoercing(app.getJackson()))
      .build();
    typeWiringBuilder.scalar(LIST_USER_GUID);

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
       * Data Type mapping
       */
      .type(
        newTypeWiring("List")
          .dataFetcher("ownerUser", this::getListOwnerUser)
          .build()
      )
      .type(
        newTypeWiring("List")
          .dataFetcher("app", this::getListApp)
          .build()
      )
      .type(
        newTypeWiring("List")
          .dataFetcher("mailings", this::getListMailing)
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

  private Future<List<Mailing>> getListMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    ListObject listObject = dataFetchingEnvironment.getSource();
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getAuthProvider()
      .checkRealmAuthorization(routingContext, listObject.getApp().getRealm(), AuthUserScope.MAILINGS_LIST_GET)
      .compose(v -> this.app.getMailingProvider().getMailingsByListWithLocalId(listObject));
  }

  private Future<App> getListApp(DataFetchingEnvironment dataFetchingEnvironment) {
    ListObject list = dataFetchingEnvironment.getSource();
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app
      .getAuthProvider()
      .checkRealmAuthorization(routingContext, list.getApp().getRealm(), AuthUserScope.APP_GET)
      .compose(v -> listProvider.buildAppAtRequestTimeEventually(list));
  }

  private Future<OrgaUser> getListOwnerUser(DataFetchingEnvironment dataFetchingEnvironment) {
    ListObject list = dataFetchingEnvironment.getSource();
    return listProvider.buildListOwnerUserAtRequestTimeEventually(list);
  }

  private Future<ListObject> getList(DataFetchingEnvironment dataFetchingEnvironment) {
    ListGuid listGuid = dataFetchingEnvironment.getArgument("listGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return listProvider.getByGuidRequestHandler(listGuid, routingContext, AuthUserScope.LIST_GET);
  }


  public Future<ListObject> createList(DataFetchingEnvironment dataFetchingEnvironment) {
    AppGuid appGuid = dataFetchingEnvironment.getArgument("appGuid");
    Map<String, Object> listPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    ListInputProps mailingInputProps = new JsonObject(listPropsMap).mapTo(ListInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return listProvider.insertListRequestHandler(appGuid, mailingInputProps, routingContext);
  }

  public Future<ListObject> updateList(DataFetchingEnvironment dataFetchingEnvironment) {
    ListGuid listGuid = dataFetchingEnvironment.getArgument("listGuid");
    Map<String, Object> listPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    ListInputProps listInputProps = new JsonObject(listPropsMap).mapTo(ListInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return listProvider.updateListRequestHandler(listGuid, listInputProps, routingContext);
  }


}
