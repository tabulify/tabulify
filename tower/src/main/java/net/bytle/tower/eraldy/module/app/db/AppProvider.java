package net.bytle.tower.eraldy.module.app.db;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.app.inputs.AppInputProps;
import net.bytle.tower.eraldy.module.app.jackson.JacksonAppGuidDeserializer;
import net.bytle.tower.eraldy.module.app.jackson.JacksonAppGuidSerializer;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.realm.db.RealmCols;
import net.bytle.tower.eraldy.module.realm.db.RealmProvider;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.type.Color;
import net.bytle.type.Handle;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;
import net.bytle.vertx.guid.GuidDeSer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manage the get/upsert of a {@link App} object asynchronously
 */
public class AppProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(AppProvider.class);

  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  protected static final String APP_COLUMN_PREFIX = "app";
  public static final String APP_GUID_PREFIX = "app";
  public static final String APP_TABLE_NAME = RealmProvider.TABLE_PREFIX + COLUMN_PART_SEP + APP_COLUMN_PREFIX;

  private final EraldyApiApp apiApp;
  private final Pool jdbcPool;
  private final JdbcTable appTable;


  public AppProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();

    GuidDeSer appGuidDeSer = apiApp.getHttpServer().getServer().getHashId().getGuidDeSer(AppProvider.APP_GUID_PREFIX, 2);
    this.apiApp.getHttpServer().getServer().getJacksonMapperManager()
      .addDeserializer(AppGuid.class, new JacksonAppGuidDeserializer(appGuidDeSer))
      .addSerializer(AppGuid.class, new JacksonAppGuidSerializer(appGuidDeSer));

    this.appTable = JdbcTable.build(jdbcSchema, APP_TABLE_NAME, AppCols.values())
      .addPrimaryKeyColumn(AppCols.ID)
      .addPrimaryKeyColumn(AppCols.REALM_ID)
      .addUniqueKeyColumns(AppCols.REALM_ID, AppCols.HANDLE)
      .addForeignKeyColumn(AppCols.REALM_ID, RealmCols.ID)
      .build();


  }


  /**
   * @param uri the authentication scope (an uri)
   * @throws IllegalStructure if the string is not an uri or an authentication scope
   */
  @SuppressWarnings("unused")
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
  public void updateGuid(App app, Long appLocalId) {
    //noinspection ConstantConditions
    assert app != null : "The app should not be null to compute the guid";

    if (app.getGuid() != null) {
      return;
    }
    Realm realm = app.getRealm();
    if (realm == null) {
      throw new InternalException("The realm should not be null to compute the guid");
    }
    RealmGuid realmGuid = realm.getGuid();
    if (realmGuid == null) {
      throw new InternalException("The realm guid should not be null to compute the app guid");
    }

    AppGuid appGuid = new AppGuid();
    appGuid.setRealmId(realmGuid.getLocalId());
    appGuid.setLocalId(appLocalId);
    app.setGuid(appGuid);
  }


  public Future<App> updateApp(App app, AppInputProps appInputProps) {

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.appTable)
      .addPredicateColumn(AppCols.ID, app.getGuid().getLocalId())
      .addPredicateColumn(AppCols.REALM_ID, app.getGuid().getRealmId())
      .addReturningColumn(AppCols.ID);

    String newName = appInputProps.getName();
    if (appInputProps.isNameSet() && !Objects.equals(app.getName(), newName)) {
      app.setName(newName);
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.NAME, app.getName());
    }

    Handle newHandle = appInputProps.getHandle();
    if (appInputProps.isHandleSet() && !Objects.equals(app.getHandle(), newHandle)) {
      app.setHandle(newHandle);
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.HANDLE, app.getHandle().getValue());
    }


    URL newLogo = appInputProps.getLogo();
    if (appInputProps.isLogoSet() && !Objects.equals(app.getLogo(), newLogo)) {
      app.setLogo(newLogo);
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.LOGO, app.getLogo().toString());
    }

    URL newTerms = appInputProps.getTermsOfServices();
    if (appInputProps.isTermsSet() && !Objects.equals(app.getTermsOfServices(), newTerms)) {
      app.setTermsOfServices(newTerms);
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.TERM_OF_SERVICE, app.getTermsOfServices().toString());
    }

    URL newHome = appInputProps.getHome();
    if (appInputProps.isHomeSet() && !Objects.equals(app.getHome(), newHome)) {
      app.setHome(newHome);
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.HOME, app.getHome().toString());
    }

    String newSlogan = appInputProps.getSlogan();
    if (appInputProps.isSloganSet() && !Objects.equals(app.getSlogan(), newSlogan)) {
      app.setSlogan(newSlogan);
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.SLOGAN, app.getSlogan());
    }

    Color newPrimaryColor = appInputProps.getPrimaryColor();
    if (appInputProps.isPrimaryColorSet() && !Objects.equals(app.getPrimaryColor(), newPrimaryColor)) {
      app.setPrimaryColor(newPrimaryColor);
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.PRIMARY_COLOR, app.getPrimaryColor().getValue());
    }

    OrgaUserGuid newOwnerUserGuid = appInputProps.getOwnerUserGuid();
    if (appInputProps.isOwnerUserGuidSet() && !Objects.equals(app.getOwnerUser().getGuid(), newOwnerUserGuid)) {

      OrgaUser newOwner = this.apiApp.getOrganizationUserProvider().toNewOwnerFromActualOwner(newOwnerUserGuid, app.getOwnerUser());
      app.setOwnerUser(newOwner);
      // We don't update the owner orga id and owner realm id
      // as we don't support change of organization
      jdbcUpdate.setUpdatedColumnWithValue(AppCols.OWNER_USER_ID, app.getOwnerUser().getGuid().getUserId());

    }

    if (jdbcUpdate.hasNoColumnToUpdate()) {
      return Future.succeededFuture(app);
    }

    app.setModificationTime(DateTimeService.getNowInUtc());
    jdbcUpdate.setUpdatedColumnWithValue(AppCols.MODIFICATION_TIME, app.getModificationTime());

    return jdbcUpdate
      .execute()
      .compose(rowSet -> {
          if (rowSet.size() != 1) {
            InternalException internalException = new InternalException("No app was updated with the guid (" + app.getGuid() + ")");
            return Future.failedFuture(internalException);
          }
          return Future.succeededFuture(app);
        }
      );
  }


  /**
   * @param realm - the realmId
   * @return the realm
   */
  public Future<List<App>> getAppsForRealm(Realm realm) {

    return JdbcSelect.from(this.appTable)
      .addEqualityPredicate(AppCols.REALM_ID, realm.getGuid().getLocalId())
      .execute()
      .compose(rows -> {

        List<App> apps = new ArrayList<>();
        for (JdbcRow row : rows) {

          App app = getFromRow(row, realm);
          apps.add(app);

        }
        return Future.succeededFuture(apps);
      });
  }

  private App getFromRow(JdbcRow row, Realm realm) {

    App app = new App();

    /**
     * Realm
     */
    Long realmId = row.getLong(AppCols.REALM_ID);
    if (!Objects.equals(realmId, realm.getGuid().getLocalId())) {
      throw new InternalException("The realm in the database (" + realmId + ") is inconsistent with the realm provided (" + realm.getGuid() + ")");
    }
    app.setRealm(realm);

    /**
     * Ownership
     */
    OrgaUserGuid ownerUserGuid = new OrgaUserGuid.Builder()
      .setRealmId(row.getLong(AppCols.OWNER_REALM_ID))
      .setUserId(row.getLong(AppCols.OWNER_USER_ID))
      .setOrgaId(row.getLong(AppCols.OWNER_ORGA_ID))
      .build();
    OrgaUser ownerUser = this.apiApp.getOrganizationUserProvider().toNewUserFromGuid(ownerUserGuid);
    app.setOwnerUser(ownerUser);


    /**
     * Identifiers
     */
    Long appId = row.getLong(AppCols.ID);
    this.updateGuid(app, appId);

    String handleString = row.getString(AppCols.HANDLE);
    if (handleString != null) {
      Handle handle = Handle.ofFailSafe(handleString);
      app.setHandle(handle);
    }


    /**
     * Scalars
     */
    app.setName(row.getString(AppCols.NAME));
    app.setSlogan(row.getString(AppCols.SLOGAN));

    /**
     * Home URL
     */
    String home = row.getString(AppCols.HOME);
    if (home != null) {
      try {
        app.setHome(new java.net.URL(home));
      } catch (MalformedURLException e) {
        // should not happen as we are responsible for the insertion
        throw new InternalException("The home app value is not a valid URL", e);
      }
    }

    /**
     * Logo URL
     */
    String logo = row.getString(AppCols.LOGO);
    if (logo != null) {
      try {
        app.setHome(new java.net.URL(logo));
      } catch (MalformedURLException e) {
        // should not happen as we are responsible for the insertion
        throw new InternalException("The logo app value is not a valid URL", e);
      }
    }

    /**
     * Terms URL
     */
    String terms = row.getString(AppCols.TERM_OF_SERVICE);
    if (terms != null) {
      try {
        app.setTermsOfServices(new java.net.URL(terms));
      } catch (MalformedURLException e) {
        // should not happen as we are responsible for the insertion
        throw new InternalException("The terms app value is not a valid URI", e);
      }
    }

    /**
     * Colors
     */
    String primaryColor = row.getString(AppCols.PRIMARY_COLOR);
    if (primaryColor != null) {
      app.setPrimaryColor(Color.ofFailSafe(primaryColor));
    }

    /**
     * Count
     */
    Integer listCount = row.getInteger(AppCols.LIST_COUNT, 0);
    app.setListCount(listCount);

    return app;


  }


  public Future<App> getAppByHandle(Handle handle, Realm realm) {

    return this.jdbcPool.withConnection(sqlConnection -> getAppByHandle(handle, realm, sqlConnection));
  }


  public AppGuid getGuidFromHash(String appGuid) throws CastException {
    return this.apiApp
      .getHttpServer()
      .getServer()
      .getJacksonMapperManager()
      .getDeserializer(AppGuid.class)
      .deserialize(appGuid);
  }


  public Future<App> getAppByGuid(AppGuid appGuid, Realm realm) {
    return this.jdbcPool.withConnection(sqlConnection -> getAppByGuid(appGuid, realm, sqlConnection));
  }


  /**
   * Getsert: get or insert an app with a local id
   */
  public Future<App> getsertOnStartup(AppInputProps appInputProps, Realm realm, Long askedLocalId) {
    Future<App> selectApp;
    if (askedLocalId != null) {
      AppGuid askedAppGuid = new AppGuid();
      askedAppGuid.setRealmId(realm.getGuid().getLocalId());
      askedAppGuid.setLocalId(askedLocalId);
      selectApp = this.getAppByGuid(askedAppGuid, realm);
    } else {
      Handle handle = appInputProps.getHandle();
      if (handle == null || handle.getValue() == null) {
        return Future.failedFuture(new InternalException("The app to getsert should have an identifier (local id, or handle)"));
      }
      selectApp = this.getAppByHandle(handle, realm);
    }
    return selectApp
      .compose(selectedApp -> {
        if (selectedApp != null) {
          return Future.succeededFuture(selectedApp);
        }
        return this.insertApp(appInputProps, realm, askedLocalId);
      });
  }

  /**
   * @param appHandle     - the app handle
   * @param realm         - the realm to insert to
   * @param sqlConnection - a connection with or without transaction
   * @return the app or null
   */
  private Future<App> getAppByHandle(Handle appHandle, Realm realm, SqlConnection sqlConnection) {
    return JdbcSelect.from(this.appTable)
      .addEqualityPredicate(AppCols.HANDLE, appHandle.getValue())
      .addEqualityPredicate(AppCols.REALM_ID, realm.getGuid().getLocalId())
      .execute(sqlConnection)
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        JdbcRow row = userRows.iterator().next();
        return Future.succeededFuture(getFromRow(row, realm));
      });
  }

  private Future<App> getAppByGuid(AppGuid appGuid, Realm realm, SqlConnection sqlConnection) {
    assert realm != null : "The realm should not be null";
    return JdbcSelect.from(this.appTable)
      .addEqualityPredicate(AppCols.REALM_ID, appGuid.getRealmId())
      .addEqualityPredicate(AppCols.ID, appGuid.getLocalId())
      .execute(sqlConnection)
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        JdbcRow row = userRows.iterator().next();
        return Future.succeededFuture(getFromRow(row, realm));
      });
  }


  /**
   * @param appInputProps - the app to insert (realm is mandatory)
   * @param realm - the realm
   * @param askedLocalId - the asked local id (used only at initial Eraldy data insertion)
   * @return the app given with an id and a guid
   */
  public Future<App> insertApp(AppInputProps appInputProps, Realm realm, Long askedLocalId) {

    /**
     * We build before getting the next id
     * to not eat an id in the sequence if there is an error
     * on this part
     */
    JdbcInsert jdbcInsert = JdbcInsert.into(this.appTable);
    App app = new App();

    /**
     * Realm
     */
    app.setRealm(realm);

    /**
     * Insertion time
     */
    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    app.setCreationTime(nowInUtc);
    jdbcInsert.addColumn(AppCols.CREATION_TIME, app.getCreationTime());
    app.setModificationTime(nowInUtc);
    jdbcInsert.addColumn(AppCols.MODIFICATION_TIME, app.getModificationTime());

    /**
     * Owner
     */
    OrgaUserGuid ownerUserGuid = appInputProps.getOwnerUserGuid();
    if (ownerUserGuid != null) {
      app.setOwnerUser(this.apiApp.getOrganizationUserProvider().toNewUserFromGuid(ownerUserGuid));
    } else {
      app.setOwnerUser(realm.getOwnerUser());
    }
    jdbcInsert.addColumn(AppCols.OWNER_USER_ID, app.getOwnerUser().getGuid().getUserId());
    jdbcInsert.addColumn(AppCols.OWNER_ORGA_ID, app.getOwnerUser().getGuid().getOrganizationId());
    jdbcInsert.addColumn(AppCols.OWNER_REALM_ID, app.getOwnerUser().getGuid().getRealmId());

    /**
     * Scalars
     */
    app.setName(appInputProps.getName());
    jdbcInsert.addColumn(AppCols.NAME, app.getName());

    Handle handle = appInputProps.getHandle();
    if (handle != null) {
      app.setHandle(handle);
      jdbcInsert.addColumn(AppCols.HANDLE, app.getHandle().getValue());
    }

    app.setSlogan(appInputProps.getSlogan());
    jdbcInsert.addColumn(AppCols.SLOGAN, app.getSlogan());

    URL logo = appInputProps.getLogo();
    if (logo != null) {
      app.setLogo(logo);
      jdbcInsert.addColumn(AppCols.LOGO, app.getLogo().toString());
    }

    URL home = appInputProps.getHome();
    if (home != null) {
      app.setHome(home);
      jdbcInsert.addColumn(AppCols.HOME, app.getHome().toString());
    }

    URL terms = appInputProps.getTermsOfServices();
    if (terms != null) {
      app.setTermsOfServices(terms);
      jdbcInsert.addColumn(AppCols.TERM_OF_SERVICE, app.getTermsOfServices().toString());
    }

    return this.jdbcPool.withTransaction(sqlConnection -> this.apiApp.getRealmSequenceProvider()
      .getNextIdForTableAndRealm(sqlConnection, realm, this.appTable)
      .compose(finalAppId -> {

        if (askedLocalId != null && !askedLocalId.equals(finalAppId)) {
          /**
           * When we insert a startup
           * with {@link #getsertOnStartup(App, SqlConnection)}
           * where there is no data
           */
          return Future.failedFuture("The asked local id (" + askedLocalId + ") is different of the id given (" + finalAppId + "). The insertion order in the Eraldy model is not good.");
        }

        /**
         * Guid
         */
        this.updateGuid(app, finalAppId);
        jdbcInsert.addColumn(AppCols.REALM_ID, app.getGuid().getRealmId());
        jdbcInsert.addColumn(AppCols.ID, app.getGuid().getLocalId());

        return jdbcInsert
          .execute(sqlConnection)
          .compose(rows -> Future.succeededFuture(app));
      }));
  }

  @SuppressWarnings("unused")
  public Future<App> getAppByIdentifier(String appIdentifier, Realm realm) {
    try {
      AppGuid guid = this.getGuidFromHash(appIdentifier);
      return this.getAppByGuid(guid, realm);
    } catch (CastException e) {
      Handle handle;
      try {
        handle = this.apiApp.getHttpServer().getServer().getJacksonMapperManager().getDeserializer(Handle.class).deserialize(appIdentifier);
      } catch (CastException ex) {
        return Future.failedFuture(TowerFailureException.builder()
          .setMessage("The appIdentifier (" + appIdentifier + ") is not a valid guid or handle. " + e.getMessage())
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setCauseException(e)
          .build()
        );
      }
      return getAppByHandle(handle, realm);
    }

  }

  public App getRequestingApp(RoutingContext routingContext) {
    return this.apiApp.getAuthClientIdHandler().getRequestingApp(routingContext);
  }


  public App toAppFromGuid(AppGuid appGuid) {
    App app = new App();
    app.setGuid(appGuid);
    return app;
  }
}
