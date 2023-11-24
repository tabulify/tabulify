package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.ValidationException;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.ListItemMixin;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.Postgres;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manage the get/upsert of a {@link ListItem} object asynchronously
 */
public class ListProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(ListProvider.class);

  protected static final String TABLE_NAME = "realm_list";

  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String OWNER_PREFIX = "owner";
  private static final String LIST_PREFIX = "list";
  public static final String APP_OWNER_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + OWNER_PREFIX + COLUMN_PART_SEP + AppProvider.APP_ID_COLUMN;
  public static final String USER_OWNER_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + OWNER_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  public static final String DATA_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "data";
  static final String ID_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "id";
  private static final String REALM_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + RealmProvider.ID_COLUMN;
  static final String LIST_GUID_PREFIX = "lis";
  private final EraldyApiApp apiApp;

  public static final String HANDLE_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + "handle";
  private static final String MODIFICATION_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private static final String CREATION_COLUMN = LIST_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private final PgPool jdbcPool;
  private final JsonMapper apiMapper;


  public ListProvider(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getApexDomain().getHttpServer().getServer().getJdbcPool();
    this.apiMapper = apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .addMixIn(ListItem.class, ListItemMixin.class )
      .build();
  }


  /**
   * @param listItem - the list to make public
   * @return the publication without id, realm and with a guid
   */
  public ListItem toPublicClone(ListItem listItem) {


    return toClone(listItem, false);

  }

  /**
   * This function was created to be sure that the data is consistent
   * between guid and (id and realm id)
   *
   * @param listItem - the registration list
   */
  private void computeGuid(ListItem listItem) {
    if (listItem.getGuid() != null) {
      return;
    }
    String guid = apiApp.createGuidFromRealmAndObjectId(LIST_GUID_PREFIX, listItem.getRealm(), listItem.getLocalId()).toString();
    listItem.setGuid(guid);
  }

  public static User getOwnerUser(ListItem listItem) {
    User ownerUser = listItem.getOwnerUser();
    if (ownerUser != null) {
      return ownerUser;
    }
    return listItem.getOwnerApp().getUser();
  }


  /**
   * @param listItem the publication to upsert
   * @return the realm with the id
   */
  public Future<ListItem> upsertList(ListItem listItem) {

    Realm realm = listItem.getRealm();
    if (realm == null) {
      return Future.failedFuture(new InternalError("The realm is mandatory when upserting a list"));
    }

    User owner = listItem.getOwnerUser();
    Long ownerId;
    if (owner != null) {
      ownerId = owner.getLocalId();
      if (ownerId == null) {
        throw new InternalException("The owner id of a user object should not be null");
      }
    }

    App app = listItem.getOwnerApp();
    if (app == null) {
      return Future.failedFuture(new InternalError("The app is mandatory when upserting a list"));
    }
    Long appId = app.getLocalId();
    if (appId == null) {
      return Future.failedFuture(new InternalError("The app id is mandatory when upserting a list"));
    }

    if (listItem.getLocalId() != null) {
      return updateList(listItem);
    }

    /**
     * No upsert SQL statement
     * See identifier.md for more info
     */
    return updateListByHandleAndGetRowSet(listItem)
      .compose(rowSet -> {
        if (rowSet.size() == 0) {
          return insertList(listItem);
        }
        Long registrationListId = rowSet.iterator().next().getLong(ID_COLUMN);
        listItem.setLocalId(registrationListId);
        return Future.succeededFuture(listItem);
      });

  }

  private Future<ListItem> insertList(ListItem listItem) {


    final String insertSql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + HANDLE_COLUMN + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + APP_OWNER_COLUMN + ",\n" +
      "  " + USER_OWNER_COLUMN + ",\n" +
      "  " + CREATION_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6, $7)";


    return jdbcPool
      .withTransaction(sqlConnection ->
        SequenceProvider
          .getNextIdForTableAndRealm(sqlConnection, TABLE_NAME, listItem.getRealm().getLocalId())
          .compose(nextId -> {
            listItem.setLocalId(nextId);
            return sqlConnection
              .preparedQuery(insertSql)
              .execute(Tuple.of(
                listItem.getRealm().getLocalId(),
                listItem.getLocalId(),
                listItem.getHandle(),
                this.getDatabaseJsonObject(listItem),
                listItem.getOwnerApp().getLocalId(),
                listItem.getOwnerUser() != null ? listItem.getOwnerUser().getLocalId() : null,
                DateTimeUtil.getNowUtc()
              ))
              .onFailure(e -> LOGGER.error("Insert List: Sql Error " + e.getMessage() + ". With Sql" + insertSql, e));
          })
          .compose(rowSet -> {
            Long realmId = listItem.getRealm().getLocalId();
            Long listId = listItem.getLocalId();
            final String createListRegistrationPartition =
              "CREATE TABLE IF NOT EXISTS " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + ListRegistrationProvider.TABLE_NAME + "_" + realmId + "_" + listId + "\n" +
                "    partition of " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + ListRegistrationProvider.TABLE_NAME + "\n" +
                "        (" + ListRegistrationProvider.REALM_COLUMN + ", " + ListRegistrationProvider.LIST_ID_COLUMN + ")\n" +
                "        FOR VALUES FROM (" + realmId + "," + listId + ") TO (" + realmId + "," + (listId + 1) + " )";
            return sqlConnection
              .preparedQuery(createListRegistrationPartition)
              .execute()
              .onFailure(e -> LOGGER.error("List Registration Partition creation Error: Sql Error " + e.getMessage() + ". With Sql" + createListRegistrationPartition, e));
          })
          .onFailure(e -> LOGGER.error("List creation Error: Sql Error " + e.getMessage(), e))
          .compose(rows -> Future.succeededFuture(listItem)));
  }

  private Future<ListItem> updateList(ListItem listItem) {

    if (listItem.getRealm() == null) {
      InternalException internalException = new InternalException("The realm is null. You can't update a list without a realm");
      return Future.failedFuture(internalException);
    }
    if (listItem.getRealm().getLocalId() == null) {
      InternalException internalException = new InternalException("The realm id is null. You can't update a list without a realm id");
      return Future.failedFuture(internalException);
    }

    if (listItem.getLocalId() != null) {

      String updateByIdSql = "UPDATE \n" +
        JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " \n" +
        " set" +
        "  " + HANDLE_COLUMN + " = $1,\n" +
        "  " + DATA_COLUMN + " = $2,\n" +
        "  " + APP_OWNER_COLUMN + " = $3,\n" +
        "  " + USER_OWNER_COLUMN + " = $4,\n" +
        "  " + MODIFICATION_COLUMN + " = $5\n" +
        "where\n" +
        "  " + ID_COLUMN + "= $6\n" +
        "AND  " + REALM_COLUMN + "= $7 ";

      return jdbcPool
        .preparedQuery(updateByIdSql)
        .execute(Tuple.of(
          listItem.getHandle(),
          this.getDatabaseJsonObject(listItem),
          listItem.getOwnerApp().getLocalId(),
          listItem.getOwnerUser() != null ? listItem.getOwnerUser().getLocalId() : null,
          DateTimeUtil.getNowUtc(),
          listItem.getLocalId(),
          listItem.getRealm().getLocalId()
        ))
        .onFailure(e -> LOGGER.error("Update List by Id Error: Sql Error " + e.getMessage() + ". With Sql" + updateByIdSql, e))
        .compose(ok -> Future.succeededFuture(listItem));
    }

    if (listItem.getHandle() == null) {
      InternalException internalException = new InternalException("The list id and handle are null. You can't update a list without an id or an handle");
      return Future.failedFuture(internalException);
    }

    return updateListByHandleAndGetRowSet(listItem)
      .compose(rowSet -> {
        if (rowSet.size() == 0) {
          InternalException internalException = new InternalException("No list where updated with the handle (" + listItem.getHandle() + ") for the realm (" + listItem.getRealm().getHandle() + ")");
          return Future.failedFuture(internalException);
        }
        Long registrationListId = rowSet.iterator().next().getLong(ID_COLUMN);
        listItem.setLocalId(registrationListId);
        return Future.succeededFuture(listItem);
      });
  }

  private Future<RowSet<Row>> updateListByHandleAndGetRowSet(ListItem listItem) {

    String updateByHandleSql = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " \n" +
      " set" +
      "  " + DATA_COLUMN + " = $1,\n" +
      "  " + APP_OWNER_COLUMN + " = $2,\n" +
      "  " + USER_OWNER_COLUMN + " = $3,\n" +
      "  " + MODIFICATION_COLUMN + " = $4\n" +
      "where\n" +
      "  " + HANDLE_COLUMN + "= $5\n" +
      "AND  " + REALM_COLUMN + "= $6\n" +
      "RETURNING " + ID_COLUMN;

    return jdbcPool
      .preparedQuery(updateByHandleSql)
      .execute(Tuple.of(
        this.getDatabaseJsonObject(listItem),
        listItem.getOwnerApp().getLocalId(),
        listItem.getOwnerUser() != null ? listItem.getOwnerUser().getLocalId() : null,
        DateTimeUtil.getNowUtc(),
        listItem.getHandle(),
        listItem.getRealm().getLocalId()
      ))
      .onFailure(e -> LOGGER.error("Error Update List by Handle: Sql Error " + e.getMessage() + ". With Sql" + updateByHandleSql, e));
  }

  private JsonObject getDatabaseJsonObject(ListItem listItem) {

    JsonObject data = JsonObject.mapFrom(listItem);
    data.remove("id");
    data.remove(Guid.GUID);
    data.remove(RealmProvider.TABLE_PREFIX);
    data.remove("ownerUser");
    data.remove("ownerApp");
    return data;

  }


  /**
   * @param realm - the realmId
   * @return the realm
   */
  public Future<java.util.List<ListItem>> getListsForRealm(Realm realm) {


    String selectListsForRealmSql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " where \n" +
      REALM_COLUMN + " = $1";
    Tuple parameters = Tuple.of(realm.getLocalId());
    return jdbcPool
      .preparedQuery(selectListsForRealmSql)
      .execute(parameters)
      .onFailure(e -> LOGGER.error("Get lists by realms error with the sql \n " + selectListsForRealmSql, e))
      .compose(rowSet -> {

        /**
         * the {@link CompositeFuture#all(java.util.List)}  all function } does not
         * take other thing than a raw future
         */
        List<Future<ListItem>> futurePublications = new ArrayList<>();
        for (Row row : rowSet) {
          Future<ListItem> futurePublication = getListFromRow(row, null, realm);
          futurePublications.add(futurePublication);
        }

        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future
          .all(futurePublications)
          .onFailure(FailureStatic::failFutureWithTrace)
          .map(CompositeFuture::<ListItem>list);
      });

  }

  private Future<ListItem> getListFromRow(Row row, App knownApp, Realm knownRealm) {


    Realm appRealm = knownRealm;
    if (appRealm == null && knownApp != null) {
      appRealm = knownApp.getRealm();
    }
    Future<Realm> realmFuture = Future.succeededFuture(appRealm);
    Long realmId = row.getLong(REALM_COLUMN);
    if (appRealm == null) {
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromId(realmId);
    } else {
      if (!Objects.equals(appRealm.getLocalId(), realmId)) {
        InternalException internalException = new InternalException("The realms between the row (" + realmId + ") and the known app or realm (" + appRealm.getLocalId() + ") are inconsistent ");
        return Future.failedFuture(internalException);
      }
    }
    return realmFuture
      .compose(realmResult -> {

        Long listId = row.getLong(ID_COLUMN);
        String listHandle = row.getString(HANDLE_COLUMN);
        Future<App> appFuture = Future.succeededFuture(knownApp);
        if (knownApp == null) {
          Long publisherAppId = row.getLong(APP_OWNER_COLUMN);
          appFuture = this.apiApp.getAppProvider()
            .getAppById(publisherAppId, realmResult);
        }

        Long ownerId = row.getLong(USER_OWNER_COLUMN);
        Future<User> ownerFuture = Future.succeededFuture();
        if (ownerId != null) {
          ownerFuture = apiApp.getUserProvider()
            .getUserById(ownerId, realmResult.getLocalId(), User.class, realmResult);
        }

        return Future
          .all(appFuture, ownerFuture)
          .onFailure(e -> {
            throw new InternalException(e);
          })
          .compose(mapper -> {

            JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);
            ListItem listItem = Json.decodeValue(jsonAppData.toBuffer(), ListItem.class);

            listItem.setLocalId(listId);
            listItem.setRealm(realmResult);
            this.computeGuid(listItem);
            listItem.setHandle(listHandle);
            App appResult = mapper.resultAt(0);
            if (appResult == null) {
              throw ValidationException.create("The app was not found", "appId", null);
            }
            listItem.setOwnerApp(appResult);
            User publisher = mapper.resultAt(1);
            if (publisher != null) {
              listItem.setOwnerUser(publisher);
            }

            listItem.setRealm(realmResult);

            return Future.succeededFuture(listItem);

          });
      });


  }

  public Future<ListItem> getListById(long listId, Realm realm) {

    String sql = "SELECT * " +
      "FROM \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " WHERE \n" +
      " " + ID_COLUMN + " = $1 " +
      " AND " + REALM_COLUMN + " = $2";
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
   * A utility function to create a publication easily from a post publication object
   *
   * @param listPostBody - the post object
   * @return the publication created
   */

  public Future<ListItem> postPublication(ListPostBody listPostBody) {

    /**
     * Realm
     */
    Future<Realm> realmFuture = null;
    if (listPostBody.getRealmIdentifier() != null) {
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromIdentifier(listPostBody.getRealmIdentifier());
    }

    /**
     * App
     */
    Future<App> futureApp;
    String publisherAppGuid = listPostBody.getOwnerAppGuid();
    AppProvider appProvider = apiApp.getAppProvider();
    if (publisherAppGuid != null) {
      futureApp = appProvider
        .getAppByGuid(publisherAppGuid);
    } else {
      URI publisherAppUri = listPostBody.getOwnerAppUri();
      if (publisherAppUri == null) {

        String publisherEmail = listPostBody.getOwnerUserEmail();
        if (publisherEmail == null) {
          throw ValidationException.create("The app uri or guid should be given", "appGuid", null);
        }

        try {
          //String scheme = HttpsCertificateUtil.createOrGet().getHttpScheme();
          String scheme = "http";
          publisherAppUri = URI.create(
            scheme + "://" + BMailInternetAddress.of(publisherEmail)
              .getDomain()
          );
        } catch (AddressException e) {
          throw ValidationException.create("The publisher user email is not valid. Error: " + e.getMessage(), "publisherUserEmail", publisherEmail);
        }
      }
      if (realmFuture == null) {
        throw ValidationException.create("The realm handle or guid should be given with an app uri", "realmGuid", null);
      }
      URI finalPublisherAppUri = publisherAppUri;
      futureApp = realmFuture
        .onFailure(FailureStatic::failFutureWithTrace)
        .compose(realm -> appProvider.getAppByHandle(finalPublisherAppUri.toString(), realm));
    }

    /**
     * User
     */
    String ownerEmail = listPostBody.getOwnerUserEmail();
    String ownerGuid = listPostBody.getOwnerUserGuid();

    Future<User> futureUser = Future.succeededFuture(null);
    if (ownerEmail != null || ownerGuid != null) {
      UserProvider userProvider = apiApp.getUserProvider();
      Long userId = null;
      if (ownerGuid != null) {
        futureUser = userProvider.getUserByGuid(ownerGuid, User.class);
      } else {
        if (realmFuture == null) {
          throw ValidationException.create("The realm handle or guid should be given with an app uri", "realmGuid", null);
        }
        futureUser = realmFuture
          .onFailure(FailureStatic::failFutureWithTrace)
          .compose(realm -> {
            User userToGetOrCreate = new User();
            userToGetOrCreate.setLocalId(userId);
            userToGetOrCreate.setEmail(ownerEmail);
            userToGetOrCreate.setRealm(realm);
            return userProvider
              .getOrCreateUserFromEmail(userToGetOrCreate);
          });
      }
    }


    return Future
      .all(futureApp, futureUser)
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(successMapper -> {
        App app = successMapper.resultAt(0);
        User user = successMapper.resultAt(1);
        ListItem listItem = new ListItem();
        listItem.setRealm(app.getRealm());
        listItem.setName(listPostBody.getListName());
        listItem.setTitle(listPostBody.getListTitle());
        listItem.setDescription(listPostBody.getListDescription());
        listItem.setHandle(listPostBody.getListHandle());
        listItem.setOwnerUser(user);
        listItem.setOwnerApp(app);
        return this.upsertList(listItem);
      });

  }

  public Future<ListItem> getListByGuid(String publicationGuid) {

    Guid publicationGuidObject;
    try {
      publicationGuidObject = apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(LIST_GUID_PREFIX, publicationGuid);
    } catch (CastException e) {
      throw ValidationException.create("The publication guid is not valid", "guid", publicationGuid);
    }

    return this.apiApp.getRealmProvider()
      .getRealmFromId(publicationGuidObject.getRealmOrOrganizationId())
      .compose(realm -> getListById(publicationGuidObject.validateRealmAndGetFirstObjectId(realm.getLocalId()), realm));
  }

  public Future<java.util.List<ListSummary>> getListsSummary(Realm realm) {

    return jdbcPool.preparedQuery(
        "SELECT list.list_id, list.list_handle, app.app_uri, count(registration.registration_user_id) subscriber_count\n" +
          "FROM " +
          JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " list \n" +
          " LEFT JOIN " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + ListRegistrationProvider.TABLE_NAME + " registration\n" +
          "    on list.list_id = registration.registration_list_id\n" +
          " JOIN " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + AppProvider.TABLE_NAME + " app\n" +
          "    on list.list_owner_app_id = app.app_id\n" +
          "where list_realm_id = $1\n" +
          "group by list.list_id, list.list_handle, app.app_uri")
      .execute(Tuple.of(realm.getLocalId()))
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(publicationRows -> {


        java.util.List<ListSummary> futurePublications = new ArrayList<>();
        for (Row row : publicationRows) {
          ListSummary listSummary = new ListSummary();

          // List Id
          Long listId = row.getLong(ID_COLUMN);
          String listGuid = apiApp.createGuidFromRealmAndObjectId(LIST_GUID_PREFIX, realm, listId).toString();
          listSummary.setGuid(listGuid);

          // List Handle
          String listHandle = row.getString(HANDLE_COLUMN);
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

  public Future<java.util.List<ListItem>> getListsForApp(App app) {
    String sql = "SELECT * FROM " +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " where \n" +
      " " + APP_OWNER_COLUMN + " = $1" +
      " AND " + REALM_COLUMN + " = $2";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(app.getLocalId(), app.getRealm().getLocalId()))
      .onFailure(t -> LOGGER.error("Error getListsForApp with the Sql:\n" + sql, t))
      .compose(listRows -> {

        /**
         * the {@link CompositeFuture#all(java.util.List)}  all function } does not
         * take other thing than a raw future
         */
        java.util.List<Future<ListItem>> futureLists = new ArrayList<>();
        for (Row row : listRows) {
          Future<ListItem> futurePublication = getListFromRow(row, app, null);
          futureLists.add(futurePublication);
        }

        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future
          .all(futureLists)
          .onFailure(FailureStatic::failFutureWithTrace)
          .map(CompositeFuture::<ListItem>list);
      });
  }

  public Guid getGuidObject(String listGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(ListProvider.LIST_GUID_PREFIX, listGuid);
  }

  public Future<ListItem> getListByHandle(String listHandle, Realm realm) {
    String sql = "SELECT * " +
      "FROM \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " WHERE \n" +
      " " + HANDLE_COLUMN + " = $1 " +
      " AND " + REALM_COLUMN + " = $2";
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

  public ListItem toTemplateClone(ListItem list) {
    return toClone(list, true);
  }

  private ListItem toClone(ListItem listItem, boolean template) {
    ListItem listClone = JsonObject.mapFrom(listItem).mapTo(ListItem.class);


    App ownerApp = listClone.getOwnerApp();
    AppProvider appProvider = apiApp.getAppProvider();
    if (template) {
      ownerApp = appProvider.toTemplateClone(ownerApp);
    } else {
      ownerApp = appProvider.toPublicClone(ownerApp);
    }
    listClone.setOwnerApp(ownerApp);

    User ownerUser = listClone.getOwnerUser();
    if (ownerUser == null && template) {
      // owner user is the user app
      ownerUser = listItem.getOwnerApp().getUser();
    }
    UserProvider userProvider = apiApp.getUserProvider();
    if (ownerUser != null) {
      if (template) {
        ownerUser = userProvider.toTemplateCloneWithoutRealm(ownerUser);
      } else {
        ownerUser = userProvider.toPublicCloneWithoutRealm(ownerUser);
      }
      listClone.setOwnerUser(ownerUser);
    }

//    URI registrationUrl = EraldyMemberApp
//      .get()
//      .getPublicRequestUriForOperationPath(ListRegistrationFlow.getRegistrationOperationPath(registrationList))
//      .addQueryProperty(OAuthQueryProperty.REALM_GUID, registrationList.getRealm().getGuid())
//      .toUri();
    URI registrationUrl = URI.create("https://example.com/todo/registration");
    listClone.setRegistrationUrl(registrationUrl);

    /**
     * Realm and id null
     */
    listClone.setLocalId(null);
    listClone.setRealm(null);
    return listClone;
  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

}
