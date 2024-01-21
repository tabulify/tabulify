package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.AssertionException;
import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyRealm;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.bytle.vertx.JdbcSchemaManager.COLUMN_PART_SEP;

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
  private final PgPool jdbcPool;

  public OrganizationUserProvider(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getApexDomain().getHttpServer().getServer().getJdbcPool();
  }


  public Future<OrganizationUser> getOrganizationUserByIdentifier(String identifier, Realm realm) {

    return apiApp.getUserProvider()
      .getUserByIdentifier(identifier, realm, OrganizationUser.class)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return buildOrgUserFromDb(user);
      });
  }

  public Future<OrganizationUser> getOrganizationUserByUser(OrganizationUser orgUser) {

    return buildOrgUserFromDb(orgUser);

  }

  /**
   * Take a OrganizationUser that was created as a child user object
   * and add organization information only such as the organization object from the database
   *
   * @param organizationUser - the organization user to add extra info
   */
  private Future<OrganizationUser> buildOrgUserFromDb(OrganizationUser organizationUser) {

    if (organizationUser.getOrganization() != null) {
      return Future.succeededFuture(organizationUser);
    }

    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " + ORGA_USER_USER_ID_COLUMN + " = $1";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(organizationUser.getLocalId()))
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the orga user by user id with the sql\n" + sql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("There is more than one orga user with the id (" + organizationUser.getLocalId() + ")"));
        }
        Row row = userRows.iterator().next();
        return this.buildOrgaUserFromDatabaseRow(row, organizationUser, null);
      });
  }

  private Future<OrganizationUser> buildOrgaUserFromDatabaseRow(Row row, OrganizationUser user, Organization organization) {

    Future<OrganizationUser> futureUser;
    Long userId;
    if (user != null) {
      futureUser = Future.succeededFuture(user);
      userId = user.getLocalId();
    } else {
      userId = row.getLong(ORGA_USER_USER_ID_COLUMN);
      futureUser = this.buildUserAsOrganizationUserById(userId);
    }

    return futureUser
      .compose(
        userFromFuture -> {
          if (userFromFuture == null) {
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
              userFromFuture.setOrganization(resOrganization);
              userFromFuture.setCreationTime(row.getLocalDateTime(ORGA_USER_CREATION_COLUMN));
              userFromFuture.setModificationTime(row.getLocalDateTime(ORGA_USER_MODIFICATION_TIME_COLUMN));
              return Future.succeededFuture(userFromFuture);

            });
        });

  }

  /**
   * Build a  OrganizationUser object
   * without organization data
   */
  private Future<OrganizationUser> buildUserAsOrganizationUserById(Long userId) {

    Realm eraldyRealm = EraldyRealm.get().getRealm();
    return this.apiApp.getUserProvider().getUserByLocalId(userId, eraldyRealm.getLocalId(), OrganizationUser.class, eraldyRealm);

  }


  <T extends User> void checkOrganizationUserRealmId(Class<T> userClass, Long localId) throws AssertionException {
    if (userClass.equals(OrganizationUser.class) && !this.apiApp.isEraldyRealm(localId)) {
      Realm eraldyRealm = this.apiApp.getEraldyRealm();
      throw new AssertionException("Organizational user are users from the realm id (" + eraldyRealm.getLocalId() + ") not from the realm id (" + localId + ")");
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
          Realm eraldyRealm = this.apiApp.getEraldyRealm();
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
    return apiApp.getUserProvider()
      .getUserByLocalId(userId, realmId, OrganizationUser.class, realm)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return buildOrgUserFromDb(user);
      });
  }

  public Future<OrganizationUser> upsertUserAndBuildOrgUser(OrganizationUser ownerUser) {

    return apiApp.getUserProvider()
      .upsertUser(ownerUser)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return buildOrgUserFromDb(user);
      });
  }
}
