package net.bytle.tower.eraldy.module.organization.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.AssertionException;
import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.OrganizationRoleProvider;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.organization.inputs.OrgaUserInputProps;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaUserGuidDeserializer;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaUserSerializer;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.eraldy.objectProvider.OrganizationProvider;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.JdbcInsert;
import net.bytle.vertx.db.JdbcSchema;
import net.bytle.vertx.db.JdbcSchemaManager;
import net.bytle.vertx.db.JdbcTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

/**
 * Organization Users are the tenants.
 * They are user of the Eraldy realm
 * that may create a realm, app and list
 * <p>
 * They are child of {@link net.bytle.tower.eraldy.model.openapi.User}
 * and are therefore created via {@link UserProvider}
 * The function {@link #checkOrganizationUserRealmId(Class, Long)}} checks the realm
 * before the object is created.
 */
public class OrganizationUserProvider {

  protected static final Logger LOGGER = LoggerFactory.getLogger(OrganizationUserProvider.class);

  private final EraldyApiApp apiApp;
  private static final String TABLE_NAME = "organization_user";

  private static final String TABLE_PREFIX = "orga_user";
  public static final String ORGA_USER_USER_ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  public static final String ORGA_USER_ORGA_ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN;

  public static final String ORGA_USER_CREATION_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  public static final String ORGA_USER_MODIFICATION_TIME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
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

