package net.bytle.tower.eraldy.module.app.graphql;

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
import net.bytle.tower.eraldy.module.app.inputs.AppInputProps;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.common.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

import java.util.List;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class AppGraphQLImpl {


  private final EraldyApiApp app;

  public AppGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder wiringBuilder) {

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
       * Mutation
       */
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("appUpdate", this::updateApp)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("appCreate", this::createApp)
          .build()
      )
      /**
       * Type wiring
       */
      .type(
        newTypeWiring("App")
          .dataFetcher("lists", this::getAppLists)
          .build()
      )
      .type(
        newTypeWiring("App")
          .dataFetcher("ownerUser", this::getAppOwner)
          .build()
      );

  }

  private Future<App> createApp(DataFetchingEnvironment dataFetchingEnvironment) {
    RealmGuid realmGuid = dataFetchingEnvironment.getArgument("realmGuid");
    Map<String, Object> appPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    AppInputProps appInputProps = new JsonObject(appPropsMap).mapTo(AppInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getRealmProvider()
      .getRealmFromGuid(realmGuid)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException
              .builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm guid (" + realmGuid + ") was not found")
              .build()
          );
        }
        return this.app.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.APP_CREATE);
      })
      .compose(realm -> this.app.getAppProvider().insertApp(appInputProps, realm, null));
  }

  private Future<App> updateApp(DataFetchingEnvironment dataFetchingEnvironment) {
    AppGuid appGuid = dataFetchingEnvironment.getArgument("appGuid");
    Map<String, Object> appPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    AppInputProps appInputProps = new JsonObject(appPropsMap).mapTo(AppInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getRealmProvider()
      .getRealmFromLocalId(appGuid.getRealmId())
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The realm of the app guid (" + appGuid.toStringLocalIds() + ") was not found")
            .build()
          );
        }
        return this.app.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.APP_UPDATE);
      })
      .compose(realm -> this.app.getAppProvider().getAppByGuid(appGuid, realm))
      .compose(app -> this.app.getAppProvider().updateApp(app, appInputProps));

  }

  private Future<OrgaUser> getAppOwner(DataFetchingEnvironment dataFetchingEnvironment) {
    App app = dataFetchingEnvironment.getSource();
    if (app.getOwnerUser().getEmailAddress() != null) {
      return Future.succeededFuture(app.getOwnerUser());
    }
    return this.app.getOrganizationUserProvider().getOrganizationUserByGuid(app.getOwnerUser().getGuid());
  }

  private Future<List<ListObject>> getAppLists(DataFetchingEnvironment dataFetchingEnvironment) {
    App app = dataFetchingEnvironment.getSource();
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getAuthProvider()
      .checkRealmAuthorization(routingContext, app.getRealm(), AuthUserScope.APP_LISTS_GET)
      .compose(v -> this.app.getListProvider().getListsForApp(app))
      .compose(Future::succeededFuture);
  }

  private Future<App> getApp(DataFetchingEnvironment dataFetchingEnvironment) {
    AppGuid appGuid = dataFetchingEnvironment.getArgument("appGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);


    return this.app.
      getRealmProvider().getRealmFromLocalId(appGuid.getRealmId())
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm of the app (" + appGuid.getHashOrNull() + ") was not found")
              .build()
          );
        }
        return this.app.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.APP_GET);
      }).compose(realm -> this.app.getAppProvider().getAppByGuid(appGuid, realm))
      .compose(app -> {
        if (app == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm was found but not the app (" + appGuid.getHashOrNull() + ")")
              .build()
          );
        }
        return Future.succeededFuture(app);
      });
  }
}
