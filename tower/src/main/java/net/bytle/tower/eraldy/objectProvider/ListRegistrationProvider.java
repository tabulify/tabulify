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
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Manage the get/upsert of a {@link ListRegistration} object asynchronously
 * <p>
 * They don't have any id as the table can be become huge
 */
public class ListRegistrationProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(ListRegistrationProvider.class);

  static final String TABLE_NAME = "realm_list_registration";


  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String REGISTRATION_PREFIX = "registration";
  public static final String STATUS_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + "status";
  public static final String ID_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + ListProvider.ID_COLUMN;
  public static final String LIST_ID_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + ListProvider.ID_COLUMN;
  public static final String USER_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  private static final String GUID_PREFIX = "reg";
  static final String REALM_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + RealmProvider.ID_COLUMN;
  public static final String REGISTERED_STATUS = "registered";
  private static final String DATA_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + "data";
  private final EraldyApiApp apiApp;
  private static final String CREATION_TIME_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MODIFICATION_COLUMN = REGISTRATION_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private final PgPool jdbcPool;
  private ObjectMapper apiMapper;


  public ListRegistrationProvider(EraldyApiApp apiApp) {

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
   * @param listRegistration - the publication to make public
   * @return the publication without id, realm and with a guid
   */
  public ListRegistration toPublicClone(ListRegistration listRegistration) {

    return toClone(listRegistration, false);
  }

  /**
   * @param listRegistration - the registration
   * @param forTemplate  - if true, the data have default (for instance, the username would never be empty)
   */
  private ListRegistration toClone(ListRegistration listRegistration, boolean forTemplate) {

    ListRegistration listRegistrationClone = JsonObject.mapFrom(listRegistration).mapTo(ListRegistration.class);

    /**
     * User
     */
    User subscriberUser = listRegistration.getSubscriber();
    if (subscriberUser != null) {
      User publicCloneWithoutRealm;
      UserProvider userProvider = this.apiApp.getUserProvider();
      if (!forTemplate) {
        publicCloneWithoutRealm = userProvider.toPublicCloneWithoutRealm(subscriberUser);
      } else {
        publicCloneWithoutRealm = userProvider.toTemplateCloneWithoutRealm(subscriberUser);
      }
      listRegistrationClone.setSubscriber(publicCloneWithoutRealm);
    }

    /**
     * List
     */
    ListProvider listProvider = this.apiApp.getListProvider();
    ListItem listItem = listRegistration.getList();
    if (!forTemplate) {
      listItem = listProvider.toPublicClone(listItem);
    } else {
      listItem = listProvider.toTemplateClone(listItem);
    }
    listRegistrationClone.setList(listItem);


    return listRegistrationClone;
  }


  /**
   * @param listRegistration the registration to upsert
   * @return the realm with the id
   */
  public Future<ListRegistration> upsertRegistration(ListRegistration listRegistration) {


    User subscriberUser = listRegistration.getSubscriber();
    if (subscriberUser == null) {
      return Future.failedFuture(new InternalError("The subscriber user is mandatory when inserting a publication subscription"));
    }
    Long subscriberId = subscriberUser.getLocalId();
    if (subscriberId == null) {
      throw new InternalException("The subscriber id of a user object should not be null");
    }
    ListItem listItem = listRegistration.getList();
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
    return updateRegistrationAndGetRowSet(listRegistration)
      .compose(rowSet -> {
        if (rowSet.rowCount() == 0) {
          return insertRegistration(listRegistration);
        }
        this.computeGuid(listRegistration);
        return Future.succeededFuture(listRegistration);
      });

  }

  private Future<RowSet<Row>> updateRegistrationAndGetRowSet(ListRegistration listRegistration) {

    String sql = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n" +
      " SET\n" +
      "  " + STATUS_COLUMN + " = $1,\n" +
      "  " + DATA_COLUMN + " = $2,\n" +
      "  " + MODIFICATION_COLUMN + " = $3\n" +
      "where\n" +
      "  " + REALM_COLUMN + " = $4\n" +
      "AND  " + ID_COLUMN + " = $5\n" +
      "AND  " + USER_COLUMN + " = $6\n";

    return jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        REGISTERED_STATUS,
        this.getDatabaseObject(listRegistration),
        DateTimeUtil.getNowUtc(),
        listRegistration.getList().getRealm().getLocalId(),
        listRegistration.getList().getLocalId(),
        listRegistration.getSubscriber().getLocalId()
      ))
      .onFailure(e -> LOGGER.error("Registration Update Sql Error " + e.getMessage() + ". With Sql:\n" + sql, e));
  }

  private Future<ListRegistration> insertRegistration(ListRegistration listRegistration) {

    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + USER_COLUMN + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + STATUS_COLUMN + ",\n" +
      "  " + CREATION_TIME_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6)";


    return jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        listRegistration.getList().getRealm().getLocalId(),
        listRegistration.getList().getLocalId(),
        listRegistration.getSubscriber().getLocalId(),
        this.getDatabaseObject(listRegistration),
        REGISTERED_STATUS,
        DateTimeUtil.getNowUtc()
      ))
      .onFailure(e -> LOGGER.error("Registration Insert Sql Error " + e.getMessage() + ". With Sql:\n" + sql, e))
      .compose(rows -> {
        this.computeGuid(listRegistration);
        return Future.succeededFuture(listRegistration);
      });

  }

  private JsonObject getDatabaseObject(ListRegistration listRegistration) {
    JsonObject data = JsonObject.mapFrom(listRegistration);
    data.remove("list");
    data.remove("subscriber");
    data.remove(Guid.GUID);
    return data;
  }


  private Future<ListRegistration> getRegistrationFromRow(Row row) {

    Long realmId = row.getLong(REALM_COLUMN);
    Future<Realm> realmFuture = this.apiApp.getRealmProvider()
      .getRealmFromId(realmId);

    return realmFuture
      .compose(realm -> {

        Long listId = row.getLong(LIST_ID_COLUMN);
        Future<ListItem> publicationFuture = apiApp.getListProvider().getListById(listId, realm);

        Long subscriberId = row.getLong(USER_COLUMN);
        Future<User> publisherFuture = apiApp.getUserProvider()
          .getUserById(subscriberId, realm.getLocalId(), User.class, realm);

        return Future
          .all(publicationFuture, publisherFuture)
          .onFailure(e -> {
            throw new InternalException(e);
          })
          .compose(mapper -> {

            JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);
            ListRegistration listRegistration = Json.decodeValue(jsonAppData.toBuffer(), ListRegistration.class);

            ListItem listItemResult = mapper.resultAt(0);
            User subscriberResult = mapper.resultAt(1);

            listRegistration.setList(listItemResult);
            listRegistration.setSubscriber(subscriberResult);

//        LocalDateTime creationTime = row.getOffsetDateTime(SUBSCRIPTION_PREFIX + COLUMN_PART_SEP + CREATION_TIME);
//        subscription.setCreationTime(creationTime);

            return Future.succeededFuture(listRegistration);
          });
      });


  }

  public Future<ListRegistration> getRegistrationByGuid(String registrationGuid) {

    Guid guidObject;
    try {
      guidObject = this.getGuidObject(registrationGuid);
    } catch (CastException e) {
      throw ValidationException.create("The registration guid (" + registrationGuid + ") is not valid", "registrationGuid", registrationGuid);
    }

    String sql = "SELECT * " +
      "FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " +
      REALM_COLUMN + " = $1\n" +
      "AND " + LIST_ID_COLUMN + " = $2\n " +
      "and " + USER_COLUMN + " = $3";
    long realmId = guidObject.getRealmOrOrganizationId();
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(
        realmId,
        guidObject.validateRealmAndGetFirstObjectId(realmId),
        guidObject.validateAndGetSecondObjectId(realmId))
      )
      .onFailure(e -> LOGGER.error("Unable to retrieve the registration. Error: " + e.getMessage() + ". Sql: \n" + sql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() > 1) {
          InternalException internalException = new InternalException("Registration Get: More than one rows (" + userRows.size() + ") returned from the registration guid " + guidObject);
          return Future.failedFuture(internalException);
        }

        Row row = userRows.iterator().next();
        return getRegistrationFromRow(row);
      });
  }

  private Guid getGuidObject(String registrationGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(GUID_PREFIX, registrationGuid);
  }


  public Future<java.util.List<RegistrationShort>> getRegistrations(String listGuid) {
    Guid guid;
    try {
      guid = apiApp.getListProvider().getGuidObject(listGuid);
    } catch (CastException e) {
      return Future.failedFuture(e);
    }

    long realmId = guid.getRealmOrOrganizationId();
    return jdbcPool.preparedQuery(
        "SELECT registration_list_id as list_id, registration_user_id as user_id, user_email as subscriber_email " +
          " FROM cs_realms.realm_list_registration  registration " +
          " JOIN cs_realms.realm_user \"user\" " +
          " on registration.registration_user_id = \"user\".user_id" +
          " WHERE " +
          " registration.registration_realm_id = $1" +
          " AND registration.registration_list_id = $2"
      )
      .execute(Tuple.of(
        realmId,
        guid.validateRealmAndGetFirstObjectId(realmId)
      ))
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(registrationRows -> {

        java.util.List<RegistrationShort> futureSubscriptions = new ArrayList<>();
        if (registrationRows.size() == 0) {
          return Future.succeededFuture(futureSubscriptions);
        }

        for (Row row : registrationRows) {


          RegistrationShort registrationShort = new RegistrationShort();
          Long listId = row.getLong("list_id");
          Long userId = row.getLong("user_id");
          String guidString = apiApp.createGuidStringFromRealmAndTwoObjectId(GUID_PREFIX, realmId, listId, userId).toString();
          registrationShort.setGuid(guidString);
          String subscriberEmail = row.getString("subscriber_email");
          registrationShort.setSubscriberEmail(subscriberEmail);

          futureSubscriptions.add(registrationShort);

        }
        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future.succeededFuture(futureSubscriptions);

      });
  }

  public Future<ListRegistration> getRegistrationByListGuidAndSubscriberEmail(String listGuid, String subscriberEmail) {
    Guid listGuidObject;
    try {
      listGuidObject = apiApp.getListProvider().getGuidObject(listGuid);
    } catch (CastException e) {
      throw ValidationException.create("The listGuid is not valid", "listGuid", listGuid);
    }

    String sql = "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME
      + " JOIN cs_realms." + UserProvider.TABLE_NAME + " userTable"
      + " ON userTable.user_id = cs_realms.realm_list_registration.registration_user_id"
      + " WHERE "
      + " registration_realm_id = $1 "
      + "AND registration_list_id = $2 "
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
      .onFailure(e -> LOGGER.error("Get registration by list guid and subscriber email error: " + e.getMessage(), e))
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

  private void computeGuid(ListRegistration listRegistration) {
    if (listRegistration.getGuid() != null) {
      return;
    }
    String guid = apiApp.createGuidStringFromRealmAndTwoObjectId(
        GUID_PREFIX,
        listRegistration.getList().getRealm(),
        listRegistration.getList().getLocalId(),
        listRegistration.getSubscriber().getLocalId()
      )
      .toString();
    listRegistration.setGuid(guid);
  }


  public ListRegistration toTemplateClone(ListRegistration listRegistration) {
    return toClone(listRegistration, true);
  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }
}
