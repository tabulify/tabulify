package net.bytle.tower.eraldy.objectProvider;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.ValidationException;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.JdbcPoolCs;
import net.bytle.tower.util.JdbcSchemaManager;
import net.bytle.tower.util.Postgres;
import net.bytle.vertx.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.tower.util.JdbcSchemaManager.COLUMN_PART_SEP;
import static net.bytle.tower.util.JdbcSchemaManager.REALM_ID_COLUMN;

/**
 * Manage the get/upsert of a {@link Realm} object asynchronously
 * in the database
 */
public class RealmProvider {


  public static final String TABLE_PREFIX = "realm";
  public static final String TABLE_NAME = "realm";
  public static final String ID = "id";
  public static final String QUALIFIED_TABLE_NAME = JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME;

  protected static final Logger LOGGER = LoggerFactory.getLogger(RealmProvider.class);

  private static final Map<Vertx, RealmProvider> mapRealmByVertx = new HashMap<>();

  public static final String REALM_HANDLE_URL_PARAMETER = "realm";
  public static final String ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "id";
  private static final String REALM_HANDLE_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "handle";
  private static final String REALM_ORGA_ID = TABLE_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN;
  private static final String REALM_DEFAULT_APP_ID = TABLE_PREFIX + COLUMN_PART_SEP + "default" + COLUMN_PART_SEP + AppProvider.APP_ID_COLUMN;
  private static final String REALM_OWNER_ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  private static final String DATA_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "data";
  private static final String ANALYTICS_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "analytics";
  private static final String CREATION_TIME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MODIFICATION_TIME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  public static final String SHORT_PREFIX = "rea";


  private final Vertx vertx;


  public RealmProvider(Vertx routingContext) {
    this.vertx = routingContext;
  }

  public static RealmProvider createFrom(Vertx vertx) {
    RealmProvider realmProvider;
    realmProvider = RealmProvider.mapRealmByVertx.get(vertx);
    if (realmProvider != null) {
      return realmProvider;
    }
    realmProvider = new RealmProvider(vertx);
    RealmProvider.mapRealmByVertx.put(vertx, realmProvider);
    return realmProvider;
  }

  /**
   * @param realm - the realm
   * @return the name or the handle of null
   */
  public static String getNameOrHandle(Realm realm) {
    if (realm.getName() != null) {
      return realm.getName();
    }
    return realm.getHandle();
  }


  public <T extends Realm> T toPublicClone(T realm) {
    // uses unchecked or unsafe operations.
    //noinspection unchecked
    JsonObject entries = JsonObject.mapFrom(realm);
    T clone = (T) entries.mapTo(realm.getClass());
    clone.setLocalId(null);
    clone.setOrganization(OrganizationProvider.createFrom(vertx).toPublicClone(realm.getOrganization()));
    clone.setOwnerUser(UserProvider.createFrom(vertx).toPublicCloneWithoutRealm(realm.getOwnerUser()));
    return clone;
  }

  /**
   * Compute the guid. This is another function
   * to be sure that the object id and guid are consistent
   *
   * @param realm - the realm
   */
  private void computeGuid(Realm realm) {
    if (realm.getGuid() != null) {
      return;
    }
    String guid = this.computeGuid(realm.getLocalId());
    realm.setGuid(guid);
  }

  /**
   * @param realm the realm to upsert
   * @return the realm with the id
   */
  public Future<Realm> upsertRealm(Realm realm) {

    if (realm.getLocalId() != null || realm.getGuid() != null) {
      return updateRealm(realm);
    }
    String handle = realm.getHandle();
    if (handle == null) {
      InternalException internalException = new InternalException("The realm handle is mandatory to upsert a realm");
      return Future.failedFuture(internalException);
    }
    /**
     * We don't use the SQL upsert statement
     * to no create a gap in the sequence
     * See identifier.md for more info
     */
    return updateRealmByHandleAndGetRowSet(realm)
      .compose(rowSet -> {
        if (rowSet.size() == 0) {
          return insertRealm(realm);
        }
        Long realmId = rowSet.iterator().next().getLong(REALM_ID_COLUMN);
        realm.setLocalId(realmId);
        this.computeGuid(realm);
        return Future.succeededFuture(realm);
      });
  }

