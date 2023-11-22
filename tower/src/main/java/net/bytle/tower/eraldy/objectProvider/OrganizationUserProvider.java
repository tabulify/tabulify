package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.AssertionException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.EraldyRealm;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


  public Future<OrganizationUser> getOrganizationUserByGuid(String guid) {

    return apiApp.getUserProvider()
      .getUserByGuid(guid, OrganizationUser.class)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return buildOrgUserFromDb(user);
      });
  }



  /**
   * Take a OrganizationUser that was created as a child user object
   * and add organization information only such as the organization object from the database
   * @param organizationUser  - the organization user to add extra info
   */
  private Future<OrganizationUser> buildOrgUserFromDb(OrganizationUser organizationUser) {


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
        return this.buildOrgaUserFromDatabaseRow(row, organizationUser);
      });
  }

  private Future<OrganizationUser> buildOrgaUserFromDatabaseRow(Row row, OrganizationUser user) {

    Future<OrganizationUser> futureUser;
    Long userId;
    if (user != null) {
      futureUser = Future.succeededFuture(user);
      userId = user.getLocalId();
    } else {
      userId = row.getLong(ORGA_USER_USER_ID_COLUMN);
      futureUser = this.getOrganizationUserById(userId);
    }

    return futureUser
      .onFailure(t -> LOGGER.error("Error while getting the organization user", t))
      .compose(userFromFuture -> {
        if (userFromFuture == null) {
          return Future.failedFuture(new NotFoundException("The organization user with the id (" + userId + ") was not found"));
        }
        Long orgaId = row.getLong(ORGA_USER_ORGA_ID_COLUMN);
        return apiApp
          .getOrganizationProvider()
          .getById(orgaId)
          .onFailure(t -> LOGGER.error("Error while getting the organization", t))
          .compose(organization -> {
            if (organization == null) {
              return Future.failedFuture(new NotFoundException("The organization with the id (" + orgaId + ") was not found"));
            }
            userFromFuture.setOrganization(organization);
            userFromFuture.setCreationTime(row.getLocalDateTime(ORGA_USER_CREATION_COLUMN));
            userFromFuture.setModificationTime(row.getLocalDateTime(ORGA_USER_MODIFICATION_TIME_COLUMN));
            return Future.succeededFuture(userFromFuture);

          });
      });

  }

  public Future<OrganizationUser> getOrganizationUserById(Long userId) {

      Realm eraldyRealm = EraldyRealm.get().getRealm();
      return this.apiApp.getUserProvider().getUserById(userId, eraldyRealm.getLocalId(), OrganizationUser.class, eraldyRealm);

  }

  <T extends User> void checkOrganizationUserRealmId(Class<T> userClass, Long localId) throws AssertionException {
    Realm eraldyRealm = EraldyRealm.get().getRealm();
    if (userClass.equals(OrganizationUser.class) && !localId.equals(eraldyRealm.getLocalId())) {
      throw new AssertionException("Organizational user are users from the realm id (" + eraldyRealm.getLocalId() + ") not from the realm id (" + localId + ")");
    }
  }

}
