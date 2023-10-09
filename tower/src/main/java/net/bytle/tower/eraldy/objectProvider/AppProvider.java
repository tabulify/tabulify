package net.bytle.tower.eraldy.objectProvider;


import io.vertx.core.CompositeFuture;
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
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;

import static net.bytle.tower.util.JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;

/**
 * Manage the get/upsert of a {@link App} object asynchronously
 */
public class AppProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(AppProvider.class);

  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String COLUMN_PREFIX = "app";
  private static final String SHORT_PREFIX = "app";
  protected static final String TABLE_NAME = RealmProvider.TABLE_PREFIX + COLUMN_PART_SEP + COLUMN_PREFIX;
  public static final String REALM_ID_COLUMN = COLUMN_PREFIX + COLUMN_PART_SEP + RealmProvider.ID_COLUMN;
  public static final String USER_COLUMN = COLUMN_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;

  private static final Map<Vertx, AppProvider> mapPublisherProviderByVertx = new HashMap<>();

  /**
   * Domain is used as specified in
   * <a href="https://www.rfc-editor.org/rfc/rfc1034#section-3.5|Preferred">name syntax</a>
   */
  private static final String URI = "uri";
  protected static final String APP_ID_COLUMN = COLUMN_PREFIX + COLUMN_PART_SEP + "id";
  private static final String DATA_COLUMN = COLUMN_PREFIX + COLUMN_PART_SEP + "data";
  public static final String GUID = "guid";


  public static final String URI_COLUMN = COLUMN_PREFIX + COLUMN_PART_SEP + URI;
  private static final String CREATION_TIME = COLUMN_PREFIX + COLUMN_PART_SEP + CREATION_TIME_COLUMN_SUFFIX;

  private final Vertx vertx;


  public AppProvider(Vertx routingContext) {
    this.vertx = routingContext;
  }

  public static AppProvider create(Vertx vertx) {
    AppProvider appProvider;
    appProvider = AppProvider.mapPublisherProviderByVertx.get(vertx);
    if (appProvider != null) {
      return appProvider;
    }
    appProvider = new AppProvider(vertx);
    AppProvider.mapPublisherProviderByVertx.put(vertx, appProvider);
    return appProvider;
  }

  public App toPublicClone(App app) {
    return toClone(app, false);
  }

  /**
   * @param uri the authentication scope (an uri)
   * @throws IllegalStructure if the string is not an uri or an authentication scope
   */
  public static void validateDomainUri(URI uri) throws IllegalStructure {


    String scheme = uri.getScheme();
    if (scheme == null) {
      // without the scheme, the host is seen as path
      throw new IllegalStructure("The scheme is mandatory");
    }
    if (!scheme.equals("https") && HttpsCertificateUtil.createOrGet().isHttpsEnable()) {
      throw new IllegalStructure("The scheme (" + scheme + ") is not supported as app uri (only https)");
    }
    String query = uri.getQuery();
    if (query != null) {
      throw new IllegalStructure("An authentication scope URI should not have any query part. The following query was found (" + query + ").");
    }
    String fragment = uri.getFragment();
    if (fragment != null) {
      throw new IllegalStructure("An authentication scope URI should not have any fragment part. The following fragment was found (" + fragment + ").");
    }
  }

  /**
   * The function is created to be sure that the
   * identifier data (id) and the guid are consistent in the app object
   */
  private void computeTheGuid(App app) {
    if (app.getGuid() != null) {
      return;
    }
    Realm realm = app.getRealm();
    if (realm == null) {
      throw new InternalException("The realm should not be null to compute the guid");
    }
    Long realmId = realm.getLocalId();
    if (realmId == null) {
      throw new InternalException("The realm id should not be null to compute the guid");
    }
    //noinspection ConstantConditions
    if (app == null) {
      throw new InternalException("The app should not be null to compute the guid");
    }
    Long appId = app.getLocalId();
    if (appId == null) {
      throw new InternalException("The app id should not be null to compute the guid");
    }
    String hashGuid = Guid.createGuidStringFromRealmAndObjectId(SHORT_PREFIX, app.getRealm(), app.getLocalId(), vertx);
    app.setGuid(hashGuid);
  }

  /**
   * @param app the publication to upsert
   * @return the realm with the id
   */
  public Future<App> upsertApp(App app) {

    if (app.getUser() == null) {
      throw new InternalException("The app user is mandatory.");
    }

    Realm realm = app.getRealm();
    if (realm == null) {
      return Future.failedFuture(new InternalError("The realm is mandatory when upsert / inserting a publisher."));
    }

    if (app.getLocalId() != null) {
      return updateApp(app);
    }

    if (app.getUri() == null) {
      throw new IllegalArgumentException("The id or uri should be given.");
    }

    /**
     * Note: No SQL upsert because
     * it will advance the identifier sequence
     * (ie if the row exists, the insert fails
     * but the identifier sequence has been taken)
     * See the identifier.md file for more info.
     */
    return updateAppByUriAndGetRowSet(app)
      .compose(rowSet -> {

        if (rowSet.size() != 1) {
          return insertApp(app);
        }

        Long appId = rowSet.iterator().next().getLong(APP_ID_COLUMN);
        app.setLocalId(appId);
        return Future.succeededFuture(app);
      });

  }

  private Future<App> insertApp(App app) {

    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_ID_COLUMN + ",\n" +
      "  " + APP_ID_COLUMN + ",\n" +
      "  " + URI_COLUMN + ",\n" +
      "  " + USER_COLUMN + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + CREATION_TIME + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6)\n";

    // https://github.com/vert-x3/vertx-examples/blob/4.x/sql-client-examples/src/main/java/io/vertx/example/sqlclient/transaction_rollback/SqlClientExample.java
    return JdbcPoolCs.getJdbcPool(this.vertx)
      .withTransaction(sqlConnection ->
        SequenceProvider
          .getNextIdForTableAndRealm(sqlConnection, TABLE_NAME, app.getRealm().getLocalId())
          .compose(nextId -> {
            app.setLocalId(nextId);
            return sqlConnection
              .preparedQuery(sql)
              .execute(
                Tuple.of(
                  app.getRealm().getLocalId(),
                  app.getLocalId(),
                  app.getUri().toString(),
                  app.getUser().getLocalId(),
                  this.getDatabaseJsonObject(app),
                  DateTimeUtil.getNowUtc()
                )
              );
          })
          .onFailure(e -> LOGGER.error("App Insert Error:" + e.getMessage() + ". Sql: " + sql, e))
          .compose(rows -> Future.succeededFuture(app)));

  }

  private Future<App> updateApp(App app) {

    if (app.getLocalId() != null) {
      String updateSqlById = "UPDATE \n" +
        JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
        "set " +
        "  " + URI_COLUMN + " = $1,\n" +
        "  " + USER_COLUMN + " = $2, \n" +
        "  " + DATA_COLUMN + " = $3 \n" +
        "where\n" +
        "  " + REALM_ID_COLUMN + "= $4" +
        " AND" + APP_ID_COLUMN + "= $5";

      JsonObject databaseJsonObject = this.getDatabaseJsonObject(app);
      return JdbcPoolCs.getJdbcPool(this.vertx)
        .preparedQuery(updateSqlById)
        .execute(Tuple.of(
          app.getUri(),
          app.getUser().getLocalId(),
          databaseJsonObject,
          app.getRealm().getLocalId(),
          app.getLocalId())
        )
        .onFailure(e -> {
          throw new InternalException("Error on app update by Id:" + e.getMessage() + ". Sql: " + updateSqlById, e);
        })
        .compose(ok -> Future.succeededFuture(app));
    }

    if (app.getUri() == null) {
      InternalException internalException = new InternalException("The app id or uri is mandatory to update an app");
      return Future.failedFuture(internalException);
    }
    return updateAppByUriAndGetRowSet(app)
      .compose(rowSet -> {
          if (rowSet.size() != 1) {
            InternalException internalException = new InternalException("No app was updated with the uri (" + app.getUri() + ") and realm (" + app.getRealm().getHandle() + ")");
            return Future.failedFuture(internalException);
          }
          Long appId = rowSet.iterator().next().getLong(APP_ID_COLUMN);
          app.setLocalId(appId);
          return Future.succeededFuture(app);
        }
      );
  }

  private Future<RowSet<Row>> updateAppByUriAndGetRowSet(App app) {
    String updateSqlByUri = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " \n" +
      "set \n" +
      "  " + USER_COLUMN + " = $1, \n" +
      "  " + DATA_COLUMN + " = $2 \n" +
      "where\n" +
      "  " + REALM_ID_COLUMN + " = $3\n" +
      " AND " + URI_COLUMN + " = $4\n" +
      " RETURNING " + APP_ID_COLUMN;

    return JdbcPoolCs.getJdbcPool(this.vertx)
      .preparedQuery(updateSqlByUri)
      .execute(Tuple.of(
          app.getUser().getLocalId(),
          this.getDatabaseJsonObject(app),
          app.getRealm().getLocalId(),
          app.getUri().toString()
        )
      )
      .onFailure(t -> LOGGER.error("Error while updating the app by uri and realm. Sql: \n" + updateSqlByUri, t));
  }

  /**
   * @param app - the app to check
   * @return true or false
   */
  @SuppressWarnings("unused")
  private Future<Boolean> exists(App app) {
    String sql;
    Future<RowSet<Row>> futureResponse;
    if (app.getLocalId() != null) {
      sql = "select " + APP_ID_COLUMN +
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
        " where " +
        " " + REALM_ID_COLUMN + " = ?" +
        " AND " + APP_ID_COLUMN + " = ?";
      futureResponse = JdbcPoolCs.getJdbcPool(this.vertx)
        .preparedQuery(sql)
        .execute(Tuple.of(
          app.getRealm().getLocalId(),
          app.getLocalId())
        );
    } else {
      java.net.URI appUri = app.getUri();
      if (appUri == null) {
        String failureMessage = "An id, or uri should be given to check the existence of an app";
        InternalException internalException = new InternalException(failureMessage);
        return Future.failedFuture(internalException);
      }
      sql = "select " + APP_ID_COLUMN +
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
        " where " +
        " " + REALM_ID_COLUMN + " = ?" +
        " AND " + URI_COLUMN + " = ?";
      futureResponse = JdbcPoolCs.getJdbcPool(this.vertx)
        .preparedQuery(sql)
        .execute(Tuple.of(
          app.getRealm().getLocalId(),
          appUri
        ));
    }
    return futureResponse
      .onFailure(e -> {
        throw new InternalException(e);
      })
      .compose(rows -> {
        if (rows.size() == 1) {
          return Future.succeededFuture(true);
        } else {
          return Future.succeededFuture(false);
        }
      });
  }

  /**
   * @param app - the app
   * @return the PgJson object to insert in the database
   */
  private JsonObject getDatabaseJsonObject(App app) {

    JsonObject data = JsonObject.mapFrom(app);
    data.remove(APP_ID_COLUMN);
    data.remove(GUID);
    data.remove(URI);
    data.remove(RealmProvider.TABLE_PREFIX);
    data.remove("user");
    return data;
  }

  /**
   * @param realm - the realmId
   * @return the realm
   */
  public Future<List<App>> getApps(Realm realm) {

    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    return jdbcPool.preparedQuery(
        "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME
          + " where " + REALM_ID_COLUMN + " = $1")
      .execute(Tuple.of(realm.getLocalId()))
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(rows -> {

        List<Future<App>> apps = new ArrayList<>();
        for (Row row : rows) {

          Future<App> app = getFromRow(row, realm);
          apps.add(app);

        }
        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future
          .all(apps)
          .onFailure(t -> LOGGER.error("Error on getApps", t))
          .map(CompositeFuture::<App>list);
      });
  }

  private Future<App> getFromRow(Row row, Realm realm) {
    Long userId = row.getLong(USER_COLUMN);
    Future<User> userFuture = UserProvider
      .createFrom(vertx)
      .getUserById(userId, realm);
    Future<Realm> realmFuture;
    Long realmId = row.getLong(REALM_ID_COLUMN);
    RealmProvider realmProvider = RealmProvider.createFrom(vertx);
    //noinspection ConstantConditions
    if (realm == null) {
      realmFuture = realmProvider.getRealmFromId(realmId);
    } else {
      if (!Objects.equals(realmId, realm.getLocalId())) {
        InternalException internalException = new InternalException("The realm in the database (" + realmId + ") is inconsistent with the realm provided (" + realm.getLocalId() + ")");
        return Future.failedFuture(internalException);
      }
      // We clone the realm to avoid a recursion in Jackson
      // through reference chain:
      // net.bytle.tower.eraldy.model.openapi.User["realm"]
      //->net.bytle.tower.eraldy.model.openapi.Realm["defaultApp"]
      //->net.bytle.tower.eraldy.model.openapi.App["user"]
      //->net.bytle.tower.eraldy.model.openapi.User["realm"]
      //Realm realmClone = realmProvider.clone(realm);
      realmFuture = Future.succeededFuture(realm);
    }
    return Future.all(userFuture, realmFuture)
      .onFailure(t -> LOGGER.error("AppProvider getFromRows Error", t))
      .compose(compositeFuture -> {
        User user = compositeFuture.resultAt(0);
        Realm realmResult = compositeFuture.resultAt(1);
        String uri = row.getString(URI_COLUMN);
        JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);
        App app = Json.decodeValue(jsonAppData.toBuffer(), App.class);
        app.setUser(user);
        app.setUri(java.net.URI.create(uri));
        app.setRealm(realmResult);
        Long appId = row.getLong(APP_ID_COLUMN);
        app.setLocalId(appId);
        this.computeTheGuid(app);
        return Future.succeededFuture(app);
      });

  }


  public Future<App> getAppByUri(URI uri, Realm realm) {
    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    return jdbcPool.preparedQuery(
        "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME
          + " WHERE " + URI_COLUMN + " = $1 "
          + "and " + REALM_ID_COLUMN + " = $2 ")
      .execute(Tuple.of(
        uri.toString(),
        realm.getLocalId()
      ))
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getFromRow(row, realm);
      });
  }


  /**
   * @param appPostBody - the post object
   * @return the app in a future
   */
  public Future<App> postApp(AppPostBody appPostBody) {

    Realm requestedRealm = new Realm();
    requestedRealm.setGuid(appPostBody.getRealmGuid());
    requestedRealm.setHandle(appPostBody.getRealmHandle());

    App requestedApp = new App();
    requestedApp.setGuid(appPostBody.getAppGuid());
    requestedApp.setUri(appPostBody.getAppUri());

    return AppProvider.create(vertx)
      .getRealmAndUpdateIdEventuallyFromRequested(requestedRealm, requestedApp)
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(realm -> {

        if (realm == null) {
          if (requestedRealm.getGuid() != null) {
            throw ValidationException.create("The realm was not found with the guid (" + requestedRealm.getGuid() + ")", "realmGuid", requestedRealm.getGuid());
          }
          throw ValidationException.create("The realm was not found with the handle (" + requestedRealm.getHandle() + ")", "realmHandle", requestedRealm.getHandle());
        }

        App app = new App();
        app.setUri(appPostBody.getAppUri());
        app.setName(appPostBody.getAppName());
        app.setLogo(appPostBody.getAppLogo());
        app.setHome(appPostBody.getAppHome());
        app.setPrimaryColor(appPostBody.getAppPrimaryColor());
        app.setTerms(appPostBody.getAppTerms());
        app.setRealm(realm);
        String appGuid = app.getGuid();

        if (appGuid != null) {
          app.setGuid(appGuid);
          long appId;
          try {
            appId = Guid.getIdFromGuidAndRealm(appGuid, realm, vertx);
          } catch (CastException e) {
            throw ValidationException.create("The app guid is not valid", "appGuid", appGuid);
          }
          app.setLocalId(appId);
        }
        /**
         * User
         */
        Long userId = null;
        String userEmail = appPostBody.getUserEmail();
        String userGuid = appPostBody.getUserGuid();
        if (userGuid != null) {
          try {
            userId = Guid.getIdFromGuidAndRealm(userGuid, realm, vertx);
          } catch (CastException e) {
            throw ValidationException.create("The user guid is not valid", "userGuid", userGuid);
          }
        }
        User userToGetOrCreate = new User();
        userToGetOrCreate.setLocalId(userId);
        userToGetOrCreate.setEmail(userEmail);
        userToGetOrCreate.setRealm(realm);
        return UserProvider.createFrom(vertx)
          .getOrCreateUserFromEmail(userToGetOrCreate)
          .onFailure(t -> LOGGER.error("Error on app upsert", t))
          .compose(user -> {
            app.setUser(user);
            return AppProvider.create(vertx)
              .upsertApp(app);
          });
      });

  }

  private Future<Realm> getRealmAndUpdateIdEventuallyFromRequested(Realm requestedRealm, App requestedApp) {
    String appGuid = requestedApp.getGuid();
    java.net.URI appUri = requestedApp.getUri();
    String realmHandle = requestedRealm.getHandle();
    String realmGuid = requestedRealm.getGuid();
    Future<Realm> realmFuture;
    if (appGuid == null) {
      if (appUri == null) {
        throw ValidationException.create("The appGuid and the appUri cannot be both null", "appGuid", null);
      }
      if (realmHandle == null && realmGuid == null) {
        throw ValidationException.create("With the appUri, a realm Handle or Guid should be given", "realmHandle", null);
      }
      realmFuture = RealmProvider.createFrom(vertx)
        .getRealmFromGuidOrHandle(realmGuid, realmHandle);
    } else {

      Guid guid;
      try {
        guid = Guid.createObjectFromRealmIdAndOneObjectId(SHORT_PREFIX, appGuid, vertx);
      } catch (CastException e) {
        return Future.failedFuture(new IllegalArgumentException("The app guid is not valid (" + appGuid + ")"));
      }

      long realmId = guid.getRealmId();

      realmFuture = RealmProvider.createFrom(vertx)
        .getRealmFromId(realmId);

      long userIdFromGuid = guid.getFirstObjectId();
      requestedApp.setLocalId(userIdFromGuid);

    }
    return realmFuture;
  }

  public Future<App> getAppByGuid(String appGuid) {
    Guid guid;
    try {
      guid = Guid.createObjectFromRealmIdAndOneObjectId(SHORT_PREFIX, appGuid, vertx);
    } catch (CastException e) {
      throw ValidationException.create("The appGuid is not valid", "appGuid", appGuid);
    }
    return RealmProvider.createFrom(vertx)
      .getRealmFromId(guid.getRealmId())
      .compose(realm -> getAppById(guid.getFirstObjectId(), realm));
  }

  Future<App> getAppById(long appId, Realm realm) {
    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    return jdbcPool.preparedQuery(
        "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME
          + " WHERE " + APP_ID_COLUMN + " = $1 "
          + " AND " + REALM_ID_COLUMN + " = $2 "
      )
      .execute(Tuple.of(appId, realm.getLocalId()))
      .onFailure(t -> LOGGER.error("Error with Sql to retrieve an app by Guid. Error: " + t.getMessage(), t))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getFromRow(row, realm);
      });
  }

  public App toTemplateClone(App app) {
    return toClone(app, true);
  }

  private App toClone(App app, boolean template) {
    App cloneApp = JsonObject.mapFrom(app).mapTo(App.class);
    cloneApp.setLocalId(null);
    UserProvider userProvider = UserProvider.createFrom(vertx);
    User owner = app.getUser();
    if (template) {
      owner = userProvider.toTemplateCloneWithoutRealm(owner);
    } else {
      owner = userProvider.toPublicCloneWithoutRealm(owner);
    }
    cloneApp.setUser(owner);
    cloneApp.setRealm(null);
    return cloneApp;
  }

}