  /**
   * @param realm - the realm to check
   * @return true or false
   */
  @SuppressWarnings("unused")
  private Future<Boolean> exists(Realm realm) {
    String sql;
    Future<RowSet<Row>> futureResponse;
    if (realm.getLocalId() != null || realm.getGuid() != null) {
      if (realm.getLocalId() == null) {
        try {
          this.updateIdFromGuid(realm);
        } catch (CastException e) {
          return Future.failedFuture(ValidationException.create("The Guid is not valid", "guid", realm.getGuid()));
        } catch (NotFoundException e) {
          return Future.failedFuture(new InternalException("The Guid was not found (should not happen)"));
        }
      }
      sql = "select " + ID_COLUMN +
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
        " where " + ID_COLUMN + " = ?";
      futureResponse = JdbcPoolCs.getJdbcPool(this.vertx)
        .preparedQuery(sql)
        .execute(Tuple.of(realm.getLocalId()));
    } else {
      String handle = realm.getHandle();
      if (handle == null) {
        String failureMessage = "An id, guid or handle should be given to check the existence of a realm";
        InternalException internalException = new InternalException(failureMessage);
        return Future.failedFuture(internalException);
      }
      sql = "select " + ID_COLUMN +
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
        " where " + REALM_HANDLE_COLUMN + " = $1";
      futureResponse = JdbcPoolCs.getJdbcPool(this.vertx)
        .preparedQuery(sql)
        .execute(Tuple.of(handle));
    }
    return futureResponse
      .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + sql, t))
      .compose(rows -> {
        if (rows.size() == 1) {
          return Future.succeededFuture(true);
        } else {
          return Future.succeededFuture(false);
        }
      });
  }

  private Future<Realm> insertRealm(Realm realm) {


    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_HANDLE_COLUMN + ",\n" +
      "  " + REALM_ORGA_ID + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + REALM_OWNER_ID_COLUMN + ",\n" +
      "  " + CREATION_TIME_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5)\n" +
      " returning " + REALM_ID_COLUMN;
    JsonObject pgJsonObject = this.getRealmAsJsonObject(realm);
    return JdbcPoolCs.getJdbcPool(this.vertx)
      .preparedQuery(sql)
      .execute(Tuple.of(
        realm.getHandle(),
        realm.getOrganization().getLocalId(),
        pgJsonObject,
        realm.getOwnerUser().getLocalId(),
        DateTimeUtil.getNowUtc()))
      .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + sql, t))
      .compose(rows -> {
        Long realmId = rows.iterator().next().getLong(REALM_ID_COLUMN);
        realm.setLocalId(realmId);
        return Future.succeededFuture(realm);
      });
  }

  private void checkEraldyOwnerUser(User owner) {
    if (!UsersUtil.isEraldyUser(owner)) {
      throw new InternalException("The realm owner (" + UsersUtil.toHandleRealmIdentifier(owner) + ") is not an eraldy user");
    }
    if (owner.getLocalId() == null) {
      throw new InternalException("The realm owner (" + UsersUtil.toHandleRealmIdentifier(owner) + ") has no id");
    }
  }

  private Future<Realm> updateRealm(Realm realm) {


    if (realm.getLocalId() != null) {
      String sql = "update\n" +
        JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
        "set \n" +
        "  " + REALM_HANDLE_COLUMN + " = $1,\n" +
        "  " + REALM_ORGA_ID + " = $2,\n" +
        "  " + DATA_COLUMN + " = $3,\n" +
        "  " + MODIFICATION_TIME_COLUMN + " = $4\n" +
        "where \n" +
        "  " + ID_COLUMN + " = $5\n";

      /**
       * JsonB object
       */
      JsonObject pgJsonObject = this.getRealmAsJsonObject(realm);

      return JdbcPoolCs.getJdbcPool(this.vertx)
        .preparedQuery(sql)
        .execute(
          Tuple.of(
            realm.getHandle(),
            realm.getOrganization().getLocalId(),
            pgJsonObject,
            DateTimeUtil.getNowUtc(),
            realm.getLocalId()
          )
        )
        .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + sql, t))
        .compose(ok -> {
            // Compute the guid: A realm may have an id without guid
            // This is the case for the Eraldy realm where the database id is known
            // as instantiation but not the guid
            this.computeGuid(realm);
            return Future.succeededFuture(realm);
          }
        );
    }

    String handle = realm.getHandle();
    if (handle == null) {
      InternalException internalException = new InternalException("To update a realm, an id or handle is mandatory");
      return Future.failedFuture(internalException);
    }

    return updateRealmByHandleAndGetRowSet(realm)
      .compose(rows -> {
        Long realmId = rows.iterator().next().getLong(REALM_ID_COLUMN);
        realm.setLocalId(realmId);
        this.computeGuid(realm);
        return Future.succeededFuture(realm);
      });

  }

  private Future<RowSet<Row>> updateRealmByHandleAndGetRowSet(Realm realm) {

    Organization organization = realm.getOrganization();
    if(organization==null){
      throw new IllegalArgumentException("The organization of realm can not be null");
    }

    String sql = "update\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      "set \n" +
      "  " + REALM_ORGA_ID + " = $1,\n" +
      "  " + DATA_COLUMN + " = $2,\n" +
      "  " + MODIFICATION_TIME_COLUMN + " = $3\n" +
      "where \n" +
      "  " + REALM_HANDLE_COLUMN + " = $4\n" +
      "RETURNING " + REALM_ID_COLUMN;

    /**
     * JsonB object
     */
    JsonObject pgJsonObject = this.getRealmAsJsonObject(realm);

    return JdbcPoolCs.getJdbcPool(this.vertx)
      .preparedQuery(sql)
      .execute(Tuple.of(
        organization.getLocalId(),
        pgJsonObject,
        DateTimeUtil.getNowUtc(),
        realm.getHandle()
      ))
      .onFailure(e -> LOGGER.error("Error while updating the realm by handle. Sql: \n" + sql, e));
  }

  private JsonObject getRealmAsJsonObject(Realm realm) {
    JsonObject data = JsonObject.mapFrom(realm);
    data.remove(Guid.GUID);
    data.remove(RealmManagerProvider.OWNER_KEY);
    return data;
  }

  /**
   * @param realm - the realm to update
   * @throws CastException     - if the guid is not valid
   * @throws NotFoundException if the guid is empty
   */
  private void updateIdFromGuid(Realm realm) throws CastException, NotFoundException {
    String guid = realm.getGuid();
    if (guid == null) {
      throw new NotFoundException("The guid is empty");
    }
    Long id = Guid.getSingleIdFromGuid(SHORT_PREFIX, guid, vertx);
    realm.setLocalId(id);
  }

  public Future<Realm> getRealmFromId(Long realmId) {
    return getRealmFromId(realmId, Realm.class);
  }

  /**
   * @param realmId - the realmId
   * @return the realm
   */
  private <T extends Realm> Future<T> getRealmFromId(Long realmId, Class<T> clazz) {

    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " + REALM_ID_COLUMN + " = $1";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(realmId))
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the realm by id with the sql\n" + sql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("the realm id (" + realmId + ") returns  more than one application"));
        }
        Row row = userRows.iterator().next();
        return this.getRealmFromDatabaseRow(row, clazz);
      });
  }

  public Future<Realm> getRealmFromHandle(String realmHandle) {
    return getRealmFromHandle(realmHandle, Realm.class);
  }

  private <T extends Realm> Future<T> getRealmFromHandle(String realmHandle, Class<T> clazz) {

    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    return jdbcPool.preparedQuery("SELECT * FROM cs_realms.realm WHERE realm_handle = $1")
      .execute(Tuple.of(realmHandle))
      .onFailure(e -> {
        throw new InternalException(e);
      })
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.failedFuture(new NotFoundException("the realm handle (" + realmHandle + ") was not found"));
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("the realm id (" + realmHandle + ") returns  more than one application"));
        }
        Row row = userRows.iterator().next();
        return this.getRealmFromDatabaseRow(row, clazz);

      });
  }


  public <T extends Realm> Future<List<T>> getRealmsForOwner(User user, Class<T> clazz) {
    UsersUtil.assertEraldyUser(user);
    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    return jdbcPool.preparedQuery("SELECT * FROM cs_realms.realm\n" +
        "where\n" +
        " " + REALM_ORGA_ID + " = $1")
      .execute(Tuple.of(user.getLocalId()))
      .onFailure(e -> {
        throw new InternalException(e);
      })
      .compose(rows -> this.getRealmsFromRows(rows, clazz));
  }

