package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
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
import net.bytle.tower.EraldyRealm;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.OrganizationPublicMixin;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.Postgres;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.JdbcSchemaManager;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.vertx.JdbcSchemaManager.COLUMN_PART_SEP;

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

  public static final String REALM_ID_COLUMN = RealmProvider.TABLE_PREFIX + COLUMN_PART_SEP + RealmProvider.ID;

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
  public static final String REALM_GUID_PREFIX = "rea";
  private final PgPool jdbcPool;
  private final EraldyApiApp apiApp;

  /**
   * The json mapper for public realm
   * to create json without any localId and
   * double realm information
   */
  private final ObjectMapper publicRealmJsonMapper;

  public RealmProvider(EraldyApiApp apiApp) {
    this.jdbcPool = apiApp.getApexDomain().getHttpServer().getServer().getJdbcPool();
    this.apiApp = apiApp;
    this.publicRealmJsonMapper = this.apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(RealmAnalytics.class, RealmPublicMixin.class)
      .addMixIn(Organization.class, OrganizationPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .build()
    ;
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
    JsonObject entries = JsonObject.mapFrom(realm);
    T clone = (T) entries.mapTo(realm.getClass());
    clone.setLocalId(null);
    clone.setOrganization(apiApp.getOrganizationProvider().toPublicClone(realm.getOrganization()));
    clone.setOwnerUser(this.apiApp.getUserProvider().toPublicCloneWithoutRealm(realm.getOwnerUser()));
    return clone;
  }


  /**
   * Compute the guid. This is another function
   * to be sure that the object id and guid are consistent
   *
   * @param realm - the realm
   */
  private void getGuidFromLong(Realm realm) {
    if (realm.getGuid() != null) {
      return;
    }
    String guid = this.getGuidFromLong(realm.getLocalId()).toString();
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
        this.getGuidFromLong(realm);
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
      futureResponse = this.jdbcPool
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
      futureResponse = this.jdbcPool
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
    return this.jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        realm.getHandle(),
        realm.getOrganization().getLocalId(),
        pgJsonObject,
        realm.getOwnerUser().getLocalId(),
        DateTimeUtil.getNowInUtc()))
      .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + sql, t))
      .compose(rows -> {
        Long realmId = rows.iterator().next().getLong(REALM_ID_COLUMN);
        realm.setLocalId(realmId);
        return Future.succeededFuture(realm);
      });
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

      return this.jdbcPool
        .preparedQuery(sql)
        .execute(
          Tuple.of(
            realm.getHandle(),
            realm.getOrganization().getLocalId(),
            pgJsonObject,
            DateTimeUtil.getNowInUtc(),
            realm.getLocalId()
          )
        )
        .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + sql, t))
        .compose(ok -> {
            // Compute the guid: A realm may have an id without guid
            // This is the case for the Eraldy realm where the database id is known
            // as instantiation but not the guid
            this.getGuidFromLong(realm);
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
        this.getGuidFromLong(realm);
        return Future.succeededFuture(realm);
      });

  }

  private Future<RowSet<Row>> updateRealmByHandleAndGetRowSet(Realm realm) {

    Organization organization = realm.getOrganization();
    if (organization == null) {
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

    return this.jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        organization.getLocalId(),
        pgJsonObject,
        DateTimeUtil.getNowInUtc(),
        realm.getHandle()
      ))
      .onFailure(e -> LOGGER.error("Error while updating the realm by handle. Sql: \n" + sql, e));
  }

  private JsonObject getRealmAsJsonObject(Realm realm) {
    JsonObject data = JsonObject.mapFrom(realm);
    data.remove(Guid.GUID);
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
    Long id = apiApp.createGuidFromRealmOrOrganizationId(REALM_GUID_PREFIX, guid).getRealmOrOrganizationId();
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

  private <T extends Realm> Future<T> getRealmFromHandle(String realmHandle, Class<T> clazz) {


    String sql = "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " WHERE realm_handle = $1";
    return this.jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(realmHandle))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.failedFuture(new NotFoundException("the realm handle (" + realmHandle + ") was not found"));
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("the realm handle (" + realmHandle + ") returns more than one application"));
        }
        Row row = userRows.iterator().next();
        return this.getRealmFromDatabaseRow(row, clazz);

      }, err -> Future.failedFuture(new InternalException("The Realm by handle SQL returns an error (" + sql + ")", err)));
  }


  public <T extends Realm> Future<List<T>> getRealmsForOwner(OrganizationUser user, Class<T> realmClass) {

    return jdbcPool.preparedQuery("SELECT * FROM cs_realms.realm\n" +
        "where\n" +
        " " + REALM_ORGA_ID + " = $1")
      .execute(Tuple.of(user.getLocalId()))
      .compose(rows -> this.getRealmsFromRows(rows, realmClass),
        err -> Future.failedFuture(
          TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
            .setMessage("Unable to get the real of the organization user (" + user + ")")
            .build()
        ));
  }


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
    this.getGuidFromLong(realm);
    Long orgaId = row.getLong(REALM_ORGA_ID);
    Future<Organization> futureOrganization = apiApp.getOrganizationProvider().getById(orgaId);
    Long realmIdContactColumn = row.getLong(REALM_OWNER_ID_COLUMN);
    Realm eraldyRealm = EraldyRealm.get().getRealm();
    Future<User> futureOwnerUser = apiApp.getUserProvider().getUserById(realmIdContactColumn, eraldyRealm.getLocalId(), User.class, eraldyRealm);
    Long defaultAppId = row.getLong(REALM_DEFAULT_APP_ID);
    Future<App> futureApp;
    if (defaultAppId == null) {
      futureApp = Future.succeededFuture();
    } else {
      futureApp = apiApp.getAppProvider()
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

  private <T extends Realm> Future<T> getRealmFromGuid(String guid, Class<T> clazz) {
    long realmId;
    try {
      realmId = this.getGuidFromHash(guid).getRealmOrOrganizationId();
    } catch (CastException e) {
      return Future.failedFuture(e);
    }
    return getRealmFromId(realmId, clazz);
  }

  public Guid getGuidFromHash(String guid) throws CastException {
    return apiApp.createGuidFromHashWithOneId(REALM_GUID_PREFIX, guid);
  }


  public Future<List<RealmWithAppUris>> getRealmsWithAppUris() {
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
          realmWithDomains.setGuid(this.getGuidFromLong(realmId).toString());

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

  private Guid getGuidFromLong(Long realmId) {
    return apiApp.createGuidFromObjectId(REALM_GUID_PREFIX, realmId);
  }

  public Future<Realm> getRealmFromIdentifier(String realmIdentifier) {
    return getRealmFromIdentifier(realmIdentifier, Realm.class);
  }

  public <T extends Realm> Future<T> getRealmFromIdentifier(String realmIdentifier, Class<T> clazz) {

    if (this.isRealmGuidIdentifier(realmIdentifier)) {
      return getRealmFromGuid(realmIdentifier, clazz);
    }
    return getRealmFromHandle(realmIdentifier, clazz);

  }

  public boolean isRealmGuidIdentifier(String realmIdentifier) {
    return realmIdentifier.startsWith(REALM_GUID_PREFIX + Guid.GUID_SEPARATOR);
  }


  public Future<RealmAnalytics> getRealmAnalyticsFromIdentifier(String realmIdentifier) {
    return getRealmFromIdentifier(realmIdentifier, RealmAnalytics.class);
  }


  public boolean isIdentifierForRealm(String realmIdentifier, Realm realm) {
    if (realm.getGuid().equals(realmIdentifier)) {
      return true;
    }
    return realm.getHandle().equals(realmIdentifier);
  }

  public ObjectMapper getPublicJsonMapper() {
    return this.publicRealmJsonMapper;
  }

  /**
   * Wrapper around {@link #getRealmFromIdentifier(String)}
   * and fail if the realm was not found (ie null)
   *
   * @param realmIdentifier - the realm identifier
   * @param realmClass      - the realm class
   * @param <T>             - the realm class
   * @return the realm
   */
  public <T extends Realm> Future<T> getRealmFromIdentifierNotNull(String realmIdentifier, Class<T> realmClass) {
    return this.getRealmFromIdentifier(realmIdentifier, realmClass)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm identifier (" + realmIdentifier + ") was not found")
              .build()
          );
        }
        return Future.succeededFuture(realm);
      });
  }

}
