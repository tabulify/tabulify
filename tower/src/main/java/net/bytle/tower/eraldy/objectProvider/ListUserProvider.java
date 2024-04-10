package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.json.schema.ValidationException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.pojo.input.ListUserProps;
import net.bytle.tower.eraldy.jackson.JacksonListUserSourceDeserializer;
import net.bytle.tower.eraldy.jackson.JacksonListUserSourceSerializer;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.ListItemMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.type.Strings;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.JdbcSchemaManager;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
  public static final String STATUS_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "status";
  public static final String ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN;
  public static final String LIST_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN;
  public static final String USER_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  public static final String IN_SOURCE_ID_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_source_id";
  public static final String IN_OPT_IN_ORIGIN_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_origin";
  public static final String IN_OPT_IN_IP_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_ip";
  public static final String IN_OPT_IN_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_time";
  public static final String OUT_OPT_OUT_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "out_opt_out_time";

  public static final String IN_OPT_IN_CONFIRMATION_IP_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_confirmation_ip";
  public static final String IN_OPT_IN_CONFIRMATION_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + "in_opt_in_confirmation_time";
  private static final String GUID_PREFIX = "liu";
  static final String REALM_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + RealmProvider.REALM_ID_COLUMN;

  private final EraldyApiApp apiApp;
  private static final String CREATION_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MODIFICATION_TIME_COLUMN = LIST_USER_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;

  private final Pool jdbcPool;
  private final String registrationsBySearchTermSql;
  private final ObjectMapper apiMapper;


  public ListUserProvider(EraldyApiApp apiApp) {

    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();
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

    // the sql is too big to be inlined in Java
    String registrationPath = "/db/parameterized-statement/list-registration-users-by-search-term.sql";
    this.registrationsBySearchTermSql = Strings.createFromResource(ListUserProvider.class, registrationPath).toString();
    if (this.registrationsBySearchTermSql == null) {
      throw new InternalException("The registration by search sql was not found in the resource path (" + registrationPath + ")");
    }

  }

  private void computeGuidForListUserObject(ListUser listUser) {
    if (listUser.getGuid() != null) {
      return;
    }
    String guid = this.getGuidObjectFromLocalIds(
        listUser.getList().getRealm(),
        listUser.getList().getLocalId(),
        listUser.getUser().getLocalId()
      )
      .toString();
    listUser.setGuid(guid);
  }

  @SuppressWarnings("unused")
  private Future<ListUser> updateListUser(ListUser listUser, ListUserProps listUserProps) {

    ListUserStatus status = listUserProps.getStatus();
    if(status==null){
      return Future.succeededFuture(listUser);
    }

    if(status == listUser.getStatus()){
      return Future.succeededFuture(listUser);
    }

    /**
     * Only unsubscription for now
     */
    if(status!=ListUserStatus.UNSUBSCRIBED){
      return Future.succeededFuture(listUser);
    }

    String sql = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " SET\n" +
      "  " + STATUS_COLUMN + " = $1,\n" +
      "  " + OUT_OPT_OUT_TIME_COLUMN + " = $2,\n" +
      "  " + MODIFICATION_TIME_COLUMN + " = $3\n" +
      "where\n" +
      "  " + REALM_COLUMN + " = $4\n" +
      "AND  " + ID_COLUMN + " = $5\n" +
      "AND  " + USER_ID_COLUMN + " = $6\n";

    return jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        listUser.getStatus().getValue(),
        DateTimeService.getNowInUtc(),
        listUser.getList().getRealm().getLocalId(),
        listUser.getList().getLocalId(),
        listUser.getUser().getLocalId()
      ))
      .onFailure(e -> LOGGER.error("List User Update Sql Error " + e.getMessage() + ". With Sql:\n" + sql, e))
      .compose(e->Future.succeededFuture(listUser));
  }

  public Future<ListUser> insertListUser(User user, ListObject list, ListUserProps listUserProps) {

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


    ListUser listUser = getListUser(user, list, listUserProps);

    return jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        listUser.getList().getRealm().getLocalId(),
        listUser.getList().getLocalId(),
        listUser.getUser().getLocalId(),
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
  private ListUser getListUser(User user, ListObject list, ListUserProps listUserProps) {
    ListUser listUser = new ListUser();
    listUser.setUser(user);
    listUser.setList(list);
    listUser.setInSourceId(listUserProps.getInListUserSource());
    listUser.setInOptInOrigin(listUserProps.getInOptInOrigin());
    listUser.setInOptInIp(listUserProps.getInOptInIp());
    listUser.setInOptInTime(listUserProps.getInOptInTime());
    listUser.setInOptInConfirmationIp(listUserProps.getInOptInConfirmationIp());
    listUser.setInOptInConfirmationTime(listUserProps.getInOptInConfirmationTime());
    listUser.setStatus(ListUserStatus.OK);
    this.computeGuidForListUserObject(listUser);
    return listUser;
  }


  private Future<ListUser> getListUserFromDatabaseRow(Row row) {

    Long realmId = row.getLong(REALM_COLUMN);

    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(realmId)
      .compose(realm -> {

        Long listId = row.getLong(LIST_ID_COLUMN);
        Future<ListObject> publicationFuture = apiApp.getListProvider().getListById(listId, realm);

        Long userId = row.getLong(USER_ID_COLUMN);
        Future<User> publisherFuture = apiApp.getUserProvider()
          .getUserByLocalId(userId, realm.getLocalId(), User.class, realm);

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

            this.computeGuidForListUserObject(listUser);

            return Future.succeededFuture(listUser);
          });
      });


  }

  public Future<ListUser> getListUserByListAndUser(ListObject listObject, User user) {
    if (!Objects.equals(listObject.getRealm().getLocalId(), user.getRealm().getLocalId())) {
      throw new InternalException("The realm should be the same between a list and a user for a registration");
    }
    return getListUserByLocalIds(listObject.getLocalId(), user.getLocalId(), listObject.getRealm().getLocalId());
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
        return getListUserFromDatabaseRow(row);
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
        "       realm_user.user_email_address as user_email\n" +
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


  private Guid getGuidObjectFromHash(String listUserGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndTwoObjectId(GUID_PREFIX, listUserGuid);
  }

  private Guid getGuidObjectFromLocalIds(Realm realm, Long listId, Long userId) {

    return apiApp.createGuidStringFromRealmAndTwoObjectId(
      GUID_PREFIX,
      realm.getLocalId(),
      listId,
      userId
    );

  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  public void getActiveListUsers(Mailing mailing) {
    /**
     * The query on the whole set
     * (without search term)
     */
//    sql = "SELECT *\n" +
//      "FROM (select *\n" +
//      "      from (SELECT ROW_NUMBER() OVER (ORDER BY list_user_creation_time DESC) AS rn,\n" +
//      "                   *\n" +
//      "            FROM cs_realms.realm_list_user registration\n" +
//      "            where registration.list_user_realm_id = $1\n" +
//      "              AND registration.list_user_list_id = $2) registration\n" +
//      "      where rn >= 1 + $3::BIGINT * $4::BIGINT\n" +
//      "        and rn < $5::BIGINT * $6::BIGINT + 1" +
//      "       ) registration_pages\n" +
//      "         JOIN cs_realms.realm_user realm_user\n" +
//      "              on registration_pages.list_user_user_id = realm_user.user_id\n" +
//      "                  and registration_pages.list_user_realm_id = realm_user.user_realm_id\n" +
//      "        order by registration_pages.list_user_creation_time desc";
//    Tuple sqlParameters = Tuple.of(
//      realmId,
//      listId,
//      pageSize,
//      pageId,
//      pageSize,
//      pageId + 1
//    );
  }
}
