package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.AssertionException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.OrganizationRoleProvider;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.db.JdbcSchemaManager;
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

  public static final String ORGA_USER_ORGA_ROLE_ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "orga_role_id";
  public static final String ORGA_USER_CREATION_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  public static final String ORGA_USER_MODIFICATION_TIME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private final Pool jdbcPool;

  public OrganizationUserProvider(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();
  }


  public Future<OrganizationUser> getOrganizationUserByIdentifier(String identifier) {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    return apiApp.getUserProvider()
      .getUserByIdentifier(identifier, eraldyRealm, OrganizationUser.class)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return getOrganizationUserEnrichedWithOrganizationDataEventually(user);
      });
  }

  /**
   * Take a OrganizationUser that was created from the {@link UserProvider} or manually
   * and add organization information (such as the organization object from the database)
   *
   * @param organizationUser - the organization user to add extra info
   * @return null or the organizationUser enriched with the organization
   */
  public Future<OrganizationUser> getOrganizationUserEnrichedWithOrganizationDataEventually(OrganizationUser organizationUser) {

    return this.jdbcPool.withConnection(sqlConnection-> getOrganizationUserEnrichedWithOrganizationDataEventually(organizationUser,sqlConnection));

  }


  private Future<Row> getOrganizationRowForUser(OrganizationUser organizationUser, SqlConnection sqlConnection) {
    Long realmLocalId = organizationUser.getRealm().getLocalId();
    try {
      this.checkOrganizationUserRealmId(OrganizationUser.class, realmLocalId);
    } catch (AssertionException e) {
      throw new InternalException("This user has not the Eraldy realm ("+realmLocalId+"). He cannot be an organization user.", e);
    }

    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " + ORGA_USER_USER_ID_COLUMN + " = $1";
    Long localId = organizationUser.getLocalId();
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
  private Future<OrganizationUser> setOrganizationFromDatabaseRow(Row row, OrganizationUser user, Organization organization, SqlConnection sqlConnection) {

    Future<OrganizationUser> futureUser;
    Long userId;
    if (user != null) {
      futureUser = Future.succeededFuture(user);
      userId = user.getLocalId();
    } else {
      userId = row.getLong(ORGA_USER_USER_ID_COLUMN);
      futureUser = this.getOrganisationUserWithoutOrganizationByLocalId(userId,sqlConnection);
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
  private Future<OrganizationUser> getOrganisationUserWithoutOrganizationByLocalId(Long userId, SqlConnection sqlConnection) {

    return this.apiApp.getUserProvider()
      .getUserByLocalId(
        userId,
        this.apiApp.getEraldyModel().getRealm().getLocalId(),
        OrganizationUser.class,
        this.apiApp.getEraldyModel().getRealm(),
        sqlConnection
      );

  }


  <T extends User> void checkOrganizationUserRealmId(Class<T> userClass, Long localId) throws AssertionException {
    if (userClass.equals(OrganizationUser.class) && !this.apiApp.getEraldyModel().isRealmLocalId(localId)) {
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
            users.add(this.apiApp.getUserProvider().getUserByLocalId(userId, eraldyRealm.getLocalId(), User.class, eraldyRealm));
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

  public Future<OrganizationUser> getOrganizationUserByLocalId(Long userId, Long realmId, Realm realm) {
    return apiApp
      .getUserProvider()
      .getUserByLocalId(userId, realmId, OrganizationUser.class, realm)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return getOrganizationUserEnrichedWithOrganizationDataEventually(user);
      });
  }

  public Future<OrganizationUser> upsertUser(OrganizationUser organizationUser) {

    return apiApp.getUserProvider()
      .upsertUser(organizationUser)
      .compose(resultUser -> {
        if (resultUser == null) {
          return Future.failedFuture(new InternalException("The result user should not be null for the user (" + organizationUser + ")"));
        }
        return getOrganizationUserEnrichedWithOrganizationDataEventually(resultUser);
      })
      .compose(resultOrganizationUser -> {
        if (resultOrganizationUser == null) {
          return insertUser(organizationUser);
        }
        return Future.succeededFuture(resultOrganizationUser);
      });
  }

  public Future<OrganizationUser> insertUser(OrganizationUser organizationUser) {

    return this.jdbcPool.withConnection(sqlConnection->insertUser(organizationUser,sqlConnection));
  }


  /**
   * Getsert: Get or insert the user
   */
  public Future<OrganizationUser> getsertOnServerStartup(OrganizationUser organizationUser, SqlConnection sqlConnection) {
    return this.getOrganizationUserEnrichedWithOrganizationDataEventually(organizationUser, sqlConnection)
      .recover(t -> Future.failedFuture(new InternalException("Error while selecting the eraldy owner realm", t)))
      .compose(selectedOrganizationUser -> {
        Future<OrganizationUser> futureOrganizationUser;
        if (selectedOrganizationUser != null) {
          futureOrganizationUser = Future.succeededFuture(selectedOrganizationUser);
        } else {
          futureOrganizationUser = this.insertUser(organizationUser, sqlConnection);
        }
        return futureOrganizationUser;
      });
  }

  private Future<OrganizationUser> insertUser(OrganizationUser organizationUser, SqlConnection sqlConnection) {
    try {
      this.checkOrganizationUserRealmId(OrganizationUser.class, organizationUser.getRealm().getLocalId());
    } catch (AssertionException e) {
      return Future.failedFuture(new InternalException("This user has not the Eraldy realm. He cannot be an organization user.", e));
    }

    Organization organization = organizationUser.getOrganization();
    if (organization == null) {
      return Future.failedFuture(new InternalException("The organization should not be null when inserting an organizational user"));
    }
    Long organizationLocalId = organization.getLocalId();
    if (organizationLocalId == null) {
      return Future.failedFuture(new InternalException("The organization id should not be null when inserting an organization user"));
    }
    Long userLocalId = organizationUser.getLocalId();
    if (userLocalId == null) {
      return Future.failedFuture(new InternalException("The user id should not be null when inserting an organization user"));
    }

    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + ORGA_USER_USER_ID_COLUMN + ",\n" +
      "  " + ORGA_USER_ORGA_ID_COLUMN + ",\n" +
      "  " + ORGA_USER_ORGA_ROLE_ID_COLUMN + ",\n" +
      "  " + ORGA_USER_CREATION_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4)\n";

    Tuple ownerInsertTuple = Tuple.of(
      userLocalId,
      organizationLocalId,
      OrganizationRoleProvider.OWNER_ROLE_ID,
      DateTimeService.getNowInUtc()
    );
    return sqlConnection
      .preparedQuery(sql)
      .execute(ownerInsertTuple)
      .recover(e -> Future.failedFuture(new InternalException("Error: " + e.getMessage() + ", while inserting the orga user with the sql\n" + sql, e)))
      .compose(userRows -> Future.succeededFuture(organizationUser));
  }

  /**
   * Take a OrganizationUser that was created from the {@link UserProvider} or manually
   * and add organization information (such as the organization object from the database)
   *
   * @param organizationUser - the organization user created from the {@link UserProvider} or manually
   * @param sqlConnection - the sql connection for transaction
   * @return null or the organizationUser enriched with the organization
   */
  private Future<OrganizationUser> getOrganizationUserEnrichedWithOrganizationDataEventually(OrganizationUser organizationUser, SqlConnection sqlConnection) {

    return this.getOrganizationRowForUser(organizationUser, sqlConnection)
      .compose(row -> {
        if (row == null) {
          return Future.succeededFuture();
        }
        return this.setOrganizationFromDatabaseRow(row, organizationUser, null, sqlConnection);
      });
  }
}
