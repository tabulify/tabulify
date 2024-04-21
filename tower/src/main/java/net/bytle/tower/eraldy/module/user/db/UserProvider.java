package net.bytle.tower.eraldy.module.user.db;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import io.vertx.sqlclient.*;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.user.inputs.UserInputProps;
import net.bytle.tower.eraldy.module.user.jackson.JacksonUserStatusDeserializer;
import net.bytle.tower.eraldy.module.user.model.UserStatus;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.PasswordHashManager;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.type.time.TimeZoneUtil;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.analytics.event.SignUpEvent;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.db.*;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Manage the get/upsert of a {@link User} object asynchronously
 */
public class UserProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(UserProvider.class);

  public static final String REALM_USER_TABLE_NAME = "realm_user";
  private final String FULL_QUALIFIED_USER_TABLE_NAME;


  private static final String TABLE_PREFIX = "user";
  public static final String PASSWORD_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "password";


  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;

  private static final String REALM_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "realm_id";
  public static final String ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "id";

  public static final String USR_GUID_PREFIX = "usr";
  private static final String MODIFICATION_TIME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private final EraldyApiApp apiApp;
  private final Pool jdbcPool;

  /**
   * Mapper for the API
   */
  private final JsonMapper apiMapper;
  private final JdbcTable userTable;


  public UserProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {

    this.apiApp = apiApp;
    Server server = this.apiApp.getHttpServer().getServer();
    this.jdbcPool = server.getPostgresClient().getPool();

    JacksonMapperManager jacksonMapperManager = server.getJacksonMapperManager();

    jacksonMapperManager.addDeserializer(UserStatus.class, new JacksonUserStatusDeserializer());

    this.apiMapper = jacksonMapperManager.jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .build();


    this.userTable = JdbcTable.build(jdbcSchema, REALM_USER_TABLE_NAME, UserCols.values())
      .addPrimaryKeyColumn(UserCols.ID)
      .addPrimaryKeyColumn(UserCols.REALM_ID)
      .build();

    this.FULL_QUALIFIED_USER_TABLE_NAME = this.userTable.getFullName();


  }


  /**
   * @param user        - the user
   * @param flowType - the web flow that insert this event
   * @return a user suitable
   */
  public <T extends User> Future<T> insertUserAndTrackEvent(Realm realm, UserInputProps user, FlowType flowType) {

    return this.jdbcPool.withConnection(sqlConnection -> insertUserAndTrackEvent(realm, user, flowType, sqlConnection));

  }

  /**
   * Package Private,
   * for creation with login/signup, the insertion should be driven by {@link AuthProvider#insertUserFromLoginAuthUserClaims(AuthUser, RoutingContext, net.bytle.vertx.flow.WebFlow)}
   * for creation via import, the insertion should be driven by {@link #insertUserAndTrackEvent(Realm, UserInputProps, FlowType, SqlConnection)}
   *
   * @param user      - the user to insert (sign-up or import)
   * @return a user
   */
  public Future<User> insertUser(Realm realm, UserInputProps user) {

    return this.jdbcPool.withConnection(sqlConnection -> insertUser(realm, user, sqlConnection));
  }

  @SuppressWarnings("unused")
  private Future<Boolean> exists(User user) {
    String sql;
    Future<RowSet<Row>> futureResponse;
    JdbcSelect select = JdbcSelect.from(this.userTable)
      .addEqualityPredicate(UserCols.REALM_ID, user.getRealm().getLocalId());
    if (user.getLocalId() != null) {

      select
        .addEqualityPredicate(UserCols.ID, user.getLocalId());

    } else {
      EmailAddress email = user.getEmailAddress();
      if (email == null) {
        String failureMessage = "An id or email should be given to check the existence of a user";
        InternalException internalException = new InternalException(failureMessage);
        return Future.failedFuture(internalException);
      }
      select
        .addEqualityPredicate(UserCols.EMAIL_ADDRESS, email.toNormalizedString());

    }
    return select
      .execute(rows -> {
        if (rows.size() == 1) {
          return Future.succeededFuture(true);
        } else {
          return Future.succeededFuture(false);
        }
      });
  }

  /**
   *
   * @param user      - the actual user
   * @param userInputProps - the new properties (ony non null are taken into account)
   * @return the user
   */
  public <T extends User> Future<T> updateUser(T user, UserInputProps userInputProps) {

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.userTable)
      .addUpdatedColumn(UserCols.MODIFICATION_IME, DateTimeService.getNowInUtc())
      .addPredicateColumn(UserCols.REALM_ID, user.getRealm().getLocalId())
      // if update by handle, we need to get the id back
      // and we check that there was an update
      .addReturningColumn(UserCols.ID);

    /**
     * We allow update via handle for test purpose
     */
    if (user.getLocalId() != null) {

      jdbcUpdate.addPredicateColumn(UserCols.ID, user.getLocalId());

      EmailAddress newEmailAddress = userInputProps.getEmailAddress();
      if (newEmailAddress != null && !Objects.equals(newEmailAddress.toNormalizedString(), user.getEmailAddress().toNormalizedString())) {
        user.setEmailAddress(newEmailAddress);
        jdbcUpdate.addUpdatedColumn(UserCols.EMAIL_ADDRESS, user.getEmailAddress().toNormalizedString());
      }

    } else if (user.getEmailAddress() != null) {

      jdbcUpdate.addPredicateColumn(UserCols.EMAIL_ADDRESS, user.getEmailAddress());

    } else {

      return Future.failedFuture(new InternalException("To update a user, the id or email should be not null"));

    }

    String newFamilyName = userInputProps.getFamilyName();
    if (newFamilyName != null && !Objects.equals(newFamilyName, user.getFamilyName())) {
      user.setFamilyName(newFamilyName);
      jdbcUpdate.addUpdatedColumn(UserCols.FAMILY_NAME, user.getFamilyName());
    }

    String newGivenName = userInputProps.getGivenName();
    if (newGivenName != null && !Objects.equals(newGivenName, user.getGivenName())) {
      user.setGivenName(newGivenName);
      jdbcUpdate.addUpdatedColumn(UserCols.GIVEN_NAME, user.getGivenName());
    }

    UserStatus newStatus = userInputProps.getStatus();
    if (newStatus != null && !Objects.equals(newStatus, user.getStatus())) {
      user.setStatus(newStatus);
      jdbcUpdate.addUpdatedColumn(UserCols.STATUS_CODE, user.getStatus().getCode());
    }

    String statusMessage = userInputProps.getStatusMessage();
    if (statusMessage != null && !Objects.equals(statusMessage, user.getStatusMessage())) {
      user.setStatusMessage(statusMessage);
      jdbcUpdate.addUpdatedColumn(UserCols.STATUS_MESSAGE, user.getStatusMessage());
    }

    String newBio = userInputProps.getBio();
    if (newBio != null && !Objects.equals(newBio, user.getBio())) {
      user.setBio(newBio);
      jdbcUpdate.addUpdatedColumn(UserCols.BIO, user.getBio());
    }

    String newTitle = userInputProps.getTitle();
    if (newTitle != null && !Objects.equals(newTitle, user.getTitle())) {
      user.setTitle(newTitle);
      jdbcUpdate.addUpdatedColumn(UserCols.TITLE, user.getTitle());
    }

    TimeZone newTimeZone = userInputProps.getTimeZone();
    if (newTimeZone != null && !Objects.equals(newTimeZone, user.getTimeZone())) {
      user.setTimeZone(newTimeZone);
      jdbcUpdate.addUpdatedColumn(UserCols.TIME_ZONE, user.getTimeZone().getID());
    }

    String newLocation = userInputProps.getLocation();
    if (newLocation != null && !Objects.equals(newLocation, user.getLocation())) {
      user.setLocation(newLocation);
      jdbcUpdate.addUpdatedColumn(UserCols.LOCATION, user.getLocation());
    }

    URI newAvatar = userInputProps.getAvatar();
    if (newAvatar != null && !Objects.equals(newAvatar, user.getAvatar())) {
      user.setAvatar(newAvatar);
      jdbcUpdate.addUpdatedColumn(UserCols.AVATAR, user.getAvatar());
    }

    URI newWebSite = userInputProps.getWebsite();
    if (newWebSite != null && !Objects.equals(newWebSite, user.getWebsite())) {
      user.setWebsite(newWebSite);
      jdbcUpdate.addUpdatedColumn(UserCols.WEBSITE, user.getWebsite());
    }

    LocalDateTime newLastActiveTime = userInputProps.getLastActiveTime();
    if (newLastActiveTime != null && !Objects.equals(newLastActiveTime, user.getLastActiveTime())) {
      user.setLastActiveTime(newLastActiveTime);
      jdbcUpdate.addUpdatedColumn(UserCols.LAST_ACTIVE_TIME, user.getLastActiveTime());
    }

    return jdbcUpdate
      .execute()
      .compose(rowSet -> {

        if (rowSet.size() != 1) {
          NoSuchElementException noSuchElementException = new NoSuchElementException("The user was not updated because it was not found (User:" + user + ")");
          return Future.failedFuture(noSuchElementException);
        }

        Long userId = rowSet.iterator().next().getLong(UserCols.ID);
        if (user.getLocalId() == null) {
          user.setLocalId(userId);
        } else {
          if (!user.getLocalId().equals(userId)) {
            return Future.failedFuture("The update id (" + userId + ") is not the same as the user id (" + user.getLocalId() + ") for the user (" + user + ")");
          }
        }
        return Future.succeededFuture(user);

      });

  }


  /**
   * @param realm      - the realmId
   * @param pageId     - the page identifier
   * @param pageSize   - the page size
   * @param searchTerm - the search term (for now works only on email search)
   * @return the realm
   */
  public Future<List<User>> getUsers(Realm realm, Long pageId, Long pageSize, String searchTerm) {

    JdbcPagination jdbcPagination = new JdbcPagination();
    jdbcPagination.setPageId(pageId);
    jdbcPagination.setPageSize(pageSize);
    jdbcPagination.setSearchTerm(searchTerm);
    return JdbcPaginatedSelect.from(this.userTable)
      .addEqualityPredicate(UserCols.REALM_ID, realm.getLocalId())
      .setSearchColumn(UserCols.EMAIL_ADDRESS)
      .setPagination(jdbcPagination)
      .addOrderBy(UserCols.CREATION_TIME)
      .execute(userRows -> {

          List<User> users = new ArrayList<>();
          for (JdbcRow row : userRows) {
            User user = getUserFromRow(row, realm);
            users.add(user);
          }

          return Future.succeededFuture(users);
        }
      );
  }

  /**
   * @param row        - the resulting row
   * @param realm - the realm
   */
  <T extends User> T getUserFromRow(JdbcRow row, Realm realm) {


    T user = this.createUserObjectFromRealm(realm);

    Long userRealmId = row.getLong(UserCols.REALM_ID);
    if (!realm.getLocalId().equals(userRealmId)) {
      throw new InternalException("User Realm Id and passed realm are not the same");
    }
    user.setRealm(realm);
    user.setLocalId(row.getLong(UserCols.ID));
    this.updateGuid(user);


    user.setEmailAddress(EmailAddress.ofFailSafe(row.getString(UserCols.EMAIL_ADDRESS)));
    UserStatus userStatus = UserStatus.fromStatusCodeFailSafe(row.getInteger(UserCols.STATUS_CODE));
    user.setStatus(userStatus);

    /**
     * Time
     */
    user.setCreationTime(row.getLocalDateTime(UserCols.CREATION_TIME));
    user.setModificationTime(row.getLocalDateTime(UserCols.MODIFICATION_IME));
    user.setLastActiveTime(row.getLocalDateTime(UserCols.LAST_ACTIVE_TIME));

    /**
     * Name
     */
    user.setGivenName(row.getString(UserCols.GIVEN_NAME));
    user.setFamilyName(row.getString(UserCols.FAMILY_NAME));
    user.setTitle(row.getString(UserCols.TITLE));

    /**
     * Location
     */
    user.setLocation(row.getString(UserCols.LOCATION));
    String timeZoneId = row.getString(UserCols.TIME_ZONE);
    if (timeZoneId != null) {
      user.setTimeZone(TimeZoneUtil.getTimeZoneFailSafe(timeZoneId));
    }

    /**
     * Description
     */
    user.setBio(row.getString(UserCols.BIO));
    String websiteStringUrl = row.getString(UserCols.WEBSITE);
    if (websiteStringUrl != null) {
      user.setWebsite(URI.create(websiteStringUrl));
    }
    String avatar = row.getString(UserCols.AVATAR);
    if (avatar != null) {
      user.setAvatar(URI.create(avatar));
    }

    return user;


  }


  public <T extends User> void updateGuid(T user) {
    if (user.getGuid() != null) {
      return;
    }
    String guid = this.getGuidFromUser(user).toString();
    user.setGuid(guid);
  }

  private Guid getGuidFromUser(User user) {
    return apiApp.createGuidFromRealmAndObjectId(USR_GUID_PREFIX, user.getRealm(), user.getLocalId());
  }

  /**
   * @param userId    - the user id
   * @param realm     - the realm
   * @return the user or null if not found
   */
  public <T extends User> Future<T> getUserByLocalId(Long userId, Realm realm) {

    return this.jdbcPool.withConnection(sqlConnection -> getUserByLocalId(userId, realm, sqlConnection));

  }


  public Future<? extends User> getUserByEmail(EmailAddress userEmail, String realmIdentifier) {
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .recover(err -> Future.failedFuture(new InternalException("getUserByEmail: Error while trying to retrieve the realm", err)))
      .compose(realm -> getUserByEmail(userEmail, realm));
  }

  /**
   * @param userEmail    - the email
   * @param realm        - the realm
   * @return the user or null if not found
   */
  public <T extends User> Future<T> getUserByEmail(EmailAddress userEmail, Realm realm) {

    return this.jdbcPool.withConnection(sqlConnection -> getUserByEmail(userEmail, realm, sqlConnection));

  }

  private <T extends User> Future<T> getUserByEmail(EmailAddress userEmail, Realm realm, SqlConnection sqlConnection) {
    assert userEmail != null;
    assert realm != null;


    return JdbcSelect.from(this.userTable)
      .addEqualityPredicate(UserCols.EMAIL_ADDRESS, userEmail.toNormalizedString())
      .addEqualityPredicate(UserCols.REALM_ID, realm.getLocalId())
      .execute(sqlConnection, userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        JdbcRow row = userRows.iterator().next();
        T userFromRow = getUserFromRow(row, realm);

        return Future.succeededFuture(userFromRow);
      });
  }

  public Future<User> getUserFromGuidOrEmail(String userGuid, String userEmail, Realm realm) {

    if (userGuid == null && userEmail == null) {
      throw ValidationException.create("The user email and id cannot be both null", "userEmail", null);
    }

    String identifier = userGuid;
    if (identifier == null) {
      identifier = userEmail;
    }
    return getUserByIdentifier(identifier, realm);
  }

  public <T extends User> Future<T> getUserByGuid(String guid, Realm realm) {

    Guid guidObject;
    try {
      guidObject = this.getGuidFromHash(guid);
    } catch (CastException e) {
      throw ValidationException.create("The user guid is not valid", "userGuid", guid);
    }
    Future<Realm> futureRealm;
    if (realm != null) {
      if (realm.getLocalId() != guidObject.getRealmOrOrganizationId()) {
        return Future.failedFuture(new InternalException("The user guid (" + guid + ") has a realm (" + guidObject.getRealmOrOrganizationId() + " that is not the same than the passed realm (" + realm.getLocalId() + ")"));
      }
      futureRealm = Future.succeededFuture(realm);
    } else {
      futureRealm = this.apiApp.getRealmProvider()
        .getRealmFromLocalId(guidObject.getRealmOrOrganizationId());
    }
    return futureRealm
      .compose(realmResult -> {
        if (realmResult == null) {
          return Future.failedFuture(new InternalException("The realm was not found"));
        }
        return this.getUserByLocalId(guidObject.validateRealmAndGetFirstObjectId(realmResult.getLocalId()), realmResult);
      });


  }


  public Future<Void> updatePassword(Long userLocalId, Long realmLocalId, String password) {

    String sql;

    if (userLocalId == null) {
      throw new InternalException("The user id should not be null.");
    }

    if (realmLocalId == null) {
      throw new InternalException("The realm id should not be null.");
    }

    String passwordHashed = PasswordHashManager.get()
      .hash(password);

    sql = "UPDATE \n" +
      FULL_QUALIFIED_USER_TABLE_NAME + " \n" +
      "set \n" +
      "  " + PASSWORD_COLUMN + " = $1,\n" +
      "  " + MODIFICATION_TIME_COLUMN + " = $2\n" +
      "where\n" +
      "  " + ID_COLUMN + "= $3\n" +
      "AND " + REALM_COLUMN + " = $4 ";

    return this.jdbcPool
      .preparedQuery(sql)
      .execute(Tuple.of(
        passwordHashed,
        DateTimeService.getNowInUtc(),
        userLocalId,
        realmLocalId
      ))
      .onFailure(error -> LOGGER.error("User Password Update: Error:" + error.getMessage() + ". Sql: " + sql, error))
      .compose(ok -> Future.succeededFuture());

  }

  /**
   * @param userEmail    - the user email
   * @param userPassword - the password in clear
   * @param realm        - the realm
   * @return a user if the user handle, realm and password combination are good
   */
  public Future<? extends User> getUserByPassword(EmailAddress userEmail, String userPassword, Realm realm) {

    String hashedPassword = PasswordHashManager.get().hash(userPassword);

    return JdbcSelect.from(this.userTable)
      .addEqualityPredicate(UserCols.EMAIL_ADDRESS, userEmail.toNormalizedString())
      .addEqualityPredicate(UserCols.REALM_ID, realm.getLocalId())
      .addEqualityPredicate(UserCols.PASSWORD, hashedPassword)
      .execute(userRows -> {
          if (userRows.size() == 0) {
            return Future.succeededFuture();
          }
          JdbcRow row = userRows.iterator().next();
          User userFromRow = getUserFromRow(row, realm);
          return Future.succeededFuture(userFromRow);
        }
      );

  }


  @SuppressWarnings("unused")
  private List<User> getUsersFromRows(JdbcRowSet userRows, Realm knowRealm) {
    List<User> users = new ArrayList<>();
    for (JdbcRow row : userRows) {
      users.add(getUserFromRow(row, knowRealm));
    }
    return users;
  }

  public Guid getGuidFromHash(String userGuid) throws CastException {
    return apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(USR_GUID_PREFIX, userGuid);
  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  public boolean isGuid(String identifier) {
    return identifier.startsWith(USR_GUID_PREFIX + Guid.GUID_SEPARATOR);
  }

  public <T extends User> Future<T> getUserByIdentifier(String identifier, Realm realm) {

    if (identifier.isEmpty()) {
      return Future.failedFuture(TowerFailureException.builder()
        .setMessage("A user identifier can not be the empty string")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .build()
      );
    }

    if (this.isGuid(identifier)) {
      return getUserByGuid(identifier, realm);
    } else {
      if (realm == null) {
        return Future.failedFuture(new InternalException("With a user email (" + identifier + ") as user identifier, the realm should be provided"));
      }
      EmailAddress email;
      try {
        email = EmailAddress.of(identifier);
      } catch (EmailCastException e) {
        return Future.failedFuture(TowerFailureException.builder()
          .setMessage("The user identifier (" + identifier + ") is not a guid nor an email")
          .setType(TowerFailureTypeEnum.BAD_STRUCTURE_422)
          .build()
        );
      }
      return getUserByEmail(email, realm);
    }
  }


  public Guid createUserGuid(long realmId, Long userId) {
    return this.apiApp.createGuidFromRealmAndObjectId(USR_GUID_PREFIX, realmId, userId);
  }


  /**
   * Get or insert the user. It's used at first to load the initial data of the Eraldy model
   * The onServerStartUp name is there because we track the flow that insert the user
   * @param userInputProps - the user to get sert
   * @param sqlConnection - the sql connection
   * @return the user
   * @param <T> extended user
   */
  public <T extends User> Future<T> getsertOnServerStartup(Realm realm, Long localId, UserInputProps userInputProps, SqlConnection sqlConnection) {

    Future<T> futureGetUser;
    if (localId != null) {
      futureGetUser = this.getUserByLocalId(
        localId,
        realm,
        sqlConnection
      );
    } else {

      EmailAddress emailAddress = userInputProps.getEmailAddress();
      futureGetUser = this.getUserByEmail(
        emailAddress,
        realm,
        sqlConnection
      );

    }
    return futureGetUser
      .compose(getUser -> {

        if (getUser == null) {
          return this.insertUserAndTrackEvent(realm, userInputProps, FlowType.SERVER_STARTUP, sqlConnection);
        }
        return this.updateUser(getUser, userInputProps);
      });
  }


  /**
   * A get that can be used in a SQlConnection with or without transaction
   * It was created initially to not block during the transaction on server startup to insert
   * Eraldy data.
   * @param userId - the user id
   * @param realm - the realm if already build
   * @param sqlConnection - the sql connection
   * @return the user or null
   * @param <T> a user extension
   */
  public <T extends User> Future<T> getUserByLocalId(Long userId, Realm realm, SqlConnection sqlConnection) {

    assert userId != null;
    assert realm != null;

    return JdbcSelect.from(this.userTable)
      .addEqualityPredicate(UserCols.ID, userId)
      .addEqualityPredicate(UserCols.REALM_ID, realm.getLocalId())
      .execute(sqlConnection, userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        JdbcRow row = userRows.iterator().next();
        T userFromRow = getUserFromRow(row, realm);
        return Future.succeededFuture(userFromRow);

      });
  }

  private <T extends User> Future<T> insertUserAndTrackEvent(Realm realm, UserInputProps userInputProps, FlowType flowType, SqlConnection sqlConnection) {

    //noinspection unchecked
    return (Future<T>) this.insertUser(realm, userInputProps, sqlConnection)
      .compose(insertedUser -> {
        SignUpEvent signUpEvent = new SignUpEvent();
        signUpEvent.getRequest().setFlowGuid(flowType.getId().toString());
        signUpEvent.getRequest().setFlowHandle(flowType.getHandle());
        this.apiApp
          .getHttpServer()
          .getServer()
          .getTrackerAnalytics()
          .eventBuilder(signUpEvent)
          .setAnalyticsUser(this.apiApp.getAuthProvider().toAnalyticsUser(insertedUser))
          .processEvent();
        return Future.succeededFuture(insertedUser);
      });
  }

  private <T extends User> Future<T> insertUser(Realm realm, UserInputProps userInputProps, SqlConnection sqlConnection) {


    T user = this.createUserObjectFromRealm(realm);

    return this.apiApp.getRealmSequenceProvider()
      .getNextIdForTableAndRealm(sqlConnection, realm, this.userTable)
      .recover(err -> Future.failedFuture(
        TowerFailureException.builder()
          .setCauseException(err)
          .setMessage("UserProvider: Error on next sequence id" + err.getMessage())
          .build()
      ))
      .compose(seqUserId -> {

        JdbcInsert jdbcInsert = JdbcInsert.into(this.userTable)
          .addColumn(UserCols.CREATION_TIME, DateTimeService.getNowInUtc())
          .addColumn(UserCols.LAST_ACTIVE_TIME, DateTimeService.getNowInUtc())
          .addColumn(UserCols.REALM_ID, realm.getLocalId());

        user.setLocalId(seqUserId);
        jdbcInsert.addColumn(UserCols.ID, user.getLocalId());
        this.updateGuid(user);

        user.setEmailAddress(userInputProps.getEmailAddress());
        jdbcInsert.addColumn(UserCols.EMAIL_ADDRESS, user.getEmailAddress().toNormalizedString());

        String givenName = userInputProps.getGivenName();
        if (givenName == null) {
          // Given name is mandatory (used everywhere)
          givenName = user.getEmailAddress().getLocalBox();
        }
        user.setGivenName(givenName);
        jdbcInsert.addColumn(UserCols.GIVEN_NAME, user.getGivenName());

        user.setFamilyName(userInputProps.getFamilyName());
        jdbcInsert.addColumn(UserCols.FAMILY_NAME, user.getFamilyName());

        user.setStatus(UserStatus.OK);
        jdbcInsert.addColumn(UserCols.STATUS_CODE, user.getStatus().getCode());

        user.setTitle(userInputProps.getTitle());
        jdbcInsert.addColumn(UserCols.TITLE, user.getTitle());

        user.setBio(userInputProps.getBio());
        jdbcInsert.addColumn(UserCols.BIO, user.getBio());

        user.setLocation(userInputProps.getLocation());
        jdbcInsert.addColumn(UserCols.LOCATION, user.getLocation());

        TimeZone timeZone = userInputProps.getTimeZone();
        if (timeZone != null) {
          user.setTimeZone(timeZone);
          jdbcInsert.addColumn(UserCols.TIME_ZONE, user.getTimeZone().getID());
        }

        URI avatar = userInputProps.getAvatar();
        if (avatar != null) {
          user.setAvatar(avatar);
          jdbcInsert.addColumn(UserCols.AVATAR, user.getAvatar().toString());
        }

        URI website = userInputProps.getWebsite();
        if (website != null) {
          user.setWebsite(website);
          jdbcInsert.addColumn(UserCols.WEBSITE, user.getWebsite().toString());
        }

        return jdbcInsert
          .execute(sqlConnection, jdbcRowSet -> Future.succeededFuture(user));
      });

  }

  public <T extends User> T createUserObjectFromRealm(Realm realm) {

    try {
      //noinspection unchecked
      T user = (T) this.getUserClass(realm).getDeclaredConstructor().newInstance();
      user.setRealm(realm);
      return user;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException("Unable to create a user pojo. Error: " + e.getMessage(), e);
    }

  }

  /**
   * We create an organization user if this is an eraldy
   * user and cast it back to a normal user
   * if it was not found in an organization
   */
  private Class<? extends User> getUserClass(Realm realm) {

    if (this.apiApp.getEraldyModel().isEraldyRealm(realm)) {
      return OrgaUser.class;
    }
    return User.class;

  }

}
