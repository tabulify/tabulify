package net.bytle.tower.eraldy.module.app.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

import java.util.List;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class AppGraphQL {


  private final EraldyApiApp app;

  public AppGraphQL(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder wiringBuilder) {

    this.app = eraldyGraphQL.getApp();

    final GraphQLScalarType APP_GUID = GraphQLScalarType
      .newScalar()
      .name("AppGuid")
      .description("The guid for an app")
      .coercing(new GraphQLAppGuidCoercing(eraldyGraphQL.getApp().getJackson()))
      .build();
    wiringBuilder.scalar(APP_GUID);


    wiringBuilder
      /**
       * Query
       */
      .type(
        newTypeWiring("Query")
          .dataFetcher("app", this::getApp)
          .build()
      )
      /**
       * Type wiring
       */
      .type(
        newTypeWiring("App")
          .dataFetcher("list", this::getAppLists)
          .build()
      );

  }

  private Future<List<ListObject>> getAppLists(DataFetchingEnvironment dataFetchingEnvironment) {
    App app = dataFetchingEnvironment.getSource();
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getAuthProvider()
      .checkRealmAuthorization(routingContext, app.getRealm(), AuthUserScope.APP_LISTS_GET)
      .compose(v->this.app.getListProvider().getListsForApp(app))
      .compose(Future::succeededFuture);
  }

  private Future<App> getApp(DataFetchingEnvironment dataFetchingEnvironment) {
    String appGuidString = dataFetchingEnvironment.getArgument("appGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    AppGuid appGuid;
    try {
      appGuid = this.app.getJackson().getDeserializer(AppGuid.class).deserialize(appGuidString);
    } catch (CastException e) {
        return Future.failedFuture(
          TowerFailureException
            .builder()
            .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
            .setMessage("The app guid (" + appGuidString + ") is not valid")
            .buildWithContextFailing(routingContext)
        );
    }

    return this.app.
      getRealmProvider().getRealmFromLocalId(appGuid.getRealmId())
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm of the app (" + appGuidString + ") was not found")
              .build()
          );
        }
        return this.app.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_APP_GET);
      }).compose(realm -> this.app.getAppProvider().getAppByGuid(appGuid, realm))
      .compose(app -> {
        if (app == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm was found but not the app (" + appGuidString + ")")
              .build()
          );
        }
        return Future.succeededFuture(app);
      });
  }
}
