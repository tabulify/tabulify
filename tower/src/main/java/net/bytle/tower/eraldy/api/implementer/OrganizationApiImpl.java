package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.OrganizationApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthScope;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.OrganizationPublicMixin;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthUser;

import java.util.List;

public class OrganizationApiImpl implements OrganizationApi {
  private final EraldyApiApp apiApp;
  private final JsonMapper orgUserMapper;

  public OrganizationApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
    this.orgUserMapper = this.apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager().jsonMapperBuilder()
      .addMixIn(Organization.class, OrganizationPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .build();
  }

  @Override
  public Future<ApiResponse<List<User>>> orgaOrgaUsersGet(RoutingContext routingContext, String orgaIdentifier) {

    Guid guid;
    try {
      guid = this.apiApp.getOrganizationProvider().createGuid(orgaIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The organization identifier (" + orgaIdentifier + ") is not an valid guid.")
          .build()
      );
    }
    return this.apiApp
      .getAuthProvider()
      .checkOrgAuthorization(routingContext,orgaIdentifier, AuthScope.ORGA_USERS_GET)
      .compose(v-> this.apiApp
        .getOrganizationProvider()
        .getById(guid.getRealmOrOrganizationId())
      )
      .compose(org->{
        if(org==null){
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The organization identifier (" + orgaIdentifier + ") was not found.")
              .build()
          );
        }
        return this.apiApp.getOrganizationUserProvider()
          .getOrgUsers(org);
      })
      .compose(users-> Future.succeededFuture(new ApiResponse<>(users).setMapper(this.orgUserMapper)));

  }

  @Override
  public Future<ApiResponse<OrganizationUser>> organizationUserMeGet(RoutingContext routingContext) {
    AuthUser signedInUser;
    try {
      signedInUser = this.apiApp.getAuthProvider().getSignedInAuthUser(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
          .buildWithContextFailing(routingContext)
      );
    }

    return this.apiApp
      .getOrganizationUserProvider()
      .getOrganizationUserByIdentifier(signedInUser.getSubject(), null)
      .compose(orgUser -> {
        if (orgUser == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
              .setMessage("The authenticated user (" + signedInUser.getSubject() + "," + signedInUser.getSubjectEmail() + ") is not member of an organization")
              .buildWithContextFailing(routingContext)
          );
        }
        ApiResponse<OrganizationUser> response = new ApiResponse<>(orgUser)
          .setMapper(this.orgUserMapper);
        return Future.succeededFuture(response);
      });
  }


}
