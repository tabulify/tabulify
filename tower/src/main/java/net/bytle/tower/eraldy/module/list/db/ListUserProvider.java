package net.bytle.tower.eraldy.module.list.db;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.pojo.input.ListUserInputProps;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.ListItemMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListUserGuidDeserializer;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListUserGuidSerializer;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListUserSourceDeserializer;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListUserSourceSerializer;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.tower.eraldy.module.user.db.UserCols;
import net.bytle.tower.eraldy.module.user.model.UserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.db.*;
import net.bytle.vertx.jackson.JacksonMapperManager;
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
  public static final String LIST_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN;
  static final String REALM_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "realm_id";

  private final EraldyApiApp apiApp;

  private final ObjectMapper apiMapper;
  private final JdbcTable listUserTable;


  public ListUserProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {

    this.apiApp = apiApp;
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
    jacksonMapperManager
      .addDeserializer(ListUserSource.class, new JacksonListUserSourceDeserializer())
      .addSerializer(ListUserSource.class, new JacksonListUserSourceSerializer())
      .addDeserializer(ListUserGuid.class, new JacksonListUserGuidDeserializer(this.apiApp))
      .addSerializer(ListUserGuid.class, new JacksonListUserGuidSerializer(this.apiApp));


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

    assert Objects.equals(listUser.getUser().getRealm(), listUser.getList().getApp().getRealm()) : "The realm is not the same on the user and on the list";

    ListUserGuid guid = new ListUserGuid();
    guid.setRealmId(listUser.getUser().getRealm().getGuid().getLocalId());
    guid.setListId(listUser.getList().getGuid().getLocalId());
    guid.setUserId(listUser.getUser().getGuid().getLocalId());
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


    assert Objects.equals(user.getRealm(), list.getApp().getRealm()) : "The user (" + user + ") and list (" + list + ")+ does not have the same realm";

    JdbcInsert jdbcInsert = JdbcInsert.into(this.listUserTable);
    ListUser listUser = new ListUser();

    listUser.setUser(user);
    listUser.setList(list);
    this.updateGuid(listUser);

    jdbcInsert
      .addColumn(ListUserCols.REALM_ID, listUser.getGuid().getRealmId())
      .addColumn(ListUserCols.LIST_ID, listUser.getGuid().getListId())
      .addColumn(ListUserCols.USER_ID, listUser.getGuid().getUserId());

    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    listUser.setModificationTime(nowInUtc);
    jdbcInsert.addColumn(ListUserCols.MODIFICATION_TIME, listUser.getModificationTime());

    listUser.setCreationTime(nowInUtc);
    jdbcInsert.addColumn(ListUserCols.CREATION_TIME, listUser.getCreationTime());

    listUser.setInSourceId(listUserInputProps.getInListUserSource());
    jdbcInsert.addColumn(ListUserCols.IN_SOURCE_ID, listUser.getInSourceId());

    listUser.setInOptInOrigin(listUserInputProps.getInOptInOrigin());
    jdbcInsert.addColumn(ListUserCols.IN_OPT_IN_ORIGIN, listUser.getInOptInOrigin());

    listUser.setInOptInIp(listUserInputProps.getInOptInIp());
    jdbcInsert.addColumn(ListUserCols.IN_OPT_IN_IP, listUser.getInOptInIp());

    listUser.setInOptInTime(listUserInputProps.getInOptInTime());
    jdbcInsert.addColumn(ListUserCols.IN_OPT_IN_TIME, listUser.getInOptInTime());

    listUser.setInOptInConfirmationIp(listUserInputProps.getInOptInConfirmationIp());
    jdbcInsert.addColumn(ListUserCols.IN_OPT_IN_CONFIRMATION_IP, listUser.getInOptInConfirmationIp());

    listUser.setInOptInConfirmationTime(listUserInputProps.getInOptInConfirmationTime());
    jdbcInsert.addColumn(ListUserCols.IN_OPT_IN_CONFIRMATION_TIME, listUser.getInOptInConfirmationTime());

    listUser.setStatus(ListUserStatus.OK);
    jdbcInsert.addColumn(ListUserCols.STATUS_CODE, listUser.getStatus().getCode());

    // on conflict do nothing because the insert can happen twice if the user
    // click on the confirmation url twice
    jdbcInsert.onConflictPrimaryKey(JdbcOnConflictAction.DO_NOTHING);

    return jdbcInsert
      .execute()
      .compose(rows -> Future.succeededFuture(listUser));

  }


  private ListUser buildListUserFromDatabaseRow(JdbcRow row) {

    Long realmId = row.getLong(ListUserCols.REALM_ID);
    Long listId = row.getLong(ListUserCols.LIST_ID);
    Long userId = row.getLong(ListUserCols.USER_ID);

    ListUser listUser = new ListUser();

    ListObject list = new ListObject();
    ListGuid listGuid = new ListGuid();
    listGuid.setRealmId(realmId);
    listGuid.setLocalId(listId);
    list.setGuid(listGuid);
    listUser.setList(list);

    User user = new User();
    UserGuid userGuid = new UserGuid();
    userGuid.setRealmId(realmId);
    userGuid.setLocalId(userId);
    user.setGuid(userGuid);
    listUser.setUser(user);

    this.updateGuid(listUser);

    // scalar
    listUser.setStatus(ListUserStatus.fromValue(row.getInteger(ListUserCols.STATUS_CODE)));
    listUser.setCreationTime(row.getLocalDateTime(ListUserCols.CREATION_TIME));
    listUser.setModificationTime(row.getLocalDateTime(ListUserCols.MODIFICATION_TIME));
    listUser.setInSourceId(ListUserSource.fromValue(row.getInteger(ListUserCols.IN_SOURCE_ID)));
    listUser.setInOptInOrigin(row.getString(ListUserCols.IN_OPT_IN_ORIGIN));
    listUser.setInOptInIp(row.getString(ListUserCols.IN_OPT_IN_IP));
    listUser.setInOptInTime(row.getLocalDateTime(ListUserCols.IN_OPT_IN_TIME));
    listUser.setInOptInConfirmationIp(row.getString(ListUserCols.IN_OPT_IN_CONFIRMATION_IP));
    listUser.setInOptInConfirmationTime(row.getLocalDateTime(ListUserCols.IN_OPT_IN_CONFIRMATION_TIME));

    return listUser;


  }

  public Future<ListUser> getListUserByListAndUser(ListObject listObject, User user) {
    long listRealmId = listObject.getApp().getRealm().getGuid().getLocalId();
    long userRealmId = user.getRealm().getGuid().getLocalId();
    if (!Objects.equals(listRealmId, userRealmId)) {
      throw new InternalException("The realm should be the same between a list and a user for a listUser");
    }
    ListUserGuid listUserGuid = new ListUserGuid();
    listUserGuid.setUserId(user.getGuid().getLocalId());
    listUserGuid.setListId(listObject.getGuid().getLocalId());
    listUserGuid.setRealmId(listRealmId);
    return getListUserByGuid(listUserGuid)
      .compose(listUser -> {
        listUser.setUser(user);
        listUser.setList(listObject);
        return Future.succeededFuture(listUser);
      });
  }

  private Future<ListUser> getListUserByGuid(ListUserGuid listUserGuid) {


    return JdbcSelect.from(this.listUserTable)
      .addEqualityPredicate(ListUserCols.REALM_ID, listUserGuid.getRealmId())
      .addEqualityPredicate(ListUserCols.LIST_ID, listUserGuid.getListId())
      .addEqualityPredicate(ListUserCols.USER_ID, listUserGuid.getUserId())
      .execute()
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() > 1) {
          InternalException internalException = new InternalException("Registration Get: More than one rows (" + userRows.size() + ") returned from the registration ( " + listUserGuid+")");
          return Future.failedFuture(internalException);
        }

        JdbcRow row = userRows.iterator().next();
        return Future.succeededFuture(buildListUserFromDatabaseRow(row));
      });
  }

  public Future<ListUser> getListUserByGuidHash(String listUserGuid) {

    ListUserGuid guidObject;
    try {
      guidObject = this.apiApp.getJackson().getDeserializer(ListUserGuid.class).deserialize(listUserGuid);
    } catch (CastException e) {
      throw ValidationException.create("The listUser guid (" + listUserGuid + ") is not valid", "listUserIdentifier", listUserGuid);
    }

    return getListUserByGuid(guidObject);

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
          String guidString = apiApp.createGuidStringFromRealmAndTwoObjectId(ListUserGuid.GUID_PREFIX, realmId, listId, userId).toString();
          listUserShort.setGuid(guidString);
          String userGuid = apiApp.getUserProvider().createUserGuid(realmId, userId).toString();
          listUserShort.setUserGuid(userGuid);
          String subscriberEmail = row.getString(UserCols.EMAIL_ADDRESS);
          listUserShort.setUserEmail(subscriberEmail);
          LocalDateTime localDateTime = row.getLocalDateTime(ListUserCols.IN_OPT_IN_CONFIRMATION_TIME);
          listUserShort.setConfirmationTime(localDateTime);
          Integer registrationStatus = row.getInteger(ListUserCols.STATUS_CODE);
          listUserShort.setStatus(registrationStatus);
          futureSubscriptions.add(listUserShort);

        }
        return Future.succeededFuture(futureSubscriptions);

      });
  }


  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

}