//  public <T extends Realm> Future<List<T>> getRealmsForOwner(User user, Class<T> clazz) {
//    UsersUtil.assertEraldyUser(user);
//    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
//    return jdbcPool.preparedQuery("SELECT * FROM cs_realms.realm\n" +
//        "where\n" +
//        " " + REALM_OWNER_ID + " = $1")
//      .execute(Tuple.of(user.getId()))
//      .onFailure(e -> {
//        throw new InternalException(e);
//      })
//      .compose(rows -> this.getRealmsFromRows(rows, clazz));
//  }

  private <T extends Realm> Future<List<T>> getRealmsFromRows(RowSet<Row> realmRows, Class<T> clazz) {
    List<Future<T>> futureRealms = new ArrayList<>();
    for (Row row : realmRows) {

      Future<T> futureRealm = this.getRealmFromDatabaseRow(row, clazz);
      futureRealms.add(futureRealm);

    }
    return Future.all(futureRealms)
      .onFailure(t -> LOGGER.error("Error while getting the future realms", t))
      .compose(compositeFuture -> Future.succeededFuture(compositeFuture.list()));
  }


  public <T extends Realm> Future<T> getRealmFromDatabaseRow(Row row, Class<T> clazz) {


    String realmHandle = row.getString(REALM_HANDLE_COLUMN);
    Long realmId = row.getLong(ID_COLUMN);
    JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);
    if (clazz.equals(RealmAnalytics.class)) {
      JsonObject jsonAnalytics = Postgres.getFromJsonB(row, ANALYTICS_COLUMN);
      if (jsonAnalytics != null) {
        jsonAppData = jsonAppData.mergeIn(jsonAnalytics);
      }
    }
    T realm = Json.decodeValue(jsonAppData.toBuffer(), clazz);

    realm.setHandle(realmHandle);
    realm.setLocalId(realmId);
    this.computeGuid(realm);
    Long orgaId = row.getLong(REALM_ORGA_ID);
    Future<Organization> futureOrganization = OrganizationProvider.createFrom(vertx).getById(orgaId);
    Long realmIdContactColumn = row.getLong(REALM_OWNER_ID_COLUMN);
    Future<User> futureOwnerUser = UserProvider.createFrom(vertx).getUserById(realmIdContactColumn, EraldyDomain.get().getEraldyRealm());
    Long defaultAppId = row.getLong(REALM_DEFAULT_APP_ID);
    Future<App> futureApp;
    if (defaultAppId == null) {
      futureApp = Future.succeededFuture();
    } else {
      futureApp = AppProvider.create(vertx)
        .getAppById(defaultAppId, realm);
    }
    return Future.all(futureOrganization, futureOwnerUser, futureApp)
      .onFailure(t -> LOGGER.error("Error while getting the future for building the realm", t))
      .compose(result -> {
        realm.setOrganization(result.resultAt(0));
        realm.setOwnerUser(result.resultAt(1));
        realm.setDefaultApp(result.resultAt(2));
        return Future.succeededFuture(realm);
      });

  }

  public Future<Realm> getRealmFromGuid(String guid) {
    return getRealmFromGuid(guid, Realm.class);
  }

  private <T extends Realm> Future<T> getRealmFromGuid(String guid, Class<T> clazz) {
    long realmId;
    try {
      realmId = Guid.getSingleIdFromGuid(SHORT_PREFIX, guid, vertx);
    } catch (CastException e) {
      return Future.failedFuture(e);
    }
    return getRealmFromId(realmId, clazz);
  }

  public Future<List<RealmWithAppUris>> getRealmsWithAppUris() {
    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    String aliasAppUris = "app_uris";
    String selectRealmSql = "select " +
      REALM_ID_COLUMN + ",\n" +
      REALM_HANDLE_COLUMN + ",\n" +
      "STRING_AGG(app_uri,', ') as " + aliasAppUris + "\n" +
      "  from \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + AppProvider.TABLE_NAME + " app\n" +
      "right join " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " realm\n" +
      "on app.app_realm_id = realm.realm_id\n" +
      "group by " + REALM_ID_COLUMN + ", " + REALM_HANDLE_COLUMN + "\n" +
      "order by " + REALM_HANDLE_COLUMN;
    return jdbcPool.preparedQuery(selectRealmSql)
      .execute()
      .onFailure(e -> LOGGER.error("select realms error. Error: " + e.getMessage() + ". With the sql:\n" + selectRealmSql, e))
      .compose(realmRows -> {

        List<RealmWithAppUris> realmsWithDomains = new ArrayList<>();
        for (Row row : realmRows) {

          RealmWithAppUris realmWithDomains = new RealmWithAppUris();
          realmsWithDomains.add(realmWithDomains);

          Long realmId = row.getLong(ID_COLUMN);
          realmWithDomains.setGuid(this.computeGuid(realmId));

          String realmHandle = row.getString(REALM_HANDLE_COLUMN);
          realmWithDomains.setHandle(realmHandle);

          String app_domains = row.getString(aliasAppUris);
          List<URI> appDomains = new ArrayList<>();
          if (app_domains != null) {
            appDomains = Arrays.stream(app_domains
                .split(", "))
              .map(URI::create)
              .collect(Collectors.toList());
          }
          realmWithDomains.setAppUris(appDomains);

        }
        return Future.succeededFuture(realmsWithDomains);
      });
  }

  private String computeGuid(Long realmId) {
    return Guid.createGuidStringFromObjectId(SHORT_PREFIX, realmId, vertx);
  }

  public Future<Realm> getRealmFromGuidOrHandle(String realmGuid, String realmHandle) {
    return getRealmFromGuidOrHandle(realmGuid, realmHandle, Realm.class);
  }

  public <T extends Realm> Future<T> getRealmFromGuidOrHandle(String realmGuid, String realmHandle, Class<T> clazz) {

    if (realmGuid != null) {
      return getRealmFromGuid(realmGuid, clazz);
    }
    if (realmHandle != null) {
      return getRealmFromHandle(realmHandle, clazz);
    }
    throw new InternalException("getRealmFromGuidOrHandle: The realmGuid or realmHandle should be given");
  }

  public Guid getGuidObject(String guid) throws CastException {
    return Guid.createObjectFromRealmId(SHORT_PREFIX, guid, vertx);
  }


  /**
   * @param realm - a realm
   * @return a realm for the frontend (ie name is not null, guid is computed and id is null)
   */
  public Realm toEraldyFrontEnd(Realm realm) {
    Realm frontEndRealm = JsonObject.mapFrom(realm).mapTo(Realm.class);
    frontEndRealm.setLocalId(null);
    frontEndRealm.setName(getNameOrHandle(frontEndRealm));
    return frontEndRealm;
  }

  public Future<RealmAnalytics> getRealmAnalyticsFromGuidOrHandle(String realmGuid, String realmHandle) {
    return getRealmFromGuidOrHandle(realmGuid, realmHandle, RealmAnalytics.class);
  }


  public Realm clone(Realm realm) {
    Realm realmClone = JsonObject.mapFrom(realm).mapTo(Realm.class);
    // The local id is ignored in Json serialization
    realmClone.setLocalId(realm.getLocalId());
    return realmClone;
  }

}
