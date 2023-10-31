package net.bytle.tower.eraldy.objectProvider;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.RealmManager;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.JdbcPostgresPool;
import net.bytle.vertx.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.bytle.vertx.JdbcSchemaManager.COLUMN_PART_SEP;

/**
 * Manage the get/upsert of a {@link RealmManager} object asynchronously
 * in the database
 */
public class RealmManagerProvider {


  public static final String TABLE_NAME = "realm_manager";
  public static final String REALM_MANAGER_PREFIX = TABLE_NAME;
  public static final String OWNER_KEY = "owner";

  protected static final Logger LOGGER = LoggerFactory.getLogger(RealmManagerProvider.class);


  private static final String REALM_MANAGER_REALM_ID_COLUMN = REALM_MANAGER_PREFIX + COLUMN_PART_SEP + RealmProvider.ID_COLUMN;
  private static final String REALM_MANAGER_EMAIL_COLUMN = REALM_MANAGER_PREFIX + COLUMN_PART_SEP + "email";

  private static final String CREATION_TIME_COLUMN = REALM_MANAGER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MODIFICATION_TIME_COLUMN = REALM_MANAGER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private static RealmManagerProvider REALM_OWNER_PROVIDER;


  private final Vertx vertx;


  public RealmManagerProvider(Vertx routingContext) {
    this.vertx = routingContext;
  }

  public static RealmManagerProvider createFrom(Vertx vertx) {
    if (REALM_OWNER_PROVIDER != null) {
      return REALM_OWNER_PROVIDER;
    }
    REALM_OWNER_PROVIDER = new RealmManagerProvider(vertx);
    return REALM_OWNER_PROVIDER;
  }


  public RealmManager toPublicClone(RealmManager realmManager) {
    RealmManager clone = JsonObject.mapFrom(realmManager).mapTo(RealmManager.class);
    clone.setRealm(RealmProvider.createFrom(vertx).toPublicClone(realmManager.getRealm()));
    clone.setOwner(UserProvider.createFrom(vertx).toPublicCloneWithoutRealm(realmManager.getOwner()));
    return clone;
  }


  /**
   * @param realmManager the realm to upsert
   * @return the realm with the id
   */
  @SuppressWarnings("unused")
  public Future<RealmManager> upsertRealmOwner(RealmManager realmManager) {

    if (!realmManager.getOwner().getRealm().getLocalId().equals(EraldyDomain.get().getEraldyRealm().getLocalId())) {
      throw new InternalException("The realm manager user is not a Eraldy user and cannot own a realm");
    }

    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_MANAGER_REALM_ID_COLUMN + ",\n" +
      "  " + REALM_MANAGER_EMAIL_COLUMN + ",\n" +
      "  " + CREATION_TIME_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3)\n" +
      " ON CONFLICT (" + REALM_MANAGER_REALM_ID_COLUMN + "," + REALM_MANAGER_EMAIL_COLUMN + ") DO UPDATE set " + MODIFICATION_TIME_COLUMN + " = EXCLUDED." + CREATION_TIME_COLUMN;

    return JdbcPostgresPool.getJdbcPool()
      .preparedQuery(sql)
      .execute(Tuple.of(realmManager.getRealm().getLocalId(), realmManager.getOwner().getEmail(), DateTimeUtil.getNowUtc()))
      .onFailure(t -> LOGGER.error("Error while inserting the realm owner with the following sql:\n" + sql, t))
      .compose(rows -> Future.succeededFuture(realmManager));
  }


  @SuppressWarnings("unused")
  public Future<List<Realm>> getRealmsForOwner(User user) {

    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();
    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      "WHERE\n" +
      " " + REALM_MANAGER_EMAIL_COLUMN + "=$1";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(user.getEmail()))
      .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + sql, t))
      .compose(realmRows -> {

        List<Future<Realm>> futureRealms = new ArrayList<>();
        for (Row row : realmRows) {

          Long realmId = row.getLong(REALM_MANAGER_REALM_ID_COLUMN);
          Future<Realm> futureRealm = RealmProvider.createFrom(vertx).getRealmFromId(realmId);
          futureRealms.add(futureRealm);

        }
        return Future.all(futureRealms)
          .onFailure(t -> LOGGER.error("Error while retrieving the list of realm for owner", t))
          .compose(compositeFuture -> Future.succeededFuture(compositeFuture.list()));
      });
  }


  @SuppressWarnings("unused")
  public Future<RealmManager> getRealmFromDatabaseRow(Row row) {

    Long realmId = row.getLong(REALM_MANAGER_REALM_ID_COLUMN);
    Long managerId = row.getLong(REALM_MANAGER_EMAIL_COLUMN);
    Future<Realm> futureRealm = RealmProvider.createFrom(vertx).getRealmFromId(realmId);
    Future<User> futureUser = UserProvider.createFrom(vertx).getEraldyUserById(managerId);
    return Future.all(futureRealm, futureUser)
      .onFailure(t -> LOGGER.error("Error while building the realm manager"))
      .compose(result -> {
        RealmManager futureRealmManager = new RealmManager();
        futureRealmManager.setRealm(result.resultAt(0));
        futureRealmManager.setOwner(result.resultAt(1));
        return Future.succeededFuture(futureRealmManager);
      });

  }


}
