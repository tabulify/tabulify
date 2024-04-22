package net.bytle.tower.eraldy.module.organization.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.AssertionException;
import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.organization.inputs.OrgaUserInputProps;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaUserGuidDeserializer;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaUserSerializer;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Organization Users are the tenants.
 * They are user of the Eraldy realm
 * that may create a realm, app and list
 * <p>
 * They are child of {@link net.bytle.tower.eraldy.model.openapi.User}
 */
public class OrganizationUserProvider {

  protected static final Logger LOGGER = LoggerFactory.getLogger(OrganizationUserProvider.class);

  private final EraldyApiApp apiApp;
  private static final String TABLE_NAME = "organization_user";
  private final Pool jdbcPool;
  private final JdbcTable organizationUserTable;


  public OrganizationUserProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    Server server = apiApp.getHttpServer().getServer();
    this.jdbcPool = server.getPostgresClient().getPool();

    this.organizationUserTable = JdbcTable.build(jdbcSchema, TABLE_NAME, OrgaUserCols.values())
      .addPrimaryKeyColumn(OrgaUserCols.USER_ID)
      .addPrimaryKeyColumn(OrgaUserCols.ORGA_ID)
      .build();

    /**
     * Hack as it's used transitory
     * while we move to GraphQL
     */

    server
      .getJacksonMapperManager()
      .addDeserializer(OrgaUserGuid.class, new JacksonOrgaUserGuidDeserializer(apiApp))
      .addSerializer(OrgaUserGuid.class, new JacksonOrgaUserSerializer(apiApp));

  }


  public Future<OrgaUser> getOrganizationUserByIdentifier(String identifier) {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    return apiApp.getUserProvider()
      .getUserByIdentifier(identifier, eraldyRealm)
      .compose(this::checkAndReturnUser);

  }

  public Future<OrgaUser> getOrganizationUserByLocalId(Long localId, SqlConnection sqlConnection) {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    return apiApp.getUserProvider()
      .getUserByLocalId(localId, eraldyRealm, sqlConnection)
      .compose(this::checkAndReturnUser);

  }

  public Future<OrgaUser> getOrganizationUserByLocalId(Long localId) {

    return this.jdbcPool.withConnection(sqlConnection -> getOrganizationUserByLocalId(localId, sqlConnection));

  }

  /**
   * This function is called by the user provider to create an Orga User
   */
  public Future<OrgaUser> createOrganizationUserObjectFromLocalIdOrNull(Long userLocalId) {
    return this.jdbcPool.withConnection(sqlConnection -> createOrganizationUserObjectFromLocalIdOrNull(userLocalId, sqlConnection));
  }

  /**
   * The entry to create an orga user from local id
   * @param userLocalId - the user id
   * @param sqlConnection a connection
   * @return an orga user or null
   */
  public Future<OrgaUser> createOrganizationUserObjectFromLocalIdOrNull(Long userLocalId, SqlConnection sqlConnection) {


    return JdbcSelect.from(this.organizationUserTable)
      .addEqualityPredicate(OrgaUserCols.USER_ID, userLocalId)
      .execute(sqlConnection)
      .compose(jdbcRowSet -> {
        if (jdbcRowSet == null) {
          return Future.succeededFuture();
        }
        if (jdbcRowSet.size() > 1) {
          LOGGER.error("Not yet supported: More than one organization for the organization user (user id: " + userLocalId + ")");
        }
        JdbcRow jdbcRow = jdbcRowSet.iterator().next();
        return this.createOrganizationUserFromDatabaseRow(jdbcRow, null);
      });

  }

  private Future<OrgaUser> createOrganizationUserFromDatabaseRow(JdbcRow jdbcRow, Organization knownOrganization) {
    OrgaUser orgaUser = new OrgaUser();
    orgaUser.setOrgaRole(OrgaRole.fromRoleIdFailSafe(jdbcRow.getInteger(OrgaUserCols.ROLE_ID)));
    Future<Organization> futureOrganization;
    Long orgaId = jdbcRow.getLong(OrgaUserCols.ORGA_ID);
    if (knownOrganization == null) {
      futureOrganization = apiApp
        .getOrganizationProvider()
        .getById(orgaId);
    } else {
      if (knownOrganization.getLocalId().equals(orgaId)) {
        return Future.failedFuture(new InternalException("Orga id between row (" + orgaId + ") and organization (" + knownOrganization.getLocalId() + ") not consistent (ie not the same)"));
      }
      futureOrganization = Future.succeededFuture(knownOrganization);
    }

    return futureOrganization
      .compose(organization -> {
        if (organization == null) {
          return Future.failedFuture(new InternalException("The organization with the identifier (" + orgaId + ") was not found"));
        }
        orgaUser.setOrganization(organization);
        return Future.succeededFuture(orgaUser);
      });
  }


  /**
   *
   * @deprecated not needed as the type of user created depend on the realm
   */
  @Deprecated
  <T extends User> void checkOrganizationUserRealmId(Class<T> userClass, Long localId) throws AssertionException {
    if (userClass.equals(OrgaUser.class) && !this.apiApp.getEraldyModel().isRealmLocalId(localId)) {
      throw new AssertionException("Organizational user are users from the realm id (" + EraldyModel.REALM_LOCAL_ID + ") not from the realm id (" + localId + ")");
    }
  }

  public Future<List<User>> getOrgUsers(Organization organization) {

    return JdbcSelect.from(this.organizationUserTable)
      .addEqualityPredicate(OrgaUserCols.ORGA_ID, organization.getLocalId())
      .execute()
      .compose(
        userRows -> {
          List<Future<OrgaUser>> users = new ArrayList<>();
          for (JdbcRow row : userRows) {
            users.add(this.createOrganizationUserFromDatabaseRow(row, organization));
          }
          // all: stop on the first failure
          return Future.all(users);
        },
        err -> Future.failedFuture(new InternalException("Unable to get the org users for the organization (" + organization + "). Message:" + err.getMessage(), err))
      )
      .compose(
        res -> Future.succeededFuture(res.list()),
        err -> Future.failedFuture(new InternalException("Unable to build the org users for the organization (" + organization + "). Message:" + err.getMessage(), err))
      );

  }


  private <T extends User> Future<OrgaUser> checkAndReturnUser(T user) {
    if (user == null) {
      return Future.succeededFuture();
    }
    if (user instanceof OrgaUser) {
      return Future.succeededFuture((OrgaUser) user);
    }
    return Future.failedFuture(TowerFailureException.builder()
      .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
      .setMessage("The user (" + user + ") is not an organizational user")
      .build()
    );
  }


  /**
   * Getsert: Get or insert the org user record
   * The user is in the Organization user format to receive
   * the org data
   */
  public Future<OrgaUser> getsertOnServerStartup(Organization organization, User user, OrgaUserInputProps orgaUserInputProps, SqlConnection sqlConnection) {
    return this.getOrganizationUserByLocalId(user.getLocalId(), sqlConnection)
      .recover(t -> Future.failedFuture(new InternalException("Error while selecting the eraldy owner realm", t)))
      .compose(organizationUser -> {
        if (organizationUser != null) {
          return Future.succeededFuture(organizationUser);
        }
        return this.insertOrgaUser(organization, user, orgaUserInputProps, sqlConnection);
      });
  }

  /**
   *
   * @param organization - the organization
   * @param user - the user (without organization data)
   * @param orgaUserInputProps - the props
   * @param sqlConnection - the connection (used for first eraldy data load)
   */
  private Future<OrgaUser> insertOrgaUser(Organization organization, User user, OrgaUserInputProps orgaUserInputProps, SqlConnection sqlConnection) {

    if (!user.getRealm().getLocalId().equals(EraldyModel.REALM_LOCAL_ID)) {
      return Future.failedFuture(new InternalException("This user has not the Eraldy realm. He cannot be an organization user."));
    }

    Long organizationLocalId = organization.getLocalId();
    if (organizationLocalId == null) {
      return Future.failedFuture(new InternalException("The organization id should not be null when inserting an organization user"));
    }
    Long userLocalId = user.getLocalId();
    if (userLocalId == null) {
      return Future.failedFuture(new InternalException("The user id should not be null when inserting an organization user"));
    }

    /**
     * Execute
     */
    return JdbcInsert.into(this.organizationUserTable)
      .addColumn(OrgaUserCols.CREATION_TIME, DateTimeService.getNowInUtc())
      .addColumn(OrgaUserCols.USER_ID, user.getLocalId())
      .addColumn(OrgaUserCols.REALM_ID, EraldyModel.REALM_LOCAL_ID)
      .addColumn(OrgaUserCols.ROLE_ID, orgaUserInputProps.getRole().getId())
      .addColumn(OrgaUserCols.ORGA_ID, organization.getLocalId())
      .execute(sqlConnection)
      .compose(userRows -> {
        /**
         * We build it back
         * Transferring the properties of the user to the orga users is too cumbersome
         * and error-prone
         */
        return this.getOrganizationUserByLocalId(user.getLocalId(), sqlConnection);
      });
  }




}
