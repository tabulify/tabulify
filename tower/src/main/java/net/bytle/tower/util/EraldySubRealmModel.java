package net.bytle.tower.util;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.module.organization.inputs.OrgaUserInputProps;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;
import net.bytle.tower.eraldy.module.realm.inputs.RealmInputProps;
import net.bytle.tower.eraldy.module.user.inputs.UserInputProps;
import net.bytle.type.EmailAddress;
import net.bytle.type.Handle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EraldySubRealmModel {
  private static final Logger LOGGER = LogManager.getLogger(EraldySubRealmModel.class);

  public static final Handle REALM_HANDLE = Handle.ofFailSafe("datacadamia");
  private final EraldyApiApp apiApp;

  public EraldySubRealmModel(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
  }

  public static EraldySubRealmModel getOrCreate(EraldyApiApp apiApp) {
    return new EraldySubRealmModel(apiApp);
  }

  public Future<Void> insertModelInDatabase() {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();


    UserInputProps userInputProps = new UserInputProps();
    userInputProps.setEmailAddress(EmailAddress.ofFailSafe("owner@datacadamia.com"));

    return this.apiApp.getHttpServer().getServer().getPostgresClient()
      .getPool()
      .withConnection(sqlConnection -> apiApp
        .getUserProvider()
        .getsertOnServerStartup(eraldyRealm,null, userInputProps, sqlConnection)
        .recover(err->Future.failedFuture(new InternalException("Error on user getsert",err)))
        .compose(ownerUser -> {
          /**
           * Create a organisation user row
           */
          LOGGER.info(REALM_HANDLE+": ownerUser created");
          OrgaUserInputProps organisationUserInputProps = new OrgaUserInputProps();
          organisationUserInputProps.setRole(OrgaRole.OWNER);
          return apiApp
            .getOrganizationUserProvider()
            .getsertOnServerStartup(eraldyRealm.getOrganization(), ownerUser, organisationUserInputProps, sqlConnection)
            .recover(err->Future.failedFuture(new InternalException("Error on user organization getsert",err)))
            .compose(ownerResult -> {

              LOGGER.info(REALM_HANDLE+": organisation ownerUser created");
              ownerResult.setOrganization(eraldyRealm.getOrganization());

              RealmInputProps realmInputProps = new RealmInputProps();
              realmInputProps.setHandle(REALM_HANDLE);
              realmInputProps.setName(REALM_HANDLE + " Realm");
                return this.apiApp.getRealmProvider()
                  .getsertOnServerStartup(null, ownerResult,realmInputProps, sqlConnection);
              }
            )
            .recover(err->Future.failedFuture(new InternalException("Error on realm getsert",err)))
            .compose(realm -> Future.succeededFuture());
        }));

  }
}
