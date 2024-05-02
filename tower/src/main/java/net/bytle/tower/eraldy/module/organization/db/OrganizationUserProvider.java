package net.bytle.tower.eraldy.module.organization.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.AssertionException;
import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.organization.inputs.OrgaUserInputProps;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaUserGuidDeserializer;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaUserGuidSerializer;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.organization.model.Organization;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.module.user.db.UserCols;
import net.bytle.tower.eraldy.module.user.model.UserGuid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.Server;
import net.bytle.vertx.db.*;
import net.bytle.vertx.guid.GuidDeSer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Organization Users are the tenants.
 * They are user of the Eraldy realm
 * that may create a realm, app and list
 * <p>
 * They are child of {@link net.bytle.tower.eraldy.model.openapi.User}
 */
public class OrganizationUserProvider {

  public static final String GUID_PREFIX = "oru";
  protected static final Logger LOGGER = LoggerFactory.getLogger(OrganizationUserProvider.class);

  private final EraldyApiApp apiApp;
  private static final String TABLE_NAME = "organization_user";
  private final Pool jdbcPool;
  private final JdbcTable organizationUserTable;


  public OrganizationUserProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    Server server = apiApp.getHttpServer().getServer();
    this.jdbcPool = server.getPostgresClient().getPool();

    HashMap<JdbcColumn, JdbcColumn> userForeignKeys = new HashMap<>();
    userForeignKeys.put(OrgaUserCols.USER_ID, UserCols.ID);
    userForeignKeys.put(OrgaUserCols.REALM_ID, UserCols.REALM_ID);
    this.organizationUserTable = JdbcTable.build(jdbcSchema, TABLE_NAME, OrgaUserCols.values())
      .addPrimaryKeyColumn(OrgaUserCols.USER_ID)
      .addPrimaryKeyColumn(OrgaUserCols.ORGA_ID)
      .addForeignKeyColumns(userForeignKeys)
      .build();

    /**
     * Hack as it's used transitory
     * while we move to GraphQL
     */

