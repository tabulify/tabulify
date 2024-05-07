package net.bytle.tower.eraldy.module.organization.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.common.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.organization.model.Organization;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

import java.util.List;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class OrgaGraphQLImpl {


  private final EraldyApiApp apiApp;

  public OrgaGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder wiringBuilder) {

    this.apiApp = eraldyGraphQL.getApp();

    final GraphQLScalarType ORGA_GUID = GraphQLScalarType
      .newScalar()
      .name("OrgaGuid")
      .description("The Guid for a organization")
      .coercing(new GraphQLOrgaGuidCoercing(eraldyGraphQL.getApp().getJackson()))
      .build();
    wiringBuilder.scalar(ORGA_GUID);

    final GraphQLScalarType ORGA_USER_GUID = GraphQLScalarType
      .newScalar()
      .name("OrgaUserGuid")
      .description("The Guid for the user of an organization")
      .coercing(new GraphQLOrgaUserGuidCoercing(eraldyGraphQL.getApp().getJackson()))
      .build();
    wiringBuilder.scalar(ORGA_USER_GUID);

    /**
     * Query
     */
    wiringBuilder.type(
      newTypeWiring("Query")
        .dataFetcher("organization", this::getOrganization)
        .build()
    );
    wiringBuilder.type(
      newTypeWiring("Query")
        .dataFetcher("orgaUser", this::getOrganizationUser)
        .build()
    );

    /**
     * Data Type mapping
     */
    wiringBuilder.type(
      newTypeWiring("Organization")
        .dataFetcher("users", this::getOrganizationUsers)
        .build()
    );

    wiringBuilder.type(
      newTypeWiring("OrganizationUser")
        .dataFetcher("ownedRealms", this::getOwnedRealms)
        .build()
    );

    wiringBuilder.type(
      newTypeWiring("OrganizationUser")
        .dataFetcher("organization", this::getUserOrganization)
        .build()
    );
  }

  /**
   * @return the organization of a user
   */
  private Future<Organization> getUserOrganization(DataFetchingEnvironment dataFetchingEnvironment) {
    OrgaUser orgaUser = dataFetchingEnvironment.getSource();
    if (orgaUser.getOrganization().getName() != null) {
      return Future.succeededFuture(orgaUser.getOrganization());
    }
    return this.apiApp.getOrganizationProvider()
      .getByGuid(orgaUser.getOrganization().getGuid(), orgaUser.getRealm());
  }

  private Future<OrgaUser> getOrganizationUser(DataFetchingEnvironment dataFetchingEnvironment) {
    OrgaUserGuid orgaUserGuid = dataFetchingEnvironment.getArgument("orgaUserGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);


    return this.apiApp
      .getAuthProvider()
      .checkOrgAuthorization(routingContext, orgaUserGuid.getOrgaGuid(), AuthUserScope.ORGA_USER_GET)
      .compose(v -> this.apiApp
        .getOrganizationUserProvider()
        .getOrganizationUserByGuid(orgaUserGuid)
        .compose(orgUser -> {
          if (orgUser == null) {
            return Future.failedFuture(
              TowerFailureException.builder()
                .setType(TowerFailureTypeEnum.NOT_FOUND_404)
                .setMessage("The organization user (" + orgaUserGuid + ") was not found")
                .buildWithContextFailing(routingContext)
            );
          }
          return Future.succeededFuture(orgUser);
        }));

  }

  private Future<List<Realm>> getOwnedRealms(DataFetchingEnvironment dataFetchingEnvironment) {
    OrgaUser orgaUser = dataFetchingEnvironment.getSource();
    return this.apiApp.getRealmProvider()
      .getRealmsForOwner(orgaUser)
      .compose(Future::succeededFuture);
  }


  private Future<List<OrgaUser>> getOrganizationUsers(DataFetchingEnvironment dataFetchingEnvironment) {
    Organization organization = dataFetchingEnvironment.getSource();
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.apiApp
      .getAuthProvider()
      .checkOrgAuthorization(routingContext, organization.getGuid(), AuthUserScope.ORGA_USERS_GET)
      .compose(v -> this.apiApp.getOrganizationUserProvider()
        .getOrgUsers(organization));
  }

  private Future<Organization> getOrganization(DataFetchingEnvironment dataFetchingEnvironment) {
    OrgaGuid orgaGuid = dataFetchingEnvironment.getArgument("orgaGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);

    return this.apiApp
      .getRealmProvider()
      .getRealmFromLocalId(orgaGuid.getRealmId())
      .compose(realm -> {

        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setMessage("The realm was not found for the orga guid (" + orgaGuid + ")")
              .build()
          );
        }

        return this.apiApp
          .getAuthProvider()
          .checkRealmAuthorization(routingContext, realm, AuthUserScope.ORGA_GET)
          .compose(v -> this.apiApp
            .getOrganizationProvider()
            .getByGuid(orgaGuid, realm)
          )
          .compose(org -> {
            if (org == null) {
              return Future.failedFuture(
                TowerFailureException.builder()
                  .setType(TowerFailureTypeEnum.NOT_FOUND_404)
                  .setMessage("The organization identifier (" + orgaGuid + ") was not found.")
                  .build()
              );
            }
            return Future.succeededFuture(org);
          });
      });
  }


}
