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
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.ListItemMixin;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.Postgres;
import net.bytle.type.Strings;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.JdbcSchemaManager;
import net.bytle.vertx.TowerFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Manage the get/upsert of a {@link ListUser} object asynchronously
 * <p>
 * The primary key is a compose of list and user pk
 *
 */
public class ListUserProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(ListUserProvider.class);

  static final String TABLE_NAME = "realm_list_user";
  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String LIST_USER_PREFIX = "list_user";
  public static final String STATUS_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "status";
  public static final String ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + ListProvider.ID_COLUMN;
  public static final String LIST_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + ListProvider.ID_COLUMN;
  public static final String USER_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  private static final String GUID_PREFIX = "liu";
  static final String REALM_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + RealmProvider.ID_COLUMN;
  public static final Integer OK_STATUS = 0;
  private static final String DATA_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "data";
  private final EraldyApiApp apiApp;
  private static final String CREATION_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MODIFICATION_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private final PgPool jdbcPool;
  private final String registrationsBySearchTermSql;
  private final ObjectMapper apiMapper;


  public ListUserProvider(EraldyApiApp apiApp) {

    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getApexDomain().getHttpServer().getServer().getJdbcPool();
    this.apiMapper = apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .addMixIn(ListItem.class, ListItemMixin.class)
      .build();

    // the sql is too big to be inlined in Java
    String registrationPath = "/db/parameterized-statement/list-registration-users-by-search-term.sql";
    this.registrationsBySearchTermSql = Strings.createFromResource(ListUserProvider.class, registrationPath).toString();
    if (this.registrationsBySearchTermSql == null) {
      throw new InternalException("The registration by search sql was not found in the resource path (" + registrationPath + ")");
    }

  }


  /**
   * @param listUser - the publication to make public
   * @return the publication without id, realm and with a guid
   */
  public ListUser toPublicClone(ListUser listUser) {

    return toClone(listUser, false);
  }

  /**
   * @param listUser - the registration
   * @param forTemplate      - if true, the data have default (for instance, the username would never be empty)
   */
  private ListUser toClone(ListUser listUser, boolean forTemplate) {

    ListUser listUserClone = JsonObject.mapFrom(listUser).mapTo(ListUser.class);

    /**
     * User
     */
    User subscriberUser = listUser.getUser();
    if (subscriberUser != null) {
      User publicCloneWithoutRealm;
      UserProvider userProvider = this.apiApp.getUserProvider();
      if (!forTemplate) {
        publicCloneWithoutRealm = userProvider.toPublicCloneWithoutRealm(subscriberUser);
      } else {
        publicCloneWithoutRealm = userProvider.toTemplateCloneWithoutRealm(subscriberUser);
      }
      listUserClone.setUser(publicCloneWithoutRealm);
    }

    /**
     * List
     */
    ListProvider listProvider = this.apiApp.getListProvider();
    ListItem listItem = listUser.getList();
    if (!forTemplate) {
      listItem = listProvider.toPublicClone(listItem);
    } else {
      listItem = listProvider.toTemplateClone(listItem);
    }
    listUserClone.setList(listItem);


    return listUserClone;
  }


  /**
   * @param listUser the registration to upsert
   * @return the realm with the id
   */
  public Future<ListUser> upsertListUser(ListUser listUser) {


    User subscriberUser = listUser.getUser();
    if (subscriberUser == null) {
      return Future.failedFuture(new InternalError("The subscriber user is mandatory when inserting a publication subscription"));
    }
    Long subscriberId = subscriberUser.getLocalId();
    if (subscriberId == null) {
      throw new InternalException("The subscriber id of a user object should not be null");
    }
    ListItem listItem = listUser.getList();
    if (listItem == null) {
      return Future.failedFuture(new InternalError("The list is mandatory when upserting a registration"));
    }

    /**
     * Realm check
     */
    if (!(subscriberUser.getRealm().getLocalId().equals(listItem.getRealm().getLocalId()))) {
      return Future.failedFuture(new InternalError("Inconsistency: The realm is not the same for the list (" + listItem.getRealm().getHandle() + " and the subscriber (" + subscriberUser.getRealm().getHandle() + ")"));
    }

    Long listId = listItem.getLocalId();
    if (listId == null) {
      return Future.failedFuture(new InternalError("The list id is mandatory when inserting a registration"));
    }

    /**
     * No upsert sql statement (see identifier.md)
     * (Even if this case it had been possible
     * because there is no object id)
     */
    return updateRegistrationAndGetRowSet(listUser)
      .compose(rowSet -> {
        if (rowSet.rowCount() == 0) {
          return insertRegistration(listUser);
        }
        this.computeGuid(listUser);
        return Future.succeededFuture(listUser);
      });

  }

  private Future<RowSet<Row>> updateRegistrationAndGetRowSet(ListUser listUser) {

    String sql = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " SET\n" +
      "  " + STATUS_COLUMN + " = $1,\n" +
      "  " + DATA_COLUMN + " = $2,\n" +
      "  " + MODIFICATION_COLUMN + " = $3\n" +
      "where\n" +
      "  " + REALM_COLUMN + " = $4\n" +
      "AND  " + ID_COLUMN + " = $5\n" +
      "AND  " + USER_ID_COLUMN + " = $6\n";

    return jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        listUser.getStatus(),
        this.getDatabaseObject(listUser),
        DateTimeUtil.getNowUtc(),
        listUser.getList().getRealm().getLocalId(),
        listUser.getList().getLocalId(),
        listUser.getUser().getLocalId()
      ))
      .onFailure(e -> LOGGER.error("Registration Update Sql Error " + e.getMessage() + ". With Sql:\n" + sql, e));
  }

  public Future<ListUser> insertRegistration(ListUser listUser) {

    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + USER_ID_COLUMN + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + STATUS_COLUMN + ",\n" +
      "  " + CREATION_TIME_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6)";


    return jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        listUser.getList().getRealm().getLocalId(),
        listUser.getList().getLocalId(),
        listUser.getUser().getLocalId(),
        this.getDatabaseObject(listUser),
        OK_STATUS,
        DateTimeUtil.getNowUtc()
      ))
      .onFailure(e -> LOGGER.error("Registration Insert Sql Error " + e.getMessage() + ". With Sql:\n" + sql, e))
      .compose(rows -> {
        this.computeGuid(listUser);
        return Future.succeededFuture(listUser);
      });

  }

  private JsonObject getDatabaseObject(ListUser listUser) {
    JsonObject data = JsonObject.mapFrom(listUser);
    data.remove("list");
    data.remove("subscriber");
    data.remove(Guid.GUID);
    return data;
  }


  private Future<ListUser> getRegistrationFromRow(Row row) {

    Long realmId = row.getLong(REALM_COLUMN);
    Future<Realm> realmFuture = this.apiApp.getRealmProvider()
      .getRealmFromId(realmId);

    return realmFuture
      .compose(realm -> {

        Long listId = row.getLong(LIST_ID_COLUMN);
        Future<ListItem> publicationFuture = apiApp.getListProvider().getListById(listId, realm);

        Long subscriberId = row.getLong(USER_ID_COLUMN);
        Future<User> publisherFuture = apiApp.getUserProvider()
          .getUserById(subscriberId, realm.getLocalId(), User.class, realm);

        return Future
          .all(publicationFuture, publisherFuture)
          .onFailure(e -> {
            throw new InternalException(e);
          })
          .compose(mapper -> {

            JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);
            ListUser listUser = Json.decodeValue(jsonAppData.toBuffer(), ListUser.class);

            ListItem listItemResult = mapper.resultAt(0);
            User subscriberResult = mapper.resultAt(1);

            listUser.setList(listItemResult);
            listUser.setUser(subscriberResult);

//        LocalDateTime creationTime = row.getOffsetDateTime(SUBSCRIPTION_PREFIX + COLUMN_PART_SEP + CREATION_TIME);
//        subscription.setCreationTime(creationTime);

            return Future.succeededFuture(listUser);
          });
      });


  }

  public Future<ListUser> getRegistrationByListAndUser(ListItem listItem, User user) {
    if (!Objects.equals(listItem.getRealm().getLocalId(), user.getRealm().getLocalId())) {
      throw new InternalException("The realm should be the same between a list and a user for a registration");
    }
    return getRegistrationByLocalIds(listItem.getLocalId(), user.getLocalId(), listItem.getRealm().getLocalId());
  }

  private Future<ListUser> getRegistrationByLocalIds(Long listId, Long userId, Long realmId) {
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
      .onFailure(e -> LOGGER.error("Unable to retrieve the registration. Error: " + e.getMessage() + ". Sql: \n" + sql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() > 1) {
          InternalException internalException = new InternalException("Registration Get: More than one rows (" + userRows.size() + ") returned from the registration ( " + realmId + ", " + listId + ", " + userId);
          return Future.failedFuture(internalException);
        }

        Row row = userRows.iterator().next();
        return getRegistrationFromRow(row);
      });
  }

  public Future<ListUser> getListUserByGuid(String listUserGuid) {

    Guid guidObject;
    try {
      guidObject = this.getGuidObject(listUserGuid);
    } catch (CastException e) {
      throw ValidationException.create("The listUser guid (" + listUserGuid + ") is not valid", "listUserIdentifier", listUserGuid);
    }

    long realmId = guidObject.getRealmOrOrganizationId();
    long listId = guidObject.validateRealmAndGetFirstObjectId(realmId);
    long userId = guidObject.validateAndGetSecondObjectId(realmId);

    return getRegistrationByLocalIds(listId, userId, realmId);

  }

  private Guid getGuidObject(String listUserGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(GUID_PREFIX, listUserGuid);
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
    String sql;
    Tuple sqlParameters;
    if (searchTerm != null && !searchTerm.trim().isEmpty()) {

      sql = this.registrationsBySearchTermSql;
      sqlParameters = Tuple.of(
        realmId,
        "%" + searchTerm + "%",
        listId,
        pageSize,
        pageId,
        pageSize,
        pageId + 1
      );

    } else {
      /**
       * The query on the whole set
       * (without search term)
       */
      sql = "SELECT registration_pages.list_user_creation_time,\n" +
        "       registration_pages.list_user_user_id as user_id,\n" +
        "       registration_pages.list_user_status,\n" +
        "       realm_user.user_email as user_email\n" +
        "FROM (select *\n" +
        "      from (SELECT ROW_NUMBER() OVER (ORDER BY list_user_creation_time DESC) AS rn,\n" +
        "                   *\n" +
        "            FROM cs_realms.realm_list_user registration\n" +
        "            where registration.list_user_realm_id = $1\n" +
        "              AND registration.list_user_list_id = $2) registration\n" +
        "      where rn >= 1 + $3::BIGINT * $4::BIGINT\n" +
        "        and rn < $5::BIGINT * $6::BIGINT + 1" +
        "       ) registration_pages\n" +
        "         JOIN cs_realms.realm_user realm_user\n" +
        "              on registration_pages.list_user_user_id = realm_user.user_id\n" +
        "                  and registration_pages.list_user_realm_id = realm_user.user_realm_id\n" +
        "        order by registration_pages.list_user_creation_time desc";
      sqlParameters = Tuple.of(
        realmId,
        listId,
        pageSize,
        pageId,
        pageSize,
        pageId + 1
      );
    }


    return jdbcPool.preparedQuery(sql)
      .execute(sqlParameters)
      .recover(err -> Future.failedFuture(TowerFailureException.builder()
        .setMessage("Error while getting the user of the list with the sql: " + sql)
        .setCauseException(err)
        .build()))
      .compose(registrationRows -> {

        java.util.List<ListUserShort> futureSubscriptions = new ArrayList<>();
        if (registrationRows.size() == 0) {
          return Future.succeededFuture(futureSubscriptions);
        }

        for (Row row : registrationRows) {

          ListUserShort listUserShort = new ListUserShort();
          String userIdAliasInQuery = "user_id";
          Long userId = row.getLong(userIdAliasInQuery);
          String guidString = apiApp.createGuidStringFromRealmAndTwoObjectId(GUID_PREFIX, realmId, listId, userId).toString();
          listUserShort.setGuid(guidString);
          String userGuid = apiApp.getUserProvider().createUserGuid(realmId, userId).toString();
          listUserShort.setUserGuid(userGuid);
          String userEmailAliasInQuery = "user_email";
          String subscriberEmail = row.getString(userEmailAliasInQuery);
          listUserShort.setUserEmail(subscriberEmail);
          LocalDateTime localDateTime = row.getLocalDateTime(CREATION_TIME_COLUMN);
          listUserShort.setConfirmationTime(localDateTime);
          Integer registrationStatus = row.getInteger(STATUS_COLUMN);
          listUserShort.setStatus(registrationStatus);
          futureSubscriptions.add(listUserShort);

        }
        return Future.succeededFuture(futureSubscriptions);

      });
  }

  public Future<ListUser> getRegistrationByListGuidAndSubscriberEmail(String listGuid, String subscriberEmail) {
    Guid listGuidObject;
    try {
      listGuidObject = apiApp.getListProvider().getGuidObject(listGuid);
    } catch (CastException e) {
      throw ValidationException.create("The listGuid is not valid", "listGuid", listGuid);
    }

    String sql = "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME
      + " JOIN cs_realms." + UserProvider.TABLE_NAME + " userTable"
      + " ON userTable.user_id = cs_realms.realm_list_user.list_user_user_id"
      + " WHERE "
      + " list_user_realm_id = $1 "
      + "AND list_user_list_id = $2 "
      + "and userTable.user_email = $3";
    long realmId = listGuidObject.getRealmOrOrganizationId();
    Tuple parameters = Tuple.of(
      realmId,
      listGuidObject.validateRealmAndGetFirstObjectId(realmId),
      subscriberEmail
    );
    return jdbcPool
      .preparedQuery(sql)
      .execute(parameters)
      .recover(e -> Future.failedFuture(TowerFailureException.builder()
        .setMessage("Get users of a list by list guid and subscriber email error with the sql " + sql)
        .setCauseException(e)
        .build())
      )
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() > 1) {
          return Future.failedFuture(new InternalError("Too much registration for list (" + listGuidObject + "), email (" + subscriberEmail + ")"));
        }

        Row row = userRows.iterator().next();
        return getRegistrationFromRow(row);
      });
  }

  private void computeGuid(ListUser listUser) {
    if (listUser.getGuid() != null) {
      return;
    }
    String guid = apiApp.createGuidStringFromRealmAndTwoObjectId(
        GUID_PREFIX,
        listUser.getList().getRealm(),
        listUser.getList().getLocalId(),
        listUser.getUser().getLocalId()
      )
      .toString();
    listUser.setGuid(guid);
  }


  public ListUser toTemplateClone(ListUser listUser) {
    return toClone(listUser, true);
  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }
}
