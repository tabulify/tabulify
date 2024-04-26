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
import net.bytle.tower.eraldy.api.implementer.flow.ListImportUserAction;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.ListItemMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.app.db.AppProvider;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.common.jackson.JacksonStatusSerializer;
import net.bytle.tower.eraldy.module.list.inputs.ListInputProps;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListGuidDeserializer;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListGuidSerializer;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.eraldy.module.list.model.ListImportListUserStatus;
import net.bytle.tower.eraldy.module.organization.db.OrganizationUserProvider;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.realm.db.RealmProvider;
import net.bytle.tower.util.Guid;
import net.bytle.type.Handle;
import net.bytle.type.HandleCastException;
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
  public static final String LIST_ID_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "id";
  public static final String LIST_GUID_PREFIX = "lis";
  private final EraldyApiApp apiApp;

  public static final String LIST_HANDLE_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "handle";
  private final Pool jdbcPool;
  private final JsonMapper apiMapper;
  private final JdbcTable listTable;


  public ListProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    Server server = apiApp.getHttpServer().getServer();
    this.jdbcPool = jdbcSchema.getJdbcClient().getPool();

    server.getJacksonMapperManager()
      .addDeserializer(ListGuid.class, new JacksonListGuidDeserializer(apiApp))
      .addSerializer(ListGuid.class, new JacksonListGuidSerializer(apiApp))
      .addSerializer(ListImportListUserStatus.class, new JacksonStatusSerializer())
      .addSerializer(ListImportUserAction.class, new JacksonStatusSerializer())
      .addSerializer(ListUserSource.class, new JacksonStatusSerializer())
    ;

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
   * @param listId the list id
   */
  public void updateGuid(ListObject listObject, Long listId) {
    if (listObject.getGuid() != null) {
      return;
    }
    ListGuid listGuid = new ListGuid();
    listGuid.setLocalId(listId);
    // because the app is not on the database row, this function should be deleted
    // as we may get an error
    listGuid.setRealmId(listObject.getApp().getGuid().getRealmId());
    listObject.setGuid(listGuid);
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
  public Future<ListObject> upsertList(Handle listHandle, App app, ListInputProps listInputProps) {


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
      futureUser = userProvider.getOrganizationUserByGuid(ownerIdentifier);
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
        jdbcInsert.addColumn(ListCols.OWNER_USER_ID, user.getGuid().getLocalId());
        jdbcInsert.addColumn(ListCols.ORGA_ID, user.getGuid().getOrganizationId());

        /**
         * Realm and App
         */
        newList.setApp(app);
        jdbcInsert.addColumn(ListCols.REALM_ID, app.getGuid().getRealmId());
        jdbcInsert.addColumn(ListCols.APP_ID, app.getGuid().getLocalId());

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

        LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
        newList.setCreationTime(nowInUtc);
        jdbcInsert.addColumn(ListCols.CREATION_TIME, newList.getCreationTime());
        newList.setModificationTime(nowInUtc);
        jdbcInsert.addColumn(ListCols.MODIFICATION_TIME, newList.getModificationTime());


        return jdbcPool
          .withTransaction(sqlConnection ->
            this.apiApp.getRealmSequenceProvider()
              .getNextIdForTableAndRealm(sqlConnection, app.getRealm(), this.listTable)
              .compose(nextId -> {

                this.updateGuid(newList, nextId);
                jdbcInsert.addColumn(ListCols.ID, newList.getGuid().getLocalId());

                /**
                 * After the guid as it's needed to create the URL
                 */
                URI memberListRegistrationPath = this.apiApp.getEraldyModel().getMemberListRegistrationPath(newList);
                newList.setRegistrationUrl(memberListRegistrationPath);

                return jdbcInsert.execute(sqlConnection);
              })
              .compose(rowSet -> {
                long realmId = newList.getGuid().getRealmId();
                long listId = newList.getGuid().getLocalId();
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
      .addPredicateColumn(ListCols.ID, listObject.getGuid().getLocalId())
      .addPredicateColumn(ListCols.REALM_ID, listObject.getGuid().getRealmId())
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

      OrgaUser orgaUser = this.apiApp.getOrganizationUserProvider().toOrgaUserFromGuid(ownerGuidObject, listObject.getApp().getRealm());
      listObject.setOwnerUser(orgaUser);

      jdbcUpdate.addUpdatedColumn(ListCols.OWNER_USER_ID, listObject.getOwnerUser().getGuid().getLocalId());
      jdbcUpdate.addUpdatedColumn(ListCols.ORGA_ID, listObject.getOwnerUser().getGuid().getOrganizationId());


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


    return JdbcSelect.from(this.listTable)
      .addEqualityPredicate(ListCols.REALM_ID, realm.getGuid().getLocalId())
      .execute()
      .compose(rowSet -> {

        List<ListObject> listObjects = new ArrayList<>();
        for (JdbcRow row : rowSet) {
          ListObject futurePublication = getListFromRow(row, realm, null);
          listObjects.add(futurePublication);
        }

        return Future.succeededFuture(listObjects);
      });

  }

  private ListObject getListFromRow(JdbcRow row, Realm realm, App app) {

    assert realm != null : "The realm should not be null";

    ListObject listObject = new ListObject();

    /**
     * Local Id
     */
    Long listId = row.getLong(ListCols.ID);
    Long realmId = row.getLong(ListCols.REALM_ID);
    if (realmId != realm.getGuid().getLocalId()) {
      throw new InternalException("The passed realm (" + realm.getGuid() + ") and the database realm id (" + realmId + " differs");
    }

    /**
     * App
     */
    if (app == null) {
      AppGuid appGuid = new AppGuid();
      appGuid.setLocalId(row.getLong(ListCols.APP_ID));
      appGuid.setRealmId(realm.getGuid().getLocalId());
      app = this.apiApp.getAppProvider().toAppFromGuid(appGuid);
      app.setRealm(realm);
    }
    listObject.setApp(app);

    /**
     * After app as it's the realm holder
     */
    this.updateGuid(listObject, listId);


    /**
     * Owner
     * Id + Orga
     */
    OrgaUserGuid ownerUserGuid = new OrgaUserGuid();
    ownerUserGuid.setOrganizationId(row.getLong(ListCols.ORGA_ID));
    ownerUserGuid.setLocalId(row.getLong(ListCols.OWNER_USER_ID));
    listObject.setOwnerUser(this.apiApp.getOrganizationUserProvider().toOrgaUserFromGuid(ownerUserGuid, realm));

    /**
     * Handle
     */
    String handleString = row.getString(ListCols.HANDLE);
    if (handleString != null) {
      Handle listHandle = Handle.ofFailSafe(handleString);
      listObject.setHandle(listHandle);
    }

    /**
     * Scalar
     */
    listObject.setName(row.getString(ListCols.NAME));

    /**
     * Analytics
     */
    listObject.setUserCount(Objects.requireNonNullElse(row.getLong(ListCols.USER_COUNT), 0L));
    listObject.setUserInCount(Objects.requireNonNullElse(row.getLong(ListCols.USER_IN_COUNT), 0L));
    listObject.setMailingCount(Objects.requireNonNullElse(row.getLong(ListCols.MAILING_COUNT), 0L));

    /**
     * Time
     */
    listObject.setCreationTime(row.getLocalDateTime(ListCols.CREATION_TIME));
    listObject.setModificationTime(row.getLocalDateTime(ListCols.MODIFICATION_TIME));

    /**
     * Registration
     */
    URI memberListRegistrationPath = this.apiApp.getEraldyModel().getMemberListRegistrationPath(listObject);
    listObject.setRegistrationUrl(memberListRegistrationPath);
    return listObject;

  }

  public Future<ListObject> getListById(long listId, Realm realm) {


    return JdbcSelect.from(this.listTable)
      .addEqualityPredicate(ListCols.REALM_ID, realm.getGuid().getLocalId())
      .addEqualityPredicate(ListCols.ID, listId)
      .execute()
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        JdbcRow row = userRows.iterator().next();
        return Future.succeededFuture(getListFromRow(row, realm, null));
      });
  }


  public Future<ListObject> getListByGuidObject(ListGuid listGuid) {

    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(listGuid.getRealmId())
      .compose(realm -> getListById(listGuid.getLocalId(), realm));
  }

  public Future<java.util.List<ListSummary>> getListsSummary(Realm realm) {

    String sql = "SELECT list.list_id, list.list_handle, app.app_id, app.app_name, count(realm_list_user.list_user_user_id) user_count\n" +
      "FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " list \n" +
      " LEFT JOIN " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + ListUserProvider.TABLE_NAME + " realm_list_user\n" +
      "    on list.list_id = realm_list_user.list_user_list_id\n" +
      " JOIN " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + AppProvider.APP_TABLE_NAME + " app\n" +
      "    on list.list_app_id = app.app_id\n" +
      "where list.list_realm_id = $1\n" +
      "group by list.list_id, list.list_handle, app.app_id, app.app_name";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(realm.getGuid().getLocalId()))
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

          // App Name
          String appName = row.getString("app_name");
          listSummary.setAppUri(appName);

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

    return JdbcSelect.from(this.listTable)
      .addEqualityPredicate(ListCols.APP_ID, app.getGuid().getLocalId())
      .addEqualityPredicate(ListCols.REALM_ID, app.getGuid().getRealmId())
      .execute()
      .compose(listRows -> {

        /**
         * the {@link CompositeFuture#all(java.util.List)}  all function } does not
         * take other thing than a raw future
         */
        java.util.List<ListObject> listObjects = new ArrayList<>();
        for (JdbcRow row : listRows) {
          ListObject futurePublication = getListFromRow(row, app.getRealm(), app);
          listObjects.add(futurePublication);
        }
        return Future.succeededFuture(listObjects);
      });
  }

  public Guid getGuidObject(String listGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(ListProvider.LIST_GUID_PREFIX, listGuid);
  }

  public Future<ListObject> getListByHandle(Handle listHandle, Realm realm) {

    return JdbcSelect.from(this.listTable)
      .addEqualityPredicate(ListCols.REALM_ID, realm.getGuid().getLocalId())
      .addEqualityPredicate(ListCols.HANDLE, listHandle.getValue())
      .execute()
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        JdbcRow row = userRows.iterator().next();
        return Future.succeededFuture(getListFromRow(row, realm, null));
      });
  }


  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  public Future<Void> deleteById(ListGuid listGuid) {

    final String deleteSql = "DELETE FROM\n" +
      this.listTable.getFullName() + "\n" +
      " WHERE \n" +
      " " + ListCols.ID.getColumnName() + " = $1 " +
      " AND " + ListCols.REALM_ID.getColumnName() + " = $2";
    Tuple parameters = Tuple.of(listGuid.getLocalId(), listGuid.getRealmId());
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
    ListGuid listGuid = null;
    Future<Realm> futureRealm;
    try {
      listGuid = this.apiApp.getJackson().getDeserializer(ListGuid.class).deserialize(listIdentifier);
      futureRealm = realmProvider.getRealmFromLocalId(listGuid.getRealmId());
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
    ListGuid finalListGuid = listGuid;
    String finalListIdentifier = listIdentifier;
    return futureRealm
      .compose(realm -> apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, scope))
      .compose(realm -> {
        Future<ListObject> listFuture;
        if (finalListGuid != null) {
          long listId = finalListGuid.getLocalId();
          listFuture = listProvider.getListById(listId, realm);
        } else {
          try {
            listFuture = listProvider.getListByHandle(Handle.of(finalListIdentifier), realm);
          } catch (HandleCastException e) {
            return Future.failedFuture(TowerFailureException.builder()
              .setMessage("The handle value (" + finalListIdentifier + ") is not valid. Error: " + e.getMessage())
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .build()
            );
          }
        }
        return listFuture;
      });
  }

  public Future<Void> deleteByList(ListObject listObject) {
    return deleteById(listObject.getGuid());
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
        return this.apiApp.getAppProvider().getAppByGuid(appGuidObject, realm);
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
        return this.getListById(listGuidObject.getLocalId(), realm);
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
      .compose(realm -> this.getListById(listGuidObject.getLocalId(), realm));

  }

  public Future<OrgaUser> buildListOwnerUserAtRequestTimeEventually(ListObject list) {
    if (!(list.getOwnerUser() == null || list.getOwnerUser().getEmailAddress() == null)) {
      return Future.succeededFuture(list.getOwnerUser());
    }
    OrgaUserGuid orgaUserGuid = list.getOwnerUser().getGuid();
    if (orgaUserGuid == null) {
      return Future.failedFuture(new InternalException("The build of the list did not set the local id on the owner user"));
    }
    return this.apiApp.getOrganizationUserProvider()
      .getOrganizationUserByGuid(orgaUserGuid);
  }

  public Future<App> buildAppAtRequestTimeEventually(ListObject list) {
    /**
     * Name is mandatory
     * If not set, not built.
     */
    if (!(list.getApp() == null || list.getApp().getName() == null)) {
      return Future.succeededFuture(list.getApp());
    }
    AppGuid appGuid = list.getApp().getGuid();
    if (appGuid == null) {
      return Future.failedFuture(new InternalException("The build of the list did not set the local id of the app"));
    }
    Realm realm = list.getApp().getRealm();
    if (realm == null) {
      return Future.failedFuture(new InternalException("The build of the list did not set the realm of the app"));
    }
    return this.apiApp.getAppProvider()
      .getAppByGuid(appGuid, realm);
  }
}
