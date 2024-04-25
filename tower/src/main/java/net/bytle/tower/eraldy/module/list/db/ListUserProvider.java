package net.bytle.tower.eraldy.module.list.db;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.json.schema.ValidationException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.pojo.input.ListUserInputProps;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.ListItemMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListUserSourceDeserializer;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListUserSourceSerializer;
import net.bytle.tower.eraldy.module.user.db.UserCols;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.db.*;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Manage the get/upsert of a {@link ListUser} listing object asynchronously
 * <p>
 * The primary key is a composition of the list and user pk
 *
 */
public class ListUserProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(ListUserProvider.class);

  static final String TABLE_NAME = "realm_list_user";
  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String LIST_USER_PREFIX = "list_user";
  public static final String STATUS_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "status_code";
  public static final String ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN;
  public static final String LIST_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN;
  public static final String USER_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  public static final String IN_SOURCE_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_source_id";
  public static final String IN_OPT_IN_ORIGIN_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_origin";
  public static final String IN_OPT_IN_IP_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_ip";
  public static final String IN_OPT_IN_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_time";

  public static final String IN_OPT_IN_CONFIRMATION_IP_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_confirmation_ip";
  public static final String IN_OPT_IN_CONFIRMATION_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_confirmation_time";
  private static final String GUID_PREFIX = "liu";
  static final String REALM_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "realm_id";

  private final EraldyApiApp apiApp;
  private static final String CREATION_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MODIFICATION_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;

  private final Pool jdbcPool;
  private final ObjectMapper apiMapper;
  private final JdbcTable listUserTable;


  public ListUserProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {

    this.apiApp = apiApp;
    JdbcClient postgresClient = apiApp.getHttpServer().getServer().getPostgresClient();
    this.jdbcPool = postgresClient.getPool();
    JacksonMapperManager jacksonMapperManager = apiApp.getHttpServer().getServer().getJacksonMapperManager();
    this.apiMapper = jacksonMapperManager
      .jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .addMixIn(ListObject.class, ListItemMixinWithoutRealm.class)
      .build();

    /**
     * Register the deserializer
     */
    jacksonMapperManager.addDeserializer(ListUserSource.class, new JacksonListUserSourceDeserializer());
    jacksonMapperManager.addSerializer(ListUserSource.class, new JacksonListUserSourceSerializer());


    Map<JdbcColumn, JdbcColumn> foreignKeysUserColumn = new HashMap<>();
    foreignKeysUserColumn.put(ListUserCols.REALM_ID, UserCols.REALM_ID);
    foreignKeysUserColumn.put(ListUserCols.USER_ID, UserCols.ID);
    Map<JdbcColumn, JdbcColumn> foreignKeysListColumn = new HashMap<>();
    foreignKeysListColumn.put(ListUserCols.REALM_ID, ListCols.REALM_ID);
    foreignKeysListColumn.put(ListUserCols.LIST_ID, ListCols.ID);
    this.listUserTable = JdbcTable.build(jdbcSchema, TABLE_NAME, ListUserCols.values())
      .addPrimaryKeyColumn(ListUserCols.REALM_ID)
      .addPrimaryKeyColumn(ListUserCols.LIST_ID)
      .addPrimaryKeyColumn(ListUserCols.USER_ID)
      .addForeignKeyColumns(foreignKeysListColumn)
      .addForeignKeyColumns(foreignKeysUserColumn)
      .build();


  }

  public void updateGuid(ListUser listUser) {
    if (listUser.getGuid() != null) {
      return;
    }
    String guid = this.getGuidObjectFromLocalIds(
        listUser.getList().getApp().getRealm(),
        listUser.getList().getGuid().getLocalId(),
        listUser.getUser().getGuid().getLocalId()
      )
      .toString();
    listUser.setGuid(guid);
  }

  @SuppressWarnings("unused")
  private Future<ListUser> updateListUser(ListUser listUser, ListUserInputProps listUserInputProps) {


    ListUserStatus status = listUserInputProps.getStatus();
    if (status == null) {
      return Future.succeededFuture(listUser);
    }

    if (status == listUser.getStatus()) {
      return Future.succeededFuture(listUser);
    }

    /**
     * Only unsubscription for now
     */
    if (status != ListUserStatus.UNSUBSCRIBED) {
      return Future.succeededFuture(listUser);
    }

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.listUserTable)
      .addPredicateColumn(ListUserCols.REALM_ID, listUser.getList().getApp().getRealm().getGuid().getLocalId())
      .addPredicateColumn(ListUserCols.LIST_ID, listUser.getList().getGuid().getLocalId())
      .addPredicateColumn(ListUserCols.USER_ID, listUser.getUser().getGuid().getLocalId());


    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    listUser.setModificationTime(nowInUtc);
    jdbcUpdate.addUpdatedColumn(ListUserCols.MODIFICATION_TIME, listUser.getModificationTime());

    listUser.setStatus(status);
    jdbcUpdate.addUpdatedColumn(ListUserCols.STATUS_CODE, listUser.getStatus().getCode());

    listUser.setOutOptOutTime(nowInUtc);
    jdbcUpdate.addUpdatedColumn(ListUserCols.OUT_OPT_OUT_TIME, listUser.getOutOptOutTime());

    return jdbcUpdate
      .execute()
      .compose(e -> Future.succeededFuture(listUser));

  }

  public Future<ListUser> insertListUser(User user, ListObject list, ListUserInputProps listUserInputProps) {

    // on conflict do nothing because the insert can happen twice if the user
    // click on the url twice

    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + USER_ID_COLUMN + ",\n" +
      "  " + IN_SOURCE_ID_COLUMN + ",\n" +
      "  " + IN_OPT_IN_ORIGIN_COLUMN + ",\n" +
      "  " + IN_OPT_IN_IP_COLUMN + ",\n" +
      "  " + IN_OPT_IN_TIME_COLUMN + ",\n" +
      "  " + IN_OPT_IN_CONFIRMATION_IP_COLUMN + ",\n" +
      "  " + IN_OPT_IN_CONFIRMATION_TIME_COLUMN + ",\n" +
      "  " + STATUS_COLUMN + ",\n" +
      "  " + CREATION_TIME_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)\n" +
      "ON CONFLICT(" + REALM_COLUMN + "," + ID_COLUMN + "," + USER_ID_COLUMN + ") DO NOTHING";


    ListUser listUser = buildListUserFromUserListAndInput(user, list, listUserInputProps);

    return jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        listUser.getList().getApp().getRealm().getGuid().getLocalId(),
        listUser.getList().getGuid().getLocalId(),
        listUser.getUser().getGuid().getLocalId(),
        listUser.getInSourceId().getValue(),
        listUser.getInOptInOrigin(),
        listUser.getInOptInIp(),
        listUser.getInOptInTime(),
        listUser.getInOptInConfirmationIp(),
        listUser.getInOptInConfirmationTime(),
        listUser.getStatus(),
        DateTimeService.getNowInUtc()
      ))
      .onFailure(e -> LOGGER.error("List User Insert Sql Error " + e.getMessage() + ". With Sql:\n" + sql, e))
      .compose(rows -> Future.succeededFuture(listUser));

  }


  @NotNull
  private ListUser buildListUserFromUserListAndInput(User user, ListObject list, ListUserInputProps listUserInputProps) {
    ListUser listUser = new ListUser();
    listUser.setUser(user);
    listUser.setList(list);
    listUser.setInSourceId(listUserInputProps.getInListUserSource());
    listUser.setInOptInOrigin(listUserInputProps.getInOptInOrigin());
    listUser.setInOptInIp(listUserInputProps.getInOptInIp());
    listUser.setInOptInTime(listUserInputProps.getInOptInTime());
    listUser.setInOptInConfirmationIp(listUserInputProps.getInOptInConfirmationIp());
    listUser.setInOptInConfirmationTime(listUserInputProps.getInOptInConfirmationTime());
    listUser.setStatus(ListUserStatus.OK);
    this.updateGuid(listUser);
    return listUser;
  }


  private Future<ListUser> buildListUserFromDatabaseRow(Row row) {

    Long realmId = row.getLong(REALM_COLUMN);

    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(realmId)
      .compose(realm -> {

        Long listId = row.getLong(LIST_ID_COLUMN);
        Future<ListObject> publicationFuture = apiApp.getListProvider().getListById(listId, realm);

        Long userId = row.getLong(USER_ID_COLUMN);
        Future<User> publisherFuture = apiApp.getUserProvider()
          .getUserByLocalId(userId, realm);

        return Future
          .all(publicationFuture, publisherFuture)
          .recover(e -> Future.failedFuture(new InternalException("A future error happened while building a list user from row", e)))
          .compose(compositeFuture -> {


            ListUser listUser = new ListUser();

            ListObject listObjectResult = compositeFuture.resultAt(0);
            User userResult = compositeFuture.resultAt(1);

            listUser.setList(listObjectResult);
            listUser.setUser(userResult);
            listUser.setStatus(ListUserStatus.fromValue(row.getInteger(STATUS_COLUMN)));
            listUser.setCreationTime(row.getLocalDateTime(CREATION_TIME_COLUMN));
            listUser.setModificationTime(row.getLocalDateTime(MODIFICATION_TIME_COLUMN));

            listUser.setInSourceId(ListUserSource.fromValue(row.getInteger(IN_SOURCE_ID_COLUMN)));
            listUser.setInOptInOrigin(row.getString(IN_OPT_IN_ORIGIN_COLUMN));
            listUser.setInOptInIp(row.getString(IN_OPT_IN_IP_COLUMN));
            listUser.setInOptInTime(row.getLocalDateTime(IN_OPT_IN_TIME_COLUMN));
            listUser.setInOptInConfirmationIp(row.getString(IN_OPT_IN_CONFIRMATION_IP_COLUMN));
            listUser.setInOptInConfirmationTime(row.getLocalDateTime(IN_OPT_IN_CONFIRMATION_TIME_COLUMN));

            this.updateGuid(listUser);

            return Future.succeededFuture(listUser);
          });
      });


  }

  public Future<ListUser> getListUserByListAndUser(ListObject listObject, User user) {
    if (!Objects.equals(listObject.getApp().getRealm().getGuid().getLocalId(), user.getRealm().getGuid().getLocalId())) {
      throw new InternalException("The realm should be the same between a list and a user for a registration");
    }
    return getListUserByLocalIds(listObject.getGuid().getLocalId(), user.getGuid().getLocalId(), listObject.getApp().getRealm().getGuid().getLocalId());
  }

  private Future<ListUser> getListUserByLocalIds(Long listId, Long userId, Long realmId) {
    String sql = "SELECT * " +
      "FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " +
      REALM_COLUMN + " = $1\n" +
      "AND " + LIST_ID_COLUMN + " = $2\n " +
      "and " + USER_ID_COLUMN + " = $3";

    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(
          realmId,
          listId,
          userId
        )
      )
      .recover(e -> Future.failedFuture(TowerFailureException.builder()
        .setMessage("Unable to retrieve the list user by local ids. Error: " + e.getMessage() + ". Sql: \n" + sql)
        .setCauseException(e)
        .build()
      ))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() > 1) {
          InternalException internalException = new InternalException("Registration Get: More than one rows (" + userRows.size() + ") returned from the registration ( " + realmId + ", " + listId + ", " + userId);
          return Future.failedFuture(internalException);
        }

        Row row = userRows.iterator().next();
        return buildListUserFromDatabaseRow(row);
      });
  }

  public Future<ListUser> getListUserByGuidHash(String listUserGuid) {

    Guid guidObject;
    try {
      guidObject = this.getGuidObjectFromHash(listUserGuid);
    } catch (CastException e) {
      throw ValidationException.create("The listUser guid (" + listUserGuid + ") is not valid", "listUserIdentifier", listUserGuid);
    }

    long realmId = guidObject.getRealmOrOrganizationId();
    long listId = guidObject.validateRealmAndGetFirstObjectId(realmId);
    long userId = guidObject.validateAndGetSecondObjectId(realmId);

    return getListUserByLocalIds(listId, userId, realmId);

  }


  public Future<java.util.List<ListUserShort>> getListUsers(String listGuid,
                                                            Long pageId, Long pageSize, String searchTerm) {
    Guid guid;
    try {
      guid = apiApp.getListProvider().getGuidObject(listGuid);
    } catch (CastException e) {
      return Future.failedFuture(e);
    }

    long realmId = guid.getRealmOrOrganizationId();
    long listId = guid.validateRealmAndGetFirstObjectId(realmId);

    JdbcPagination pagination = new JdbcPagination();
    pagination.setSearchTerm(searchTerm);
    pagination.setPageSize(pageSize);
    pagination.setPageId(pageId);
    return JdbcPaginatedSelect.from(this.listUserTable)
      .setPagination(pagination)
      .addExtraSelectColumn(UserCols.EMAIL_ADDRESS)
      .setSearchColumn(UserCols.EMAIL_ADDRESS)
      .addOrderBy(ListUserCols.CREATION_TIME)
      .addEqualityPredicate(ListUserCols.REALM_ID, realmId)
      .addEqualityPredicate(ListUserCols.LIST_ID, listId)
      .execute()
      .compose(registrationRows -> {

        java.util.List<ListUserShort> futureSubscriptions = new ArrayList<>();
        if (registrationRows.size() == 0) {
          return Future.succeededFuture(futureSubscriptions);
        }

        for (JdbcRow row : registrationRows) {

          ListUserShort listUserShort = new ListUserShort();
          Long userId = row.getLong(ListUserCols.USER_ID);
          String guidString = apiApp.createGuidStringFromRealmAndTwoObjectId(GUID_PREFIX, realmId, listId, userId).toString();
          listUserShort.setGuid(guidString);
          String userGuid = apiApp.getUserProvider().createUserGuid(realmId, userId).toString();
          listUserShort.setUserGuid(userGuid);
          String subscriberEmail = row.getString(UserCols.EMAIL_ADDRESS);
          listUserShort.setUserEmail(subscriberEmail);
          LocalDateTime localDateTime = row.getLocalDateTime(ListUserCols.OPT_IN_CONFIRMATION_TIME);
          listUserShort.setConfirmationTime(localDateTime);
          Integer registrationStatus = row.getInteger(ListUserCols.STATUS_CODE);
          listUserShort.setStatus(registrationStatus);
          futureSubscriptions.add(listUserShort);

        }
        return Future.succeededFuture(futureSubscriptions);

      });
  }


  private Guid getGuidObjectFromHash(String listUserGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndTwoObjectId(GUID_PREFIX, listUserGuid);
  }

  private Guid getGuidObjectFromLocalIds(Realm realm, Long listId, Long userId) {

    return apiApp.createGuidStringFromRealmAndTwoObjectId(
      GUID_PREFIX,
      realm.getGuid().getLocalId(),
      listId,
      userId
    );

  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

}
