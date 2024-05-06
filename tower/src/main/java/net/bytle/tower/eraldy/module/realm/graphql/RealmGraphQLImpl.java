package net.bytle.tower.eraldy.module.realm.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.common.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.module.organization.model.Organization;
import net.bytle.tower.eraldy.module.realm.inputs.UserInputProps;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.tower.eraldy.module.realm.model.UserGuid;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.JdbcPagination;
import org.dataloader.BatchLoaderEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class RealmGraphQLImpl {


  private final EraldyApiApp app;


  public RealmGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder typeWiringBuilder) {
    this.app = eraldyGraphQL.getApp();

    /**
     * Realm Guid scalar
     */
    final GraphQLScalarType REALM_GUID = GraphQLScalarType
      .newScalar()
      .name("RealmGuid")
      .description("The Guid for a realm")
      .coercing(new GraphQLRealmGuidCoercing(this.app.getJackson()))
      .build();
    typeWiringBuilder.scalar(REALM_GUID);

    final GraphQLScalarType USER_GUID = GraphQLScalarType
      .newScalar()
      .name("UserGuid")
      .description("The Guid for a user in the realm")
      .coercing(new GraphQLUserGuidCoercing(this.app.getJackson()))
      .build();
    typeWiringBuilder.scalar(USER_GUID);

    /**
     * Map type to function
     * Data Type mapping
     */
    typeWiringBuilder
      .type(
        newTypeWiring("Query")
          .dataFetcher("realm", this::getRealm)
          .build()
      )
      .type(
        newTypeWiring("Query")
          .dataFetcher("user", this::getRealmUser)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("userUpdate", this::updateRealmUser)
          .build()
      );

    typeWiringBuilder
      .type(
        newTypeWiring("Realm")
          .dataFetcher("organization", this::getRealmOrganization)
          .build()
      );
    typeWiringBuilder
      .type(
        newTypeWiring("Realm")
          .dataFetcher("ownerUser", this::getRealmOwner)
          .build()
      );
    typeWiringBuilder
      .type(
        newTypeWiring("Realm")
          .dataFetcher("apps", this::getRealmApps)
          .build()
      );
    typeWiringBuilder
      .type(
        newTypeWiring("Realm")
          .dataFetcher("users", this::getRealmUsers)
          .build()
      );

  }

  private Future<User> getRealmUser(DataFetchingEnvironment dataFetchingEnvironment) {
    UserGuid userGuid = dataFetchingEnvironment.getArgument("userGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app
      .getRealmProvider()
      .getRealmFromLocalId(userGuid.getRealmId())
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm of the user (" + userGuid + ") was not found")
              .build()
          );
        }
        return this.app.getAuthProvider()
          .checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_USER_GET)
          .compose(v ->
            this.app.getUserProvider()
              .getUserByGuid(userGuid, realm)
              .compose(user -> {
                if (user == null) {
                  return Future.failedFuture(
                    TowerFailureException.builder()
                      .setType(TowerFailureTypeEnum.NOT_FOUND_404)
                      .setMessage("The user (" + userGuid + ") was not found")
                      .build()
                  );
                }
                return Future.succeededFuture(user);
              }));
      });
  }

  private Future<List<App>> getRealmApps(DataFetchingEnvironment dataFetchingEnvironment) {
    Realm realm = dataFetchingEnvironment.getSource();
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_APPS_GET)
      .compose(v -> this.app.getAppProvider().getAppsForRealm(realm))
      .compose(Future::succeededFuture,
        err -> Future.failedFuture(
          TowerFailureException.builder()
            .setMessage("Unable to get the apps for the realm (" + realm + ")")
            .setCauseException(err)
            .buildWithContextFailing(routingContext)
        )
      );
  }

  private Future<OrgaUser> getRealmOwner(DataFetchingEnvironment dataFetchingEnvironment) {
    Realm realm = dataFetchingEnvironment.getSource();
    if (realm.getOwnerUser().getEmailAddress() != null) {
      return Future.succeededFuture(realm.getOwnerUser());
    }
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getAuthProvider()
      .checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_OWNER_GET)
      .compose(v -> this
        .app
        .getOrganizationUserProvider()
        .getOrganizationUserByGuid(realm.getOwnerUser().getGuid())
      );
  }

  private Future<User> updateRealmUser(DataFetchingEnvironment dataFetchingEnvironment) {
    UserGuid userGuid = dataFetchingEnvironment.getArgument("userGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    Map<String, Object> userInputPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    UserInputProps userInputProps = new JsonObject(userInputPropsMap).mapTo(UserInputProps.class);

    return this.app.getAuthProvider()
      .getRealmByLocalIdWithAuthorizationCheck(userGuid.getRealmId(), AuthUserScope.REALM_USER_UPDATE, routingContext)
      .compose(realm -> this.app.getUserProvider().getUserByGuid(userGuid, realm))
      .compose(user -> {
        if (user == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The user (" + userGuid + ") was not found")
            .build()
          );
        }
        return this.app.getUserProvider().updateUser(user, userInputProps);
      });

  }

  private Future<Realm> getRealm(DataFetchingEnvironment dataFetchingEnvironment) {
    RealmGuid realmGuid = dataFetchingEnvironment.getArgument("realmGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app
      .getRealmProvider()
      .getRealmFromGuid(realmGuid)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm was not found")
              .build()
          );
        }
        return this.app.getAuthProvider()
          .checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_GET)
          .compose(Future::succeededFuture);
      });
  }

  private Future<List<User>> getRealmUsers(DataFetchingEnvironment dataFetchingEnvironment) {
    Realm realm = dataFetchingEnvironment.getSource();
    Map<String, Object> paginationPropsMap = dataFetchingEnvironment.getArgument("pagination");
    // Type safe (if null, the value was not passed)
    JdbcPagination pagination = new JsonObject(paginationPropsMap).mapTo(JdbcPagination.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getAuthProvider()
      .checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_USER_GET)
      .compose(v -> this.app.getUserProvider().getUsers(realm, pagination));
  }

  private Future<Organization> getRealmOrganization(DataFetchingEnvironment dataFetchingEnvironment) {
    Realm realm = dataFetchingEnvironment.getSource();
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getAuthProvider()
      .checkRealmAuthorization(routingContext, realm, AuthUserScope.ORGA_GET)
      .compose(v -> this.app.getRealmProvider().buildOrganizationAtRequestTimeEventually(realm));
  }


  /**
   * Minimal example that returns empty users
   */
  @SuppressWarnings("unused")
  public CompletionStage<List<User>> batchLoadUsers(List<String> strings, BatchLoaderEnvironment batchLoaderEnvironment) {
    // A list of ids and returns a CompletionStage for a list of users
    Future<List<User>> future = Future.succeededFuture(new ArrayList<>());
    return future.toCompletionStage();
  }

}
