package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import io.vertx.sqlclient.*;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.bytle.vertx.db.JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
import static net.bytle.vertx.db.JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;

/**
 * Manage the get/upsert of a {@link App} object asynchronously
 */
public class AppProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(AppProvider.class);

  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String APP_COLUMN_PREFIX = "app";
  public static final String APP_GUID_PREFIX = "app";
  public static final String APP_TABLE_NAME = RealmProvider.TABLE_PREFIX + COLUMN_PART_SEP + APP_COLUMN_PREFIX;
  public static final String APP_REALM_ID_COLUMN = APP_COLUMN_PREFIX + COLUMN_PART_SEP + "realm_id";
  public static final String APP_USER_COLUMN = APP_COLUMN_PREFIX + COLUMN_PART_SEP + "owner" + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  public static final String APP_ORG_COLUMN = APP_COLUMN_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN;


  public static final String APP_ID_COLUMN = APP_COLUMN_PREFIX + COLUMN_PART_SEP + "id";

  public static final String APP_HANDLE_COLUMN = APP_COLUMN_PREFIX + COLUMN_PART_SEP + "handle";
  private static final String APP_CREATION_TIME = APP_COLUMN_PREFIX + COLUMN_PART_SEP + CREATION_TIME_COLUMN_SUFFIX;
  private static final String APP_NAME_COLUMN = APP_COLUMN_PREFIX + COLUMN_PART_SEP + "name";
  private static final String APP_HOME_COLUMN = APP_COLUMN_PREFIX + COLUMN_PART_SEP + "home";
  private static final String APP_MODIFICATION_TIME = APP_COLUMN_PREFIX + COLUMN_PART_SEP + MODIFICATION_TIME_COLUMN_SUFFIX;

  private final EraldyApiApp apiApp;
  private final Pool jdbcPool;
  private final JsonMapper apiMapper;
  private final String updateSqlById;
  private final String insertSql;
  private final String updateSqlByHandle;


  public AppProvider(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();
    this.apiMapper = apiApp.getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .build();


    this.updateSqlById = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME + "\n" +
      "set\n" +
      "  " + APP_HANDLE_COLUMN + " = $1,\n" +
      "  " + APP_NAME_COLUMN + " = $2, \n" +
      "  " + APP_HOME_COLUMN + " = $3, \n" +
      "  " + APP_USER_COLUMN + " = $4, \n" +
      "  " + APP_MODIFICATION_TIME + " = $5 \n" +
      "where\n" +
      "  " + APP_REALM_ID_COLUMN + "= $6\n" +
      " AND " + APP_ID_COLUMN + "= $7";

    updateSqlByHandle = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME + " \n" +
      "set \n" +
      "  " + APP_USER_COLUMN + " = $1, \n" +
      "  " + APP_NAME_COLUMN + " = $2, \n" +
      "  " + APP_HOME_COLUMN + " = $3, \n" +
      "  " + APP_MODIFICATION_TIME + " = $4 \n" +
      "where\n" +
      "  " + APP_REALM_ID_COLUMN + " = $5\n" +
      " AND " + APP_HANDLE_COLUMN + " = $6\n" +
      " RETURNING " + APP_ID_COLUMN;

    this.insertSql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME + " (\n" +
      "  " + APP_REALM_ID_COLUMN + ",\n" +
      "  " + APP_ID_COLUMN + ",\n" +
      "  " + APP_HANDLE_COLUMN + ",\n" +
      "  " + APP_NAME_COLUMN + ", \n" +
      "  " + APP_HOME_COLUMN + ",\n" +
      "  " + APP_ORG_COLUMN + ",\n" +
      "  " + APP_USER_COLUMN + ",\n" +
      "  " + APP_CREATION_TIME + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6, $7, $8)\n";
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
  private void updateGuid(App app) {
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
    String hashGuid = this.apiApp.createGuidFromRealmAndObjectId(APP_GUID_PREFIX, app.getRealm(), app.getLocalId()).toString();
    app.setGuid(hashGuid);
  }

  /**
   * @param app the publication to upsert
   * @return the realm with the id
   */
  public Future<App> upsertApp(App app) {

    if (app.getOwnerUser() == null) {
      throw new InternalException("The app user is mandatory.");
    }

    Realm realm = app.getRealm();
    if (realm == null) {
      return Future.failedFuture(new InternalError("The realm is mandatory when upsert / inserting a publisher."));
    }

    if (app.getLocalId() != null) {
      return updateApp(app);
    }

    if (app.getHandle() == null) {
      throw new IllegalArgumentException("The id or uri should be given.");
    }

    /**
     * Note: No SQL upsert because
     * it will advance the identifier sequence
     * (ie if the row exists, the insert fails
     * but the identifier sequence has been taken)
     * See the identifier.md file for more info.
     */
    return updateAppByHandle(app)
      .compose(rowSet -> {

        if (rowSet.size() == 0) {
          return insertApp(app);
        }

        Long appId = rowSet.iterator().next().getLong(APP_ID_COLUMN);
        app.setLocalId(appId);
        return Future.succeededFuture(app);
      });

  }

  private Future<App> insertApp(App app) {

    return this.jdbcPool.withTransaction(sqlConnection -> insertApp(app, sqlConnection));


  }

  private Future<App> updateApp(App app) {

    if (app.getLocalId() != null) {

      return jdbcPool
        .preparedQuery(updateSqlById)
        .execute(Tuple.of(
          app.getHandle(),
          app.getOwnerUser().getLocalId(),
          app.getName(),
          app.getHome(),
          DateTimeService.getNowInUtc(),
          app.getRealm().getLocalId(),
          app.getLocalId())
        )
        .recover(e -> Future.failedFuture(new InternalException("Error on app update by Id:" + e.getMessage() + ". Sql: " + updateSqlById, e)))
        .compose(ok -> {
          this.updateGuid(app);
          return Future.succeededFuture(app);
        });
    }

    if (app.getHandle() == null) {
      InternalException internalException = new InternalException("The app id or handle is mandatory to update an app");
      return Future.failedFuture(internalException);
    }
    return updateAppByHandle(app)
      .compose(rowSet -> {
          if (rowSet.size() != 1) {
            InternalException internalException = new InternalException("No app was updated with the uri (" + app.getHandle() + ") and realm (" + app.getRealm().getHandle() + ")");
            return Future.failedFuture(internalException);
          }
          Long appId = rowSet.iterator().next().getLong(APP_ID_COLUMN);
          app.setLocalId(appId);
          this.updateGuid(app);
          return Future.succeededFuture(app);
        }
      );
  }


  private Future<RowSet<Row>> updateAppByHandle(App app) {


    return jdbcPool
      .preparedQuery(updateSqlByHandle)
      .execute(Tuple.of(
          app.getOwnerUser().getLocalId(),
          app.getName(),
          app.getHome(),
          DateTimeService.getNowInUtc(),
          app.getRealm().getLocalId(),
          app.getHandle()
        )
      )
      .onFailure(t -> LOGGER.error("Error while updating the app by uri and realm. Sql: \n" + updateSqlByHandle, t));
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
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME +
        " where " +
        " " + APP_REALM_ID_COLUMN + " = ?" +
        " AND " + APP_ID_COLUMN + " = ?";
      futureResponse = jdbcPool
        .preparedQuery(sql)
        .execute(Tuple.of(
          app.getRealm().getLocalId(),
          app.getLocalId())
        );
    } else {
      String appHandle = app.getHandle();
      if (appHandle == null) {
        String failureMessage = "An id, or handle should be given to check the existence of an app";
        InternalException internalException = new InternalException(failureMessage);
        return Future.failedFuture(internalException);
      }
      sql = "select " + APP_ID_COLUMN +
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME +
        " where " +
        " " + APP_REALM_ID_COLUMN + " = ?" +
        " AND " + APP_HANDLE_COLUMN + " = ?";
      futureResponse = jdbcPool
        .preparedQuery(sql)
        .execute(Tuple.of(
          app.getRealm().getLocalId(),
          appHandle
        ));
    }
    return futureResponse
      .recover(e -> Future.failedFuture(new InternalException("App exists error with the following sql: " + sql, e)))
      .compose(rows -> {
        if (rows.size() == 1) {
          return Future.succeededFuture(true);
        } else {
          return Future.succeededFuture(false);
        }
      });
  }


  /**
   * @param realm - the realmId
   * @return the realm
   */
  public Future<List<App>> getApps(Realm realm) {

    return jdbcPool
      .preparedQuery(
        "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME
          + " where " + APP_REALM_ID_COLUMN + " = $1")
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
    Long userId = row.getLong(APP_USER_COLUMN);
    Future<OrgaUser> userFuture = apiApp
      .getOrganizationUserProvider()
      .getOrganizationUserByLocalId(userId);
    Future<Realm> realmFuture;
    Long realmId = row.getLong(APP_REALM_ID_COLUMN);
    RealmProvider realmProvider = this.apiApp.getRealmProvider();
    //noinspection ConstantConditions
    if (realm == null) {
      realmFuture = realmProvider.getRealmFromLocalId(realmId);
    } else {
      if (!Objects.equals(realmId, realm.getLocalId())) {
        InternalException internalException = new InternalException("The realm in the database (" + realmId + ") is inconsistent with the realm provided (" + realm.getLocalId() + ")");
        return Future.failedFuture(internalException);
      }
      realmFuture = Future.succeededFuture(realm);
    }
    return Future
      .all(userFuture, realmFuture)
      .recover(t -> Future.failedFuture(new InternalException("AppProvider getFromRows Error (" + t.getMessage() + ")", t)))
      .compose(compositeFuture -> {
        OrgaUser orgaUser = compositeFuture.resultAt(0);
        Realm realmResult = compositeFuture.resultAt(1);
        String uri = row.getString(APP_HANDLE_COLUMN);

        App app = new App();

        /**
         * Identifiers
         */
        Long appId = row.getLong(APP_ID_COLUMN);
        app.setLocalId(appId);
        app.setRealm(realmResult);
        this.updateGuid(app);
        app.setHandle(uri);

        /**
         * Scalars
         */
        app.setName(row.getString(APP_NAME_COLUMN));
        String home = row.getString(APP_HOME_COLUMN);
        if (home != null) {
          try {
            app.setHome(java.net.URI.create(home));
          } catch (IllegalArgumentException e) {
            // should not happen as we are responsible for the insertion
            throw new InternalException("The home realm value is not a valid URI", e);
          }
        }

        /**
         * Foreign Objects
         */
        app.setOwnerUser(orgaUser);

        return Future.succeededFuture(app);
      });

  }


  public Future<App> getAppByHandle(String handle, Realm realm) {

    return this.jdbcPool.withConnection(sqlConnection -> getAppByHandle(handle, realm, sqlConnection));
  }


  /**
   * @param appPostBody - the post object
   * @param realm       - the realm
   * @return the app in a future
   */
  public Future<App> postApp(AppPostBody appPostBody, Realm realm) {


    App app = new App();
    app.setGuid(appPostBody.getAppGuid());
    app.setHandle(appPostBody.getAppHandle());
    URI appUri = appPostBody.getAppHome();
    try {
      validateDomainUri(appUri);
    } catch (IllegalStructure e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setMessage("The App Uri (" + appUri + ") is not a valid URI identifier. " + e.getMessage())
        // request is good data is not
        .setType(TowerFailureTypeEnum.BAD_STRUCTURE_422)
        .setCauseException(e)
        .build()
      );
    }
    app.setUri(appUri);
    app.setName(appPostBody.getAppName());
    app.setHome(appPostBody.getAppHome());
    app.setLogo(appPostBody.getAppLogo());
    app.setPrimaryColor(appPostBody.getAppPrimaryColor());
    app.setTerms(appPostBody.getAppTerms());
    app.setRealm(realm);
    String appGuid = app.getGuid();

    if (appGuid != null) {
      app.setGuid(appGuid);
      long appId;
      try {
        appId = this.getGuidFromHash(appGuid)
          .getAppLocalId(realm.getLocalId());
      } catch (CastException e) {
        throw ValidationException.create("The app guid is not valid", "appGuid", appGuid);
      }
      app.setLocalId(appId);
    }
    /**
     * User
     */
    String userIdentifier = appPostBody.getUserIdentifier();
    Future<OrgaUser> futureOrgUser;
    if (userIdentifier == null) {
      futureOrgUser = Future.succeededFuture();
    } else {
      futureOrgUser = apiApp.getOrganizationUserProvider()
        .getOrganizationUserByIdentifier(userIdentifier);
    }
    return futureOrgUser
      .compose(organizationUser -> {
        if (organizationUser != null) {
          app.setOwnerUser(organizationUser);
        }
        return upsertApp(app);
      });

  }

  public AppGuid getGuidFromHash(String appGuid) throws CastException {
    return this.apiApp
      .getHttpServer()
      .getServer()
      .getJacksonMapperManager()
      .getDeserializer(AppGuid.class)
      .deserialize(appGuid);
  }

  public Future<App> getAppByGuid(AppGuid guid, Realm realm) {

    Future<Realm> realmFuture;
    if (realm != null) {
      realmFuture = Future.succeededFuture(realm);
    } else {
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromLocalId(guid.getRealmId());
    }
    return realmFuture
      .compose(realmRes -> {
        long appId = guid.getAppLocalId(realmRes.getLocalId());
        return getAppById(appId, realmRes);
      });
  }

  public Future<App> getAppById(long appId, Realm realm) {
    return this.jdbcPool.withConnection(sqlConnection -> getAppById(appId, realm, sqlConnection));
  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  /**
   * Getsert: get or insert an app with a local id
   */
  public Future<App> getsertOnStartup(App app) {
    Future<App> selectApp;
    if (app.getLocalId() != null) {
      selectApp = this.getAppById(app.getLocalId(), app.getRealm());
    } else {
      String handle = app.getHandle();
      if (handle == null) {
        return Future.failedFuture(new InternalException("The app to getsert should have an identifier (local id, or handle)"));
      }
      selectApp = this.getAppByHandle(handle, app.getRealm());
    }
    return selectApp
      .compose(selectedApp -> {
        if (selectedApp != null) {
          return Future.succeededFuture(selectedApp);
        }
        return this.insertApp(app);
      });
  }

  /**
   * @param appHandle     - the app handle
   * @param realm         - the realm to insert to
   * @param sqlConnection - a connection with or without transaction
   * @return the app or null
   */
  private Future<App> getAppByHandle(String appHandle, Realm realm, SqlConnection sqlConnection) {
    return sqlConnection.preparedQuery(
        "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME
          + " WHERE " + APP_HANDLE_COLUMN + " = $1 "
          + "and " + APP_REALM_ID_COLUMN + " = $2 ")
      .execute(Tuple.of(
        appHandle,
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

  private Future<App> getAppById(Long appId, Realm realm, SqlConnection sqlConnection) {
    return sqlConnection.preparedQuery(
        "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + APP_TABLE_NAME
          + " WHERE " + APP_ID_COLUMN + " = $1 "
          + " AND " + APP_REALM_ID_COLUMN + " = $2 "
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

  /**
   * @param app           - the app to insert (realm is mandatory)
   * @param sqlConnection - the sql connection (with or without a transaction)
   * @return the app given with an id and a guid
   */
  private Future<App> insertApp(App app, SqlConnection sqlConnection) {

    return this.apiApp.getRealmSequenceProvider()
      .getNextIdForTableAndRealm(sqlConnection, app.getRealm(), APP_TABLE_NAME)
      .compose(finalAppId -> {
        Long askedLocalId = app.getLocalId();
        if (askedLocalId != null && !askedLocalId.equals(finalAppId)) {
          /**
           * When we insert a startup
           * with {@link #getsertOnStartup(App, SqlConnection)}
           * where there is no data
           */
          return Future.failedFuture("The asked local id (" + askedLocalId + ") is different of the id given (" + finalAppId + "). The insertion order in the Eraldy model is not good.");
        }
        app.setLocalId(finalAppId);
        this.updateGuid(app);
        return sqlConnection
          .preparedQuery(insertSql)
          .execute(
            Tuple.of(
              app.getRealm().getLocalId(),
              app.getLocalId(),
              app.getHandle(),
              app.getName(),
              app.getHome().toString(),
              app.getOwnerUser().getOrganization().getLocalId(),
              app.getOwnerUser().getLocalId(),
              DateTimeService.getNowInUtc()
            )
          );
      })
      .recover(e -> Future.failedFuture(new InternalException("App Insert Error:" + e.getMessage() + ". Sql: " + insertSql, e)))
      .compose(rows -> Future.succeededFuture(app));
  }

  public Future<App> getAppByIdentifier(String appIdentifier, Realm realm) {
    try {
      AppGuid guid = this.getGuidFromHash(appIdentifier);
      return this.getAppByGuid(guid, realm);
    } catch (CastException e) {
      return getAppByHandle(appIdentifier, realm);
    }

  }

  public App getRequestingApp(RoutingContext routingContext) {
    return this.apiApp.getAuthClientIdHandler().getRequestingApp(routingContext);
  }


}
