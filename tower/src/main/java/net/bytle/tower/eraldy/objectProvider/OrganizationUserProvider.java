package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytle.vertx.JdbcSchemaManager.COLUMN_PART_SEP;

/**
 * Organization User are the real customers
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

  @SuppressWarnings("unused")
  public Future<Boolean> isOrganizationUser(User user) {
    return getOrganizationUserById(user.getLocalId(), user)
      .compose(organizationUser -> {
        if (organizationUser != null) {
          return Future.succeededFuture(true);
        } else {
          return Future.succeededFuture(false);
        }
      });
  }

  @SuppressWarnings("unused")
  public Future<OrganizationUser> getOrganizationUserByGuid(String guid) {
    return apiApp.getUserProvider()
      .getUserByGuid(guid)
      .compose(user -> {
        if (user == null) {
          return Future.succeededFuture();
        }
        return getOrganizationUserByUser(user);
      });
  }

  public Future<OrganizationUser> getOrganizationUserByUser(User user) {
    UsersUtil.assertEraldyUser(user);
    return getOrganizationUserById(user.getLocalId(), user);
  }

  public Future<OrganizationUser> getOrganizationUserById(Long localId, User user) {


    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " + ORGA_USER_USER_ID_COLUMN + " = $1";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(localId))
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the orga user by user id with the sql\n" + sql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("their is more than one orga user with the id (" + user.getLocalId() + ")"));
        }
        Row row = userRows.iterator().next();
        return this.getOrgaUserFromDatabaseRow(row, user);
      });
  }

  private Future<OrganizationUser> getOrgaUserFromDatabaseRow(Row row, User user) {

    Future<User> futureUser;
    Long userId;
    if (user != null) {
      UsersUtil.assertEraldyUser(user);
      futureUser = Future.succeededFuture(user);
      userId = user.getLocalId();
    } else {
      userId = row.getLong(ORGA_USER_USER_ID_COLUMN);
      futureUser = apiApp.getUserProvider()
        .getEraldyUserById(userId);
    }

    return futureUser
      .onFailure(t -> LOGGER.error("Error while getting the user", t))
      .compose(userFromFuture -> {
        if (userFromFuture == null) {
          return Future.failedFuture(new NotFoundException("The eraldy user with the id (" + userId + ") was not found"));
        }
        OrganizationUser organizationUser = JsonObject.mapFrom(userFromFuture).mapTo(OrganizationUser.class);
        Long orgaId = row.getLong(ORGA_USER_ORGA_ID_COLUMN);
        return apiApp.getOrganizationProvider()
          .getById(orgaId)
          .onFailure(t -> LOGGER.error("Error while getting the organization", t))
          .compose(organization -> {
            if (organization == null) {
              return Future.failedFuture(new NotFoundException("The organization with the id (" + orgaId + ") was not found"));
            }
            organizationUser.setOrganization(organization);
            organizationUser.setCreationTime(row.getLocalDateTime(ORGA_USER_CREATION_COLUMN));
            organizationUser.setModificationTime(row.getLocalDateTime(ORGA_USER_MODIFICATION_TIME_COLUMN));
            return Future.succeededFuture(organizationUser);

          });
      });

  }

}
