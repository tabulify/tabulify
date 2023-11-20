package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.OrganizationApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.OrganizationPublicMixin;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureStatusEnum;

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
  public Future<ApiResponse<OrganizationUser>> organizationUserMeGet(RoutingContext routingContext) {

    return this.apiApp.getAuthSignedInUser(routingContext,User.class)
      .compose(signedInUser->{
        if(signedInUser==null){
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_LOGGED_IN_401)
              .buildWithContextFailing(routingContext)
          );
        }
        return this.apiApp
          .getOrganizationUserProvider()
          .getOrganizationUserByGuid(signedInUser.getGuid())
          .compose(orgUser -> {
            if (orgUser == null) {
              return Future.failedFuture(
                TowerFailureException.builder()
                  .setStatus(TowerFailureStatusEnum.NOT_AUTHORIZED_403)
                  .setMessage("The authenticated user (" + signedInUser.getGuid()+","+signedInUser.getEmail() + ") is not member of an organization")
                  .buildWithContextFailing(routingContext)
              );
            }
            ApiResponse<OrganizationUser> response = new ApiResponse<>(orgUser)
              .setMapper(this.orgUserMapper);
            return Future.succeededFuture(response);
          });
      });

  }
}
