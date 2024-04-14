package net.bytle.tower.eraldy.module.list.db;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
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
import net.bytle.tower.eraldy.module.list.inputs.ListInputProps;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.eraldy.objectProvider.OrganizationUserProvider;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.*;
import net.bytle.vertx.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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
  private static final String OWNER_PREFIX = "owner";
  private static final String LIST_PREFIX = "list";
  public static final String LIST_APP_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + AppProvider.APP_ID_COLUMN;
  public static final String LIST_USER_OWNER_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + OWNER_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  public static final String LIST_ID_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "id";
  private static final String LIST_REALM_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + RealmProvider.REALM_ID_COLUMN;
  static final String LIST_GUID_PREFIX = "lis";
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
    this.apiMapper = server.getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .addMixIn(ListObject.class, ListItemMixinWithRealm.class)
      .build();
    this.listTable = JdbcTable.build(jdbcSchema, "realm_list")
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
    String guid = apiApp.createGuidFromRealmAndObjectId(LIST_GUID_PREFIX, listObject.getRealm(), listObject.getLocalId()).toString();
    listObject.setGuid(guid);
  }

  public static OrganizationUser getOwnerUser(ListObject list) {
    OrganizationUser ownerUser = list.getOwnerUser();
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
    String ownerIdentifier = listInputProps.getOwnerGuid();
    Future<OrganizationUser> futureUser = Future.succeededFuture();
    if (ownerIdentifier != null) {
      OrganizationUserProvider userProvider = apiApp.getOrganizationUserProvider();
      futureUser = userProvider.getOrganizationUserByIdentifier(ownerIdentifier);
    }

    return futureUser
      .compose(user -> {

        JdbcInsert jdbcInsert = JdbcInsert.into(this.listTable);

        // New list
        ListObject newList = new ListObject();
        if (user != null) {
          newList.setOwnerUser(user);
          jdbcInsert.addColumn(ListCols.OWNER_USER_ID, user.getLocalId());
        }

        newList.setRealm(app.getRealm());
        jdbcInsert.addColumn(ListCols.REALM_ID, app.getRealm().getLocalId());
        newList.setApp(app);
        jdbcInsert.addColumn(ListCols.APP_ID, app.getLocalId());
        newList.setName(listInputProps.getName());
        jdbcInsert.addColumn(ListCols.NAME, listInputProps.getName());
        newList.setName(listInputProps.getHandle());
        jdbcInsert.addColumn(ListCols.HANDLE, listInputProps.getHandle());
        jdbcInsert.addColumn(ListCols.CREATION_TIME, DateTimeService.getNowInUtc());

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
                Long realmId = newList.getRealm().getLocalId();
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
      .addPrimaryKeyColumn(ListCols.ID, listObject.getLocalId())
      .addPrimaryKeyColumn(ListCols.REALM_ID, listObject.getRealm().getLocalId())
      .addUpdatedColumn(ListCols.MODIFICATION_TIME, DateTimeService.getNowInUtc());

    String newName = listInputProps.getName();
    if (newName != null && !listObject.getName().equals(newName)) {
      jdbcUpdate.addUpdatedColumn(ListCols.NAME, newName);
      listObject.setName(newName);
    }

    String newHandle = listInputProps.getHandle();
    if (newHandle != null && !listObject.getHandle().equals(newHandle)) {
      jdbcUpdate.addUpdatedColumn(ListCols.HANDLE, newHandle);
      listObject.setHandle(newHandle);
    }

    String ownerGuid = listInputProps.getOwnerGuid();
    if (ownerGuid != null) {
      Guid ownerGuidObject;
      try {
        ownerGuidObject = this.apiApp.getUserProvider().getGuidFromHash(ownerGuid);
      } catch (CastException e) {
        return Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The value (" + ownerGuid + ") is not a valid guid")
          .setCauseException(e)
          .build()
        );
      }
      long ownerRealmId = ownerGuidObject.getRealmOrOrganizationId();
      if (!listObject.getRealm().getLocalId().equals(ownerRealmId)) {
        return Future.failedFuture(TowerFailureException.builder()
          .setMessage("The owner and the list does not belong to the same realm")
          .build()
        );
      }
      long userLocalId = ownerGuidObject.validateRealmAndGetFirstObjectId(listObject.getRealm().getLocalId());
      jdbcUpdate.addUpdatedColumn(ListCols.OWNER_USER_ID, userLocalId);
      // Lazy initialization (GraphQL feature)
      OrganizationUser organizationUser = new OrganizationUser();
      organizationUser.setLocalId(userLocalId);
      organizationUser.setRealm(listObject.getRealm());
      listObject.setOwnerUser(organizationUser);
    }

    /**
     * May happen after an import
     */
    Long newUserCount = listInputProps.getUserCount();
    if(newUserCount!=null && newUserCount.equals(listObject.getUserCount())){
      listObject.setUserCount(newUserCount);
      jdbcUpdate.addUpdatedColumn(ListCols.USER_COUNT, newUserCount);
    }
    Long newUserInCount = listInputProps.getUserInCount();
    if(newUserInCount!=null && newUserInCount.equals(listObject.getUserInCount())){
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
        List<Future<ListObject>> futurePublications = new ArrayList<>();
        for (Row row : rowSet) {
          Future<ListObject> futurePublication = getListFromRow(row, null, realm);
          futurePublications.add(futurePublication);
        }

        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future
          .all(futurePublications)
          .onFailure(FailureStatic::failFutureWithTrace)
          .map(CompositeFuture::<ListObject>list);
      });

  }

  private Future<ListObject> getListFromRow(Row row, App knownApp, Realm knownRealm) {


    Realm appRealm = knownRealm;
    if (appRealm == null && knownApp != null) {
      appRealm = knownApp.getRealm();
    }
    Future<Realm> realmFuture = Future.succeededFuture(appRealm);
    Long realmId = row.getLong(LIST_REALM_COLUMN);
    if (appRealm == null) {
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromLocalId(realmId);
    } else {
      if (!Objects.equals(appRealm.getLocalId(), realmId)) {
        InternalException internalException = new InternalException("The realms between the row (" + realmId + ") and the known app or realm (" + appRealm.getLocalId() + ") are inconsistent ");
        return Future.failedFuture(internalException);
      }
    }
    return realmFuture
      .compose(realmResult -> {

        Long listId = row.getLong(LIST_ID_COLUMN);
        String listHandle = row.getString(LIST_HANDLE_COLUMN);
        Future<App> appFuture = Future.succeededFuture(knownApp);
        if (knownApp == null) {
          Long publisherAppId = row.getLong(LIST_APP_COLUMN);
          appFuture = this.apiApp.getAppProvider()
            .getAppById(publisherAppId, realmResult);
        }

        Long ownerId = row.getLong(LIST_USER_OWNER_COLUMN);
        Future<OrganizationUser> ownerFuture = Future.succeededFuture();
        if (ownerId != null) {
          ownerFuture = apiApp.getOrganizationUserProvider()
            .getOrganizationUserByLocalId(ownerId, realmResult.getLocalId(), realmResult);
        }

        return Future
          .all(appFuture, ownerFuture)
          .recover(e -> Future.failedFuture(new InternalException("getListFromRow error " + e.getMessage(), e)))
          .compose(mapper -> {


            ListObject listItem = new ListObject();

            /**
             * Identifier
             */
            listItem.setLocalId(listId);
            listItem.setRealm(realmResult);
            this.updateGuid(listItem);
            listItem.setHandle(listHandle);

            /**
             * Scalar
             */
            listItem.setName(row.getString(LIST_NAME_COLUMN));

            /**
             * Analytics
             */
            listItem.setUserCount(Objects.requireNonNullElse(row.getLong(LIST_USER_COUNT_COLUMN), 0L));
            listItem.setUserInCount(Objects.requireNonNullElse(row.getLong(LIST_USER_IN_COUNT_COLUMN), 0L));
            listItem.setMailingCount(Objects.requireNonNullElse(row.getLong(LIST_MAILING_COUNT_COLUMN), 0L));

            /**
             * Futures
             */
            App appResult = mapper.resultAt(0);
            if (appResult == null) {
              throw ValidationException.create("The app was not found", "appId", null);
            }
            listItem.setApp(appResult);
            OrganizationUser publisher = mapper.resultAt(1);
            if (publisher != null) {
              listItem.setOwnerUser(publisher);
            }

            listItem.setRealm(realmResult);
            URI memberListRegistrationPath = this.apiApp.getEraldyModel().getMemberListRegistrationPath(listItem);
            listItem.setRegistrationUrl(memberListRegistrationPath);
            return Future.succeededFuture(listItem);

          });
      });


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
        return getListFromRow(row, null, realm);
      });
  }

  /**
   * Build a list object from a post and insert it
   *
   * @param listPostBody - the post object
   * @return the list created
   */

  public Future<ListObject> postList(ListBody listPostBody, App app) {

    ListInputProps listObject = new ListInputProps();
    listObject.setName(listPostBody.getListName());
    listObject.setTitle(listPostBody.getListTitle());
    listObject.setHandle(listPostBody.getListHandle());
    listObject.setOwnerIdentifier(listPostBody.getOwnerUserIdentifier());
    return this.insertList(app, listObject);

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
      "where list_realm_id = $1\n" +
      "group by list.list_id, list.list_handle, app.app_uri";
    return jdbcPool.preparedQuery(
        sql)
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
        java.util.List<Future<ListObject>> futureLists = new ArrayList<>();
        for (Row row : listRows) {
          Future<ListObject> futurePublication = getListFromRow(row, app, app.getRealm());
          futureLists.add(futurePublication);
        }

        return Future
          .all(futureLists)
          .onFailure(FailureStatic::failFutureWithTrace)
          .map(CompositeFuture::<ListObject>list);
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
        return getListFromRow(row, null, realm);
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
    return deleteById(listObject.getLocalId(), listObject.getRealm());
  }


}
