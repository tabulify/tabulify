package net.bytle.tower.eraldy.module.auth.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.common.graphql.EraldyGraphQL;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthUser;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class AuthGraphQLImpl {


  private final EraldyApiApp apiApp;

  public AuthGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder wiringBuilder) {

    this.apiApp = eraldyGraphQL.getApp();

    wiringBuilder
      .type(
        newTypeWiring("Query")
          .dataFetcher("authMeOrga", this::getOrganizationUserMe)
          .build()
      )
      .type(
        newTypeWiring("Query")
          .dataFetcher("authMe", this::getUserMe)
          .build()
      );
  }

  private Future<User> getUserMe(DataFetchingEnvironment dataFetchingEnvironment) {

    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    AuthUser signedInUser;
    try {
      signedInUser = this.apiApp.getAuthProvider().getSignedInAuthUser(routingContext);
    } catch (NotFoundException e) {
      return Future.succeededFuture();
    }
    User user = this.apiApp.getAuthProvider().toModelUser(signedInUser);
    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(user.getGuid().getRealmId())
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setMessage("The realm for the user (" + signedInUser.getSubject() + ") was not found")
              .buildWithContextFailing(routingContext)
          );
        }
        return this.apiApp
          .getUserProvider()
          .getUserByGuid(user.getGuid(), realm)
          .compose(orgUser -> {
            if (orgUser == null) {
              return Future.failedFuture(
                TowerFailureException.builder()
                  .setMessage("The authenticated user (" + signedInUser.getSubject() + "," + signedInUser.getSubjectEmail() + ") was not found")
                  .buildWithContextFailing(routingContext)
              );
            }
            return Future.succeededFuture(orgUser);
          });
      });

  }

  private Future<OrgaUser> getOrganizationUserMe(DataFetchingEnvironment dataFetchingEnvironment) {
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    AuthUser signedInUser;
    try {
      signedInUser = this.apiApp.getAuthProvider().getSignedInAuthUser(routingContext);
    } catch (NotFoundException e) {
      // null. no user and not 401
      // because this is not an error
      return Future.succeededFuture();
    }
    User user = this.apiApp.getAuthProvider().toModelUser(signedInUser);
    if (!(user instanceof OrgaUser)) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
          .setMessage("The authenticated user (" + signedInUser.getSubject() + "," + signedInUser.getSubjectEmail() + ") is not an organization user")
          .buildWithContextFailing(routingContext)
      );
    }

    OrgaUser orgaUser = (OrgaUser) user;
    return this.apiApp
      .getOrganizationUserProvider()
      .getOwnerOrganizationUserByGuid(orgaUser.getGuid())
      .compose(orgUser -> {
        if (orgUser == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("The authenticated user (" + signedInUser.getSubject() + "," + signedInUser.getSubjectEmail() + ") is not a member of an organization")
              .buildWithContextFailing(routingContext)
          );
        }
        return Future.succeededFuture(orgUser);
      });
  }
}
