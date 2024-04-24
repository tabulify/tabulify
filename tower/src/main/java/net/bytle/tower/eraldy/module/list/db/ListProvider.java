package net.bytle.tower.eraldy.module.list.db;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.ListItemMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.app.db.AppProvider;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.list.inputs.ListInputProps;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListGuidDeserializer;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.eraldy.module.organization.db.OrganizationUserProvider;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.realm.db.RealmProvider;
import net.bytle.tower.util.Guid;
import net.bytle.type.Handle;
import net.bytle.vertx.*;
import net.bytle.vertx.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manage the get/upsert of a {@link ListObject} object asynchronously
 */
public class ListProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(ListProvider.class);

  protected static final String TABLE_NAME = "realm_list";

  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String LIST_PREFIX = "list";
  public static final String LIST_APP_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + AppProvider.APP_ID_COLUMN;
  public static final String LIST_ID_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "id";
  private static final String LIST_REALM_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "realm_id";
  public static final String LIST_GUID_PREFIX = "lis";
  private final EraldyApiApp apiApp;

  public static final String LIST_HANDLE_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "handle";
  public static final String LIST_NAME_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "name";
  private final Pool jdbcPool;
  private final JsonMapper apiMapper;
  private static final String LIST_USER_COUNT_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "user" + COLUMN_PART_SEP + "count";
  private static final String LIST_USER_IN_COUNT_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "user" + COLUMN_PART_SEP + "in" + COLUMN_PART_SEP + "count";
  private static final String LIST_MAILING_COUNT_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "mailing" + COLUMN_PART_SEP + "count";
  private final JdbcTable listTable;


  public ListProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    Server server = apiApp.getHttpServer().getServer();
    this.jdbcPool = jdbcSchema.getJdbcClient().getPool();

    server.getJacksonMapperManager()
      .addDeserializer(ListGuid.class, new JacksonListGuidDeserializer(apiApp));

    this.apiMapper = server.getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .addMixIn(ListObject.class, ListItemMixinWithRealm.class)
      .build();
    this.listTable = JdbcTable.build(jdbcSchema, "realm_list", ListCols.values())
      .addPrimaryKeyColumn(ListCols.ID)
      .addPrimaryKeyColumn(ListCols.REALM_ID)
      .build()
    ;

  }


  /**
   * This function was created to be sure that the data is consistent
   * between guid and (id and realm id)
   *
   * @param listObject - the registration list
   */
  private void updateGuid(ListObject listObject) {
    if (listObject.getGuid() != null) {
      return;
    }
    String guid = apiApp.createGuidFromRealmAndObjectId(LIST_GUID_PREFIX, listObject.getApp().getRealm(), listObject.getLocalId()).toString();
    listObject.setGuid(guid);
  }

  public static OrgaUser getOwnerUser(ListObject list) {
    OrgaUser ownerUser = list.getOwnerUser();
    if (ownerUser != null) {
      return ownerUser;
    }
    ownerUser = list.getApp().getOwnerUser();
    if (ownerUser != null) {
      return ownerUser;
    }
    ownerUser = list.getApp().getRealm().getOwnerUser();
    if (ownerUser != null) {
      return ownerUser;
    }
    throw new InternalException("The owner of the list (" + list + ") could not be determined");
  }


  /**
   * @param listHandle - the handle to lookup
   * @param app - the app if insertion
   * @param listInputProps  - the props
   * @return the realm with the id
   */
  public Future<ListObject> upsertList(String listHandle, App app, ListInputProps listInputProps) {


    return this.getListByHandle(listHandle, app.getRealm())
      .compose(list -> {
        if (list == null) {
          return this.insertList(app, listInputProps);
        }
        return this.updateList(list, listInputProps);
      });


  }

  private Future<ListObject> insertList(App app, ListInputProps listInputProps) {

    /**
     * User
     */
    OrgaUserGuid ownerIdentifier = listInputProps.getOwnerUserGuid();
    Future<OrgaUser> futureUser = Future.succeededFuture(app.getOwnerUser());
    if (ownerIdentifier != null) {
      OrganizationUserProvider userProvider = apiApp.getOrganizationUserProvider();
      futureUser = userProvider.getOrganizationUserByLocalId(ownerIdentifier.getLocalId());
    }

    return futureUser
      .compose(user -> {

        JdbcInsert jdbcInsert = JdbcInsert.into(this.listTable);


        // New list
        ListObject newList = new ListObject();

        /**
         * Owner
         */
        if (user == null) {
          user = app.getOwnerUser();
        }
        newList.setOwnerUser(user);
        jdbcInsert.addColumn(ListCols.OWNER_USER_ID, user.getLocalId());
        jdbcInsert.addColumn(ListCols.ORGA_ID, user.getOrganization().getLocalId());

        /**
         * Realm and App
         */
        newList.setApp(app);
        jdbcInsert.addColumn(ListCols.REALM_ID, app.getRealm().getLocalId());
        jdbcInsert.addColumn(ListCols.APP_ID, app.getLocalId());

        /**
         * Name
         */
        String name = listInputProps.getName();
        if (name == null || name.isBlank()) {
          name = "List of " + LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE);
        }
        newList.setName(name);
        jdbcInsert.addColumn(ListCols.NAME, newList.getName());

        Handle handle = listInputProps.getHandle();
        if (handle != null) {
          newList.setHandle(handle);
          jdbcInsert.addColumn(ListCols.HANDLE, newList.getHandle().getValue());
        }

        newList.setUserCount(0L);
        jdbcInsert.addColumn(ListCols.USER_COUNT, newList.getUserCount());

        newList.setUserInCount(0L);
        jdbcInsert.addColumn(ListCols.USER_IN_COUNT, newList.getUserInCount());

        newList.setMailingCount(0L);
        jdbcInsert.addColumn(ListCols.MAILING_COUNT, newList.getMailingCount());

        newList.setCreationTime(DateTimeService.getNowInUtc());
        jdbcInsert.addColumn(ListCols.CREATION_TIME, newList.getCreationTime());

        URI memberListRegistrationPath = this.apiApp.getEraldyModel().getMemberListRegistrationPath(newList);
        newList.setRegistrationUrl(memberListRegistrationPath);

        return jdbcPool
          .withTransaction(sqlConnection ->
            this.apiApp.getRealmSequenceProvider()
              .getNextIdForTableAndRealm(sqlConnection, app.getRealm(), this.listTable)
              .compose(nextId -> {

                newList.setLocalId(nextId);
                jdbcInsert.addColumn(ListCols.ID, nextId);
                this.updateGuid(newList);

                return jdbcInsert.execute(sqlConnection);
              })
              .compose(rowSet -> {
                Long realmId = newList.getApp().getRealm().getLocalId();
                Long listId = newList.getLocalId();
                final String createListRegistrationPartition =
                  "CREATE TABLE IF NOT EXISTS " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + ListUserProvider.TABLE_NAME + "_" + realmId + "_" + listId + "\n" +
                    "    partition of " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + ListUserProvider.TABLE_NAME + "\n" +
                    "        (" + ListUserProvider.REALM_COLUMN + ", " + ListUserProvider.LIST_ID_COLUMN + ")\n" +
                    "        FOR VALUES FROM (" + realmId + "," + listId + ") TO (" + realmId + "," + (listId + 1) + " )";
                return sqlConnection
                  .preparedQuery(createListRegistrationPartition)
                  .execute()
                  .onFailure(e -> LOGGER.error("List Registration Partition creation Error: Sql Error " + e.getMessage() + ". With Sql" + createListRegistrationPartition, e));
              })
              .onFailure(e -> LOGGER.error("List creation Error: Sql Error " + e.getMessage(), e))
              .compose(rows -> Future.succeededFuture(newList)));

      });
  }


  public Future<ListObject> updateList(ListObject listObject, ListInputProps listInputProps) {

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.listTable)
      .addPredicateColumn(ListCols.ID, listObject.getLocalId())
      .addPredicateColumn(ListCols.REALM_ID, listObject.getApp().getRealm().getLocalId())
      .addUpdatedColumn(ListCols.MODIFICATION_TIME, DateTimeService.getNowInUtc());

    String newName = listInputProps.getName();
    if (newName != null && !listObject.getName().equals(newName)) {
      jdbcUpdate.addUpdatedColumn(ListCols.NAME, newName);
      listObject.setName(newName);
    }

    Handle newHandle = listInputProps.getHandle();
    if (newHandle != null && !Objects.equals(listObject.getHandle(), newHandle)) {
      jdbcUpdate.addUpdatedColumn(ListCols.HANDLE, newHandle);
      listObject.setHandle(newHandle);
    }

    OrgaUserGuid ownerGuidObject = listInputProps.getOwnerUserGuid();
    if (ownerGuidObject != null && !Objects.equals(ownerGuidObject, listObject.getOwnerUser().getGuid())) {

      long userLocalId = ownerGuidObject.getLocalId();
      jdbcUpdate.addUpdatedColumn(ListCols.OWNER_USER_ID, userLocalId);
      // Lazy initialization (GraphQL feature)
      OrgaUser orgaUser = new OrgaUser();
      orgaUser.setLocalId(userLocalId);
      orgaUser.setRealm(this.apiApp.getEraldyModel().getRealm());
      listObject.setOwnerUser(orgaUser);

    }

    /**
     * May happen after an import
     */
    Long newUserCount = listInputProps.getUserCount();
    if (newUserCount != null && newUserCount.equals(listObject.getUserCount())) {
      listObject.setUserCount(newUserCount);
      jdbcUpdate.addUpdatedColumn(ListCols.USER_COUNT, newUserCount);
    }
    Long newUserInCount = listInputProps.getUserInCount();
    if (newUserInCount != null && newUserInCount.equals(listObject.getUserInCount())) {
      listObject.setUserInCount(newUserInCount);
      jdbcUpdate.addUpdatedColumn(ListCols.USER_IN_COUNT, newUserInCount);
    }

    return jdbcUpdate.execute()
      .compose(ok -> Future.succeededFuture(listObject));


  }


  /**
   * @param realm - the realmId
   * @return the realm
   */
  public Future<java.util.List<ListObject>> getListsForRealm(Realm realm) {


    String selectListsForRealmSql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " where \n" +
      LIST_REALM_COLUMN + " = $1";
    Tuple parameters = Tuple.of(realm.getLocalId());
    return jdbcPool
      .preparedQuery(selectListsForRealmSql)
      .execute(parameters)
      .onFailure(e -> LOGGER.error("Get lists by realms error with the sql \n " + selectListsForRealmSql, e))
      .compose(rowSet -> {

        /**
         * the {@link CompositeFuture#all(java.util.List) all function} does not
         * take other thing than a raw future
         */
        List<ListObject> listObjects = new ArrayList<>();
        for (Row row : rowSet) {
          ListObject futurePublication = getListFromRow(row, realm, null);
          listObjects.add(futurePublication);
        }

        return Future.succeededFuture(listObjects);
      });

  }

  private ListObject getListFromRow(Row row, Realm realm, App app) {

    assert realm != null : "The realm should not be null";

    ListObject listObject = new ListObject();

    /**
     * Local Id
     */
    Long listId = row.getLong(LIST_ID_COLUMN);
    listObject.setLocalId(listId);

    /**
     * App
     */
    Long realmId = row.getLong(LIST_REALM_COLUMN);
    if (!Objects.equals(realmId, realm.getLocalId())) {
      throw new InternalException("The passed realm (" + realm.getLocalId() + ") and the database realm id (" + realmId + " differs");
    }
    if (app == null) {
      app = new App();
      app.setLocalId(row.getLong(LIST_APP_COLUMN));
      app.setRealm(realm);
      this.apiApp.getAppProvider().updateGuid(app);
    }
    listObject.setApp(app);

    /**
     * Guid
     */
    this.updateGuid(listObject);

    /**
     * Owner
     * Id + Orga
     */
    Organization organisation = new Organization();
    organisation.setLocalId(row.getLong(ListCols.ORGA_ID.getColumnName()));
    OrgaUser orgaUser = new OrgaUser();
    orgaUser.setOrganization(organisation);
    listObject.setOwnerUser(orgaUser);
    orgaUser.setLocalId(row.getLong(ListCols.OWNER_USER_ID.getColumnName()));
    orgaUser.setRealm(this.apiApp.getEraldyModel().getRealm());
    this.apiApp.getUserProvider().updateGuid(orgaUser);


    /**
     * Handle
     */
    String handleString = row.getString(LIST_HANDLE_COLUMN);
    if (handleString != null) {
      Handle listHandle = Handle.ofFailSafe(handleString);
      listObject.setHandle(listHandle);
    }

    /**
     * Scalar
     */
    listObject.setName(row.getString(LIST_NAME_COLUMN));

    /**
     * Analytics
     */
    listObject.setUserCount(Objects.requireNonNullElse(row.getLong(LIST_USER_COUNT_COLUMN), 0L));
    listObject.setUserInCount(Objects.requireNonNullElse(row.getLong(LIST_USER_IN_COUNT_COLUMN), 0L));
    listObject.setMailingCount(Objects.requireNonNullElse(row.getLong(LIST_MAILING_COUNT_COLUMN), 0L));


    /**
     * Registration
     */
    URI memberListRegistrationPath = this.apiApp.getEraldyModel().getMemberListRegistrationPath(listObject);
    listObject.setRegistrationUrl(memberListRegistrationPath);
    return listObject;

  }

  public Future<ListObject> getListById(long listId, Realm realm) {

    String sql = "SELECT * " +
      "FROM \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " WHERE \n" +
      " " + LIST_ID_COLUMN + " = $1 " +
      " AND " + LIST_REALM_COLUMN + " = $2";
    Tuple parameters = Tuple.of(listId, realm.getLocalId());
    return jdbcPool
      .preparedQuery(sql)
      .execute(parameters)
      .onFailure(e -> LOGGER.error("Get list by Id error with the sql.\n" + sql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return Future.succeededFuture(getListFromRow(row, realm, null));
      });
  }


  public Future<ListObject> getListByGuidObject(Guid listGuid) {

    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(listGuid.getRealmOrOrganizationId())
      .compose(realm -> getListById(listGuid.validateRealmAndGetFirstObjectId(realm.getLocalId()), realm));
  }

  public Future<java.util.List<ListSummary>> getListsSummary(Realm realm) {

    String sql = "SELECT list.list_id, list.list_handle, app.app_uri, count(realm_list_user.list_user_user_id) user_count\n" +
      "FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " list \n" +
      " LEFT JOIN " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + ListUserProvider.TABLE_NAME + " realm_list_user\n" +
      "    on list.list_id = realm_list_user.list_user_list_id\n" +
      " JOIN " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + AppProvider.APP_TABLE_NAME + " app\n" +
      "    on list.list_app_id = app.app_id\n" +
      "where list.list_realm_id = $1\n" +
      "group by list.list_id, list.list_handle, app.app_uri";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(realm.getLocalId()))
      .recover(err -> FailureStatic.SqlFailFuture("List Summary", sql, err))
      .compose(publicationRows -> {

        java.util.List<ListSummary> futurePublications = new ArrayList<>();
        for (Row row : publicationRows) {
          ListSummary listSummary = new ListSummary();

          // List Id
          Long listId = row.getLong(LIST_ID_COLUMN);
          String listGuid = apiApp.createGuidFromRealmAndObjectId(LIST_GUID_PREFIX, realm, listId).toString();
          listSummary.setGuid(listGuid);

          // List Handle
          String listHandle = row.getString(LIST_HANDLE_COLUMN);
          listSummary.setHandle(listHandle);

          // App Uri
          String appUri = row.getString("app_uri");
          listSummary.setAppUri(appUri);

          // Publication Name
          Integer subscriberCount = row.getInteger("subscriber_count");
          listSummary.setSubscriberCount(subscriberCount);

          futurePublications.add(listSummary);

        }

        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future.succeededFuture(futurePublications);
      });
  }

  public Future<java.util.List<ListObject>> getListsForApp(App app) {
    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " where \n" +
      " " + LIST_APP_COLUMN + " = $1" +
      " AND " + LIST_REALM_COLUMN + " = $2";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(app.getLocalId(), app.getRealm().getLocalId()))
      .onFailure(t -> LOGGER.error("Error getListsForApp with the Sql:\n" + sql, t))
      .compose(listRows -> {

        /**
         * the {@link CompositeFuture#all(java.util.List)}  all function } does not
         * take other thing than a raw future
         */
        java.util.List<ListObject> listObjects = new ArrayList<>();
        for (Row row : listRows) {
          ListObject futurePublication = getListFromRow(row, app.getRealm(), app);
          listObjects.add(futurePublication);
        }
        return Future.succeededFuture(listObjects);
      });
  }

  public Guid getGuidObject(String listGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(ListProvider.LIST_GUID_PREFIX, listGuid);
  }

  public Future<ListObject> getListByHandle(String listHandle, Realm realm) {
    String sql = "SELECT * " +
      "FROM \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " WHERE \n" +
      " " + LIST_HANDLE_COLUMN + " = $1 " +
      " AND " + LIST_REALM_COLUMN + " = $2";
    Tuple parameters = Tuple.of(listHandle, realm.getLocalId());
    return jdbcPool
      .preparedQuery(sql)
      .execute(parameters)
      .onFailure(e -> LOGGER.error("Get list by handle error with the sql.\n" + sql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return Future.succeededFuture(getListFromRow(row, realm, null));
      });
  }


  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  public Future<Void> deleteById(Long listId, Realm realm) {

    final String deleteSql = "DELETE FROM\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " WHERE \n" +
      " " + LIST_ID_COLUMN + " = $1 " +
      " AND " + LIST_REALM_COLUMN + " = $2";
    Tuple parameters = Tuple.of(listId, realm.getLocalId());
    return jdbcPool
      .preparedQuery(deleteSql)
      .execute(parameters)
      .compose(
        res -> Future.succeededFuture(),
        err -> {
          Throwable cause;
          if (this.apiApp.addDebugInfo()) {
            cause = new InternalException("SQL executed:\n" + deleteSql, err);
          } else {
            cause = err;
          }
          return Future.failedFuture(new InternalException("Could not delete the list by id. Error: " + err.getMessage(), cause));
        }
      );
  }


  /**
   * Utility function to return a list via a Guid identifier
   *
   * @param listIdentifier - a guid hash
   */
  public Future<ListObject> getListByGuidHashIdentifier(String listIdentifier) {
    Guid listGuid;
    try {
      listGuid = this.getGuidObject(listIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setMessage("The list identifier should be a guid. The value (" + listIdentifier + ") is not a valid guid.")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .build()
      );
    }
    return this.getListByGuidObject(listGuid);
  }

  /**
   * Retrieve a list identifier in the path, check the scope and return a list
   * @param routingContext - the routing context
   * @param scope - the scope
   * @return the list object or null if not found
   */
  public Future<ListObject> getListByIdentifierFoundInPathParameterAndVerifyScope(RoutingContext routingContext, AuthUserScope scope) {
    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    String listIdentifier;
    try {
      listIdentifier = routingContextWrapper.getRequestPathParameter("listIdentifier").getString();
    } catch (NotFoundException e) {
      // our fault
      return Future.failedFuture(
        TowerFailureException.builder()
          .setMessage("The list identifier was not found in the path")
          .build()
      );
    }
    String realmIdentifier = routingContextWrapper.getRequestQueryParameterAsString("realmIdentifier");
    ListProvider listProvider = apiApp.getListProvider();
    RealmProvider realmProvider = apiApp.getRealmProvider();
    Guid listGuid = null;
    Future<Realm> futureRealm;
    try {
      listGuid = listProvider.getGuidObject(listIdentifier);
      futureRealm = realmProvider.getRealmFromLocalId(listGuid.getRealmOrOrganizationId());
    } catch (CastException e) {
      if (realmIdentifier == null) {
        return Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The realm identifier should be given when the list identifier (" + listIdentifier + ") is a handle")
          .buildWithContextFailing(routingContext)
        );
      }
      futureRealm = realmProvider.getRealmFromIdentifier(realmIdentifier);
    }
    Guid finalListGuid = listGuid;
    String finalListIdentifier = listIdentifier;
    return futureRealm
      .compose(realm -> apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, scope))
      .compose(realm -> {
        Future<ListObject> listFuture;
        if (finalListGuid != null) {
          long listId = finalListGuid.validateRealmAndGetFirstObjectId(realm.getLocalId());
          listFuture = listProvider.getListById(listId, realm);
        } else {
          listFuture = listProvider.getListByHandle(finalListIdentifier, realm);
        }
        return listFuture;
      });
  }

  public Future<Void> deleteByList(ListObject listObject) {
    return deleteById(listObject.getLocalId(), listObject.getApp().getRealm());
  }


  public Future<ListObject> insertListRequestHandler(String appGuid, ListInputProps listInputProps, RoutingContext routingContext) {

    AppGuid appGuidObject;
    try {
      appGuidObject = this.apiApp.getAppProvider().getGuidFromHash(appGuid);
    } catch (CastException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The app guid (" + appGuid + ") is not valid")
        .buildWithContextFailing(routingContext)
      );
    }
    return this.apiApp.getRealmProvider()
      .getRealmByLocalIdWithAuthorizationCheck(appGuidObject.getRealmId(), AuthUserScope.LIST_CREATION, routingContext)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The realm for the app guid (" + appGuid + ") was not found")
            .buildWithContextFailing(routingContext)
          );
        }
        return this.apiApp.getAppProvider().getAppById(appGuidObject.getAppLocalId(), realm);
      })
      .compose(app -> {
        if (app == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The app with the guid (" + appGuid + ") was not found")
            .buildWithContextFailing(routingContext)
          );
        }
        return this.insertList(app, listInputProps);
      });
  }

  public Future<ListObject> updateListRequestHandler(String listGuid, ListInputProps listInputProps, RoutingContext routingContext) {

    ListGuid listGuidObject;
    try {
      listGuidObject = this.apiApp.getHttpServer().getServer().getJacksonMapperManager().getDeserializer(ListGuid.class).deserialize(listGuid);
    } catch (CastException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The list guid (" + listGuid + ") is not valid")
        .build()
      );
    }
    ListProvider listProvider = this.apiApp.getListProvider();
    return this.apiApp.getRealmProvider().getRealmByLocalIdWithAuthorizationCheck(listGuidObject.getRealmId(), AuthUserScope.LIST_PATCH, routingContext)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setMessage("The realm for the list (" + listGuid + ") was not found")
            .build()
          );
        }
        return this.getListById(listGuidObject.getListLocalId(), realm);
      })
      .compose(list -> {
        if (list == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The list (" + listGuid + ") was not found")
            .build()
          );
        }
        return listProvider
          .updateList(list, listInputProps)
          .compose(Future::succeededFuture);
      });
  }

  public Future<ListObject> getByGuidRequestHandler(String listGuid, RoutingContext routingContext, AuthUserScope authUserScope) {
    ListGuid listGuidObject;
    try {
      listGuidObject = this.apiApp.getJackson().getDeserializer(ListGuid.class).deserialize(listGuid);
    } catch (CastException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The list guid (" + listGuid + ") is not valid")
        .build()
      );
    }
    return this.apiApp.getRealmProvider()
      .getRealmByLocalIdWithAuthorizationCheck(listGuidObject.getRealmId(), authUserScope, routingContext)
      .compose(realm -> this.getListById(listGuidObject.getListLocalId(), realm));

  }

  public Future<OrgaUser> buildListOwnerUserAtRequestTimeEventually(ListObject list) {
    if (!(list.getOwnerUser() == null || list.getOwnerUser().getEmailAddress() == null)) {
      return Future.succeededFuture(list.getOwnerUser());
    }
    Long localId = list.getOwnerUser().getLocalId();
    if (localId == null) {
      return Future.failedFuture(new InternalException("The build of the list did not set the local id on the owner user"));
    }
    return this.apiApp.getOrganizationUserProvider()
      .getOrganizationUserByLocalId(localId);
  }

  public Future<App> buildAppAtRequestTimeEventually(ListObject list) {
    /**
     * Name is mandatory
     * If not set, not built.
     */
    if (!(list.getApp() == null || list.getApp().getName() == null)) {
      return Future.succeededFuture(list.getApp());
    }
    Long localId = list.getApp().getLocalId();
    if (localId == null) {
      return Future.failedFuture(new InternalException("The build of the list did not set the local id of the app"));
    }
    Realm realm = list.getApp().getRealm();
    if (realm == null) {
      return Future.failedFuture(new InternalException("The build of the list did not set the realm of the app"));
    }
    return this.apiApp.getAppProvider()
      .getAppById(localId, realm);
  }
}