    server
      .getJacksonMapperManager()
      .addDeserializer(OrgaUserGuid.class, new JacksonOrgaUserGuidDeserializer(apiApp))
      .addSerializer(OrgaUserGuid.class, new JacksonOrgaUserSerializer(apiApp));

  }


  public Future<OrgaUser> getOrganizationUserByIdentifier(String identifier) {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    return apiApp.getUserProvider()
      .getUserByIdentifier(identifier, eraldyRealm)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return getOrganizationUser((OrgaUser) user);
      })
      .compose(eventualOrgaUser -> {
        if (eventualOrgaUser instanceof OrgaUser) {
          return Future.succeededFuture((OrgaUser) eventualOrgaUser);
        }
        return Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
          .setMessage("The user (" + eventualOrgaUser + ") is not an organiational user")
          .build()
        );
      });
  }

  /**
   * Take a OrganizationUser that was created from the {@link UserProvider} or manually
   * and add organization information (such as the organization object from the database)
   *
   * @param orgaUser - the organization user to add extra info
   * @return null or the organizationUser enriched with the organization
   */
  public <T extends User> Future<T> getOrganizationUser(OrgaUser orgaUser) {

    return this.jdbcPool.withConnection(sqlConnection -> getOrganizationUser(orgaUser, sqlConnection));

  }


  private Future<Row> getOrganizationRowForUser(OrgaUser orgaUser, SqlConnection sqlConnection) {
    Long realmLocalId = orgaUser.getRealm().getLocalId();
    try {
      this.checkOrganizationUserRealmId(OrgaUser.class, realmLocalId);
    } catch (AssertionException e) {
      throw new InternalException("This user has not the Eraldy realm (" + realmLocalId + "). He cannot be an organization user.", e);
    }

    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " + ORGA_USER_USER_ID_COLUMN + " = $1";
    Long localId = orgaUser.getLocalId();
    return sqlConnection.preparedQuery(sql)
      .execute(Tuple.of(localId))
      .recover(e -> Future.failedFuture(new InternalException("Error: " + e.getMessage() + ", while retrieving the orga user by user id with the sql\n" + sql, e)))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          // null
          return Future.succeededFuture();
        }

        if (userRows.size() != OrganizationRoleProvider.OWNER_ROLE_ID) {
          return Future.failedFuture(new InternalException("There is more than one orga user with the id (" + localId + ")"));
        }
        Row row = userRows.iterator().next();
        return Future.succeededFuture(row);
      });
  }

  @SuppressWarnings("SameParameterValue")
  private Future<OrgaUser> setOrganizationFromDatabaseRow(Row row, OrgaUser user, Organization organization, SqlConnection sqlConnection) {

    Future<OrgaUser> futureUser;
    Long userId;
    if (user != null) {
      futureUser = Future.succeededFuture(user);
      userId = user.getLocalId();
    } else {
      userId = row.getLong(ORGA_USER_USER_ID_COLUMN);
      futureUser = this.getOrganisationUserWithoutOrganizationByLocalId(userId, sqlConnection);
    }

    return futureUser
      .compose(resUser -> {
        if (resUser == null) {
          return Future.failedFuture(new InternalException("The organization user with the id (" + userId + ") was not found"));
        }
        Future<Organization> futureOrganization;
        Long orgaId = row.getLong(ORGA_USER_ORGA_ID_COLUMN);
        if (organization != null) {
          futureOrganization = Future.succeededFuture(organization);
        } else {
          futureOrganization = apiApp
            .getOrganizationProvider()
            .getById(orgaId);
        }
        return futureOrganization
          .compose(resOrganization -> {
            if (resOrganization == null) {
              return Future.failedFuture(new InternalException("The organization with the identifier (" + orgaId + ") was not found"));
            }
            resUser.setOrganization(resOrganization);
            resUser.setCreationTime(row.getLocalDateTime(ORGA_USER_CREATION_COLUMN));
            resUser.setModificationTime(row.getLocalDateTime(ORGA_USER_MODIFICATION_TIME_COLUMN));
            return Future.succeededFuture(resUser);

          });
      });

  }

  /**
   * Build a  OrganizationUser object
   * without organization data
   */
  private <T extends User> Future<T> getOrganisationUserWithoutOrganizationByLocalId(Long userId, SqlConnection sqlConnection) {

    return this.apiApp.getUserProvider()
      .getUserByLocalId(
        userId,
        this.apiApp.getEraldyModel().getRealm(),
        sqlConnection
      );

  }


  /**
   *
   * @deprecated not needed as the type of user created depend on the realm
   */
  @Deprecated
  <T extends User> void checkOrganizationUserRealmId(Class<T> userClass, Long localId) throws AssertionException {
    if (userClass.equals(OrgaUser.class) && !this.apiApp.getEraldyModel().isRealmLocalId(localId)) {
      Long eraldyRealmLocalId = this.apiApp.getEraldyModel().getRealmLocalId();
      throw new AssertionException("Organizational user are users from the realm id (" + eraldyRealmLocalId + ") not from the realm id (" + localId + ")");
    }
  }

  public Future<List<User>> getOrgUsers(Organization organization) {
    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " + ORGA_USER_ORGA_ID_COLUMN + " = $1";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(organization.getLocalId()))
      .compose(
        userRows -> {
          List<Future<User>> users = new ArrayList<>();
          Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
          for (Row row : userRows) {
            Long userId = row.getLong(ORGA_USER_USER_ID_COLUMN);
            users.add(this.apiApp.getUserProvider().getUserByLocalId(userId, eraldyRealm));
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

  public Future<OrgaUser> getOrganizationUserByLocalId(Long userId) {
    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    return apiApp
      .getUserProvider()
      .getUserByLocalId(userId, eraldyRealm)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        // With the eraldy realm, the user is an organization user
        return getOrganizationUser((OrgaUser) user);
      })
      .compose(orgaUser -> {
        if (orgaUser instanceof OrgaUser) {
          return Future.succeededFuture((OrgaUser) orgaUser);
        }
        return Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_AUTHORIZED_403)
          .setMessage("The user (" + orgaUser + ") is not an organizational user")
          .build()
        );
      });
  }


  /**
   * Getsert: Get or insert the org user record
   * The user is in the Organization user format to receive
   * the org data
   */
  public Future<OrgaUser> getsertOnServerStartup(Organization organization, OrgaUser user, OrgaUserInputProps orgaUserInputProps, SqlConnection sqlConnection) {
    return this.getOrganizationUser(user, sqlConnection)
      .recover(t -> Future.failedFuture(new InternalException("Error while selecting the eraldy owner realm", t)))
      .compose(organizationUser -> {
        if (organizationUser != null) {
          return Future.succeededFuture((OrgaUser) organizationUser);
        }
        return this.insertOrgaUser(organization, user, orgaUserInputProps, sqlConnection);
      });
  }

  /**
   *
   * @param organization - the organization
   * @param orgaUser - the user (without organization data)
   * @param orgaUserInputProps - the props
   * @param sqlConnection - the connection (used for first eraldy data load)
   */
  private Future<OrgaUser> insertOrgaUser(Organization organization, OrgaUser orgaUser, OrgaUserInputProps orgaUserInputProps, SqlConnection sqlConnection) {
    try {
      this.checkOrganizationUserRealmId(OrgaUser.class, orgaUser.getRealm().getLocalId());
    } catch (AssertionException e) {
      return Future.failedFuture(new InternalException("This user has not the Eraldy realm. He cannot be an organization user.", e));
    }

    Long organizationLocalId = organization.getLocalId();
    if (organizationLocalId == null) {
      return Future.failedFuture(new InternalException("The organization id should not be null when inserting an organization user"));
    }
    Long userLocalId = orgaUser.getLocalId();
    if (userLocalId == null) {
      return Future.failedFuture(new InternalException("The user id should not be null when inserting an organization user"));
    }

    /**
     * Build the orga user
     */
    JdbcInsert jdbcInsert = JdbcInsert.into(this.organizationUserTable)
      .addColumn(OrgaUserCols.CREATION_TIME, DateTimeService.getNowInUtc())
      .addColumn(OrgaUserCols.USER_ID, orgaUser.getLocalId())
      .addColumn(OrgaUserCols.REALM_ID, EraldyModel.REALM_LOCAL_ID);

    /**
     * Orga
     */
    orgaUser.setOrganization(organization);
    jdbcInsert.addColumn(OrgaUserCols.ORGA_ID, orgaUser.getOrganization().getLocalId());

    /**
     * Role
     */
    orgaUser.setOrgaRole(orgaUserInputProps.getRole());
    jdbcInsert.addColumn(OrgaUserCols.ROLE_ID, orgaUser.getOrgaRole().getId());

    /**
     * Execute
     */
    return jdbcInsert
      .execute(sqlConnection)
      .compose(userRows -> Future.succeededFuture(orgaUser));
  }

  /**
   * Take a OrganizationUser that was created from the {@link UserProvider} or manually
   * and add organization information (such as the organization object from the database)
   *
   * @param orgaUser - the organization user created from the {@link UserProvider} or manually
   * @param sqlConnection - the sql connection for transaction
   * @return null or the organizationUser enriched with the organization
   */
  private <T extends User> Future<T> getOrganizationUser(OrgaUser orgaUser, SqlConnection sqlConnection) {

    return this.getOrganizationRowForUser(orgaUser, sqlConnection)
      .compose(row -> {
        if (row == null) {
          // casting below to User will not change the object type
          // we can test instanceOf
          // we return null
          return Future.succeededFuture();
        }
        //noinspection unchecked
        return (Future<T>) this.setOrganizationFromDatabaseRow(row, orgaUser, null, sqlConnection);
      });
  }
}