    GuidDeSer orgaUserGuidDeser = this.apiApp.getHttpServer().getServer().getHashId().getGuidDeSer(GUID_PREFIX, 3);
    server
      .getJacksonMapperManager()
      .addDeserializer(OrgaUserGuid.class, new JacksonOrgaUserGuidDeserializer(orgaUserGuidDeser))
      .addSerializer(OrgaUserGuid.class, new JacksonOrgaUserGuidSerializer(orgaUserGuidDeser));

  }


  public Future<OrgaUser> getOrganizationUserByIdentifier(OrgaUserGuid identifier) {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    return apiApp.getUserProvider()
      .getUserByGuid(identifier, eraldyRealm)
      .compose(this::checkAndReturnUser);

  }

  /**
   * @param userGuid - a user guid and not an orga user guid as it may not exist
   */
  public Future<OrgaUser> getOrganizationUserByGuid(UserGuid userGuid, SqlConnection sqlConnection) {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    return apiApp.getUserProvider()
      .getUserByLocalId(userGuid.getLocalId(), eraldyRealm, sqlConnection)
      .compose(this::checkAndReturnUser);

  }

  public Future<OrgaUser> getOrganizationUserByGuid(OrgaUserGuid orgaUserGuid) {

    return this.jdbcPool.withConnection(sqlConnection -> getOrganizationUserByGuid(orgaUserGuid, sqlConnection));

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
        if (jdbcRowSet == null || jdbcRowSet.size() == 0) {
          return Future.succeededFuture();
        }
        if (jdbcRowSet.size() > 1) {
          LOGGER.error("Not yet supported: More than one organization for the organization user (user id: " + userLocalId + ")");
        }
        JdbcRow jdbcRow = jdbcRowSet.iterator().next();
        return Future.succeededFuture(this.createOrganizationUserFromDatabaseRow(jdbcRow, null));
      });

  }

  private OrgaUser createOrganizationUserFromDatabaseRow(JdbcRow jdbcRow, Organization knownOrganization) {
    OrgaUser orgaUser = new OrgaUser();
    OrgaUserGuid guid = new OrgaUserGuid();
    guid.setOrganizationId(jdbcRow.getLong(OrgaUserCols.ORGA_ID));
    guid.setLocalId(jdbcRow.getLong(OrgaUserCols.USER_ID));
    orgaUser.setGuid(guid);
    orgaUser.setOrgaRole(OrgaRole.fromRoleIdFailSafe(jdbcRow.getInteger(OrgaUserCols.ROLE_ID)));
    orgaUser.setOrganization(Objects.requireNonNullElseGet(knownOrganization, () -> Organization.createFromAnyId(guid)));
    orgaUser.setOrgaRole(OrgaRole.fromRoleIdFailSafe(jdbcRow.getInteger(OrgaUserCols.ROLE_ID)));
    orgaUser.setCreationTime(jdbcRow.getLocalDateTime(OrgaUserCols.CREATION_TIME));
    orgaUser.setModificationTime(jdbcRow.getLocalDateTime(OrgaUserCols.MODIFICATION_IME));
    return orgaUser;
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

  public Future<List<OrgaUser>> getOrgUsers(Organization organization) {

    return JdbcSelect.from(this.organizationUserTable)
      .addSelectAllColumnsFromTable(this.apiApp.getUserProvider().getTable())
      .addEqualityPredicate(OrgaUserCols.ORGA_ID, organization.getGuid().getLocalId())
      .execute()
      .recover(err -> Future.failedFuture(new InternalException("Unable to get the org users for the organization (" + organization + "). Message:" + err.getMessage(), err)))
      .compose(rowSet -> {

        List<OrgaUser> users = new ArrayList<>();
        for (JdbcRow row : rowSet) {
          OrgaUser organizationUserFromDatabaseRow = this.createOrganizationUserFromDatabaseRow(row, organization);
          OrgaUser user = this.apiApp.getUserProvider().buildUserFromRow(organizationUserFromDatabaseRow, row, this.apiApp.getEraldyModel().getRealm());
          users.add(user);
        }
        return Future.succeededFuture(users);

      });

  }


  private <T extends User> Future<OrgaUser> checkAndReturnUser(T user) {
    if (user == null) {
      return Future.succeededFuture();
    }
    if (user instanceof OrgaUser) {
      return Future.succeededFuture((OrgaUser) user);
    }
    /**
     * We have for now a user, but it's not an orga user
     * therefore we return null
     */
    return Future.succeededFuture();
  }


  /**
   * Getsert: Get or insert the org user record
   * The user is in the Organization user format to receive
   * the org data
   */
  public Future<OrgaUser> getsertOnServerStartup(Organization organization, User user, OrgaUserInputProps orgaUserInputProps, SqlConnection sqlConnection) {
    return this.getOrganizationUserByGuid(user.getGuid(), sqlConnection)
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

    if (user.getRealm().getGuid().getLocalId() != EraldyModel.REALM_LOCAL_ID) {
      return Future.failedFuture(new InternalException("This user has not the Eraldy realm. He cannot be an organization user."));
    }

    OrgaGuid organizationGuid = organization.getGuid();
    if (organizationGuid == null) {
      return Future.failedFuture(new InternalException("The organization id should not be null when inserting an organization user"));
    }
    UserGuid userGuid = user.getGuid();
    if (userGuid == null) {
      return Future.failedFuture(new InternalException("The user id should not be null when inserting an organization user"));
    }

    /**
     * Execute
     */
    return JdbcInsert.into(this.organizationUserTable)
      .addColumn(OrgaUserCols.CREATION_TIME, DateTimeService.getNowInUtc())
      .addColumn(OrgaUserCols.USER_ID, userGuid.getLocalId())
      .addColumn(OrgaUserCols.REALM_ID, EraldyModel.REALM_LOCAL_ID)
      .addColumn(OrgaUserCols.ROLE_ID, orgaUserInputProps.getRole().getId())
      .addColumn(OrgaUserCols.ORGA_ID, organizationGuid.getLocalId())
      .execute(sqlConnection)
      .compose(userRows -> {
        /**
         * We build it back
         * Transferring the properties of the user to the orga users is too cumbersome
         * and error-prone
         */
        return this.getOrganizationUserByGuid(userGuid, sqlConnection);
      });
  }

  /**
   * Utility class to create a minimal orga user from a guid
   * (used in row reading)
   *
   * @param orgaUserGuid - the guid to transform
   * @param objectRealm - the realm of the objects (app, list, ...), **not from the user** to check that the organizations of the objects realm and of the user are the same
   *                    The organization of the user and the organization of the user realm may not be equal as an orga user has an eraldy realm
   */
  public OrgaUser toOrgaUserFromGuid(OrgaUserGuid orgaUserGuid, Realm objectRealm) {

    OrgaUser newOwner = new OrgaUser();
    newOwner.setGuid(orgaUserGuid);
    newOwner.setRealm(this.apiApp.getEraldyModel().getRealm());

    /**
     * Orga
     */
    Organization organization = Organization.createFromAnyId(orgaUserGuid);
    newOwner.setOrganization(organization);

    /**
     * Check
     * The object realm is null when we recreate an orga user
     * for authentication
     */
    if (objectRealm != null) {
      OrgaGuid objectRealmOrgId = objectRealm.getOrganization().getGuid();
      OrgaGuid ownerOrgId = newOwner.getOrganization().getGuid();
      if (!Objects.equals(objectRealmOrgId, ownerOrgId)) {
        throw new InternalException("The object realm org id (" + objectRealmOrgId + ") is not the same as the owner org id (" + ownerOrgId + ")");
      }
    }
    return newOwner;

  }
}
