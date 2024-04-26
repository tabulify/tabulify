package net.bytle.tower.eraldy.module.user.db;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.user.inputs.UserInputProps;
import net.bytle.tower.eraldy.module.user.jackson.JacksonUserGuidDeserializer;
import net.bytle.tower.eraldy.module.user.jackson.JacksonUserGuidSerializer;
import net.bytle.tower.eraldy.module.user.jackson.JacksonUserStatusDeserializer;
import net.bytle.tower.eraldy.module.user.model.UserGuid;
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

    jacksonMapperManager
      .addDeserializer(UserStatus.class, new JacksonUserStatusDeserializer())
      .addDeserializer(UserGuid.class, new JacksonUserGuidDeserializer(apiApp))
      .addSerializer(UserGuid.class, new JacksonUserGuidSerializer(apiApp))
    ;

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


  /**
   *
   * @param user      - the actual user
   * @param userInputProps - the new properties (ony non null are taken into account)
   * @return the user
   */
  public <T extends User> Future<T> updateUser(T user, UserInputProps userInputProps) {

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.userTable)
      .addUpdatedColumn(UserCols.MODIFICATION_IME, DateTimeService.getNowInUtc())
      .addPredicateColumn(UserCols.REALM_ID, user.getGuid().getRealmId())
      // if update by handle, we need to get the id back
      // and we check that there was an update
      .addReturningColumn(UserCols.ID);

    /**
     * We allow update via handle for test purpose
     */
    if (user.getGuid() != null) {

      jdbcUpdate.addPredicateColumn(UserCols.ID, user.getGuid().getLocalId());

      EmailAddress newEmailAddress = userInputProps.getEmailAddress();
      if (newEmailAddress != null && !Objects.equals(newEmailAddress.toNormalizedString(), user.getEmailAddress().toNormalizedString())) {
        user.setEmailAddress(newEmailAddress);
        jdbcUpdate.addUpdatedColumn(UserCols.EMAIL_ADDRESS, user.getEmailAddress().toNormalizedString());
      }

    } else if (user.getEmailAddress() != null) {

      jdbcUpdate.addPredicateColumn(UserCols.EMAIL_ADDRESS, user.getEmailAddress());

    } else {

      return Future.failedFuture(new InternalException("To update a user, the guid or email should be not null"));

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
        if (user.getGuid() == null) {
          this.updateGuid(user, userId);

        } else {
          if (user.getGuid().getLocalId() != userId) {
            return Future.failedFuture(new InternalException("The update id (" + userId + ") is not the same as the user id (" + user.getGuid().getLocalId() + ") for the user (" + user + ")"));
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
      .addEqualityPredicate(UserCols.REALM_ID, realm.getGuid().getLocalId())
      .setSearchColumn(UserCols.EMAIL_ADDRESS)
      .setPagination(jdbcPagination)
      .addOrderBy(UserCols.CREATION_TIME)
      .execute(userRows -> {

          List<User> users = new ArrayList<>();
          for (JdbcRow row : userRows) {
            User user = new User();
            buildUserFromRow(user, row, realm);
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
  public <T extends User> T buildUserFromRow(T user, JdbcRow row, Realm realm) {


    Long userRealmId = row.getLong(UserCols.REALM_ID);
    if (realm.getGuid().getLocalId() != userRealmId) {
      throw new InternalException("User Realm Id and passed realm are not the same");
    }
    user.setRealm(realm);
    this.updateGuid(user, row.getLong(UserCols.ID));


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


  public <T extends User> void updateGuid(T user, Long userLocalId) {
    if (user.getGuid() != null) {
      return;
    }
    UserGuid userGuid = new UserGuid();
    userGuid.setLocalId(userLocalId);
    userGuid.setRealmId(user.getRealm().getGuid().getLocalId());
    user.setGuid(userGuid);

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
      .addEqualityPredicate(UserCols.REALM_ID, realm.getGuid().getLocalId())
      .execute(sqlConnection, userRows -> this.buildUserFromRowSet(userRows, realm, sqlConnection));

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

  public <T extends User> Future<T> getUserByGuid(UserGuid guidObject, Realm realm) {


    Future<Realm> futureRealm;
    if (realm != null) {
      if (!Objects.equals(realm.getGuid().getLocalId(), guidObject.getRealmId())) {
        return Future.failedFuture(new InternalException("The user guid (" + guidObject + ") has a realm (" + guidObject.getRealmId() + " that is not the same than the passed realm (" + realm.getGuid().getLocalId() + ")"));
      }
      futureRealm = Future.succeededFuture(realm);
    } else {
      futureRealm = this.apiApp.getRealmProvider()
        .getRealmFromLocalId(guidObject.getRealmId());
    }
    return futureRealm
      .compose(realmResult -> {
        if (realmResult == null) {
          return Future.failedFuture(new InternalException("The realm was not found"));
        }
        return this.getUserByLocalId(guidObject.getLocalId(), realmResult);
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
  public <T extends User> Future<T> getUserByPassword(EmailAddress userEmail, String userPassword, Realm realm, SqlConnection sqlConnection) {

    String hashedPassword = PasswordHashManager.get().hash(userPassword);

    return JdbcSelect.from(this.userTable)
      .addEqualityPredicate(UserCols.EMAIL_ADDRESS, userEmail.toNormalizedString())
      .addEqualityPredicate(UserCols.REALM_ID, realm.getGuid().getLocalId())
      .addEqualityPredicate(UserCols.PASSWORD, hashedPassword)
      .execute(userRows -> this.buildUserFromRowSet(userRows, realm, sqlConnection));

  }


  @SuppressWarnings("unused")
  private List<User> getUsersFromRows(JdbcRowSet userRows, Realm knowRealm) {
    List<User> users = new ArrayList<>();
    for (JdbcRow row : userRows) {
      User user = new User();
      users.add(buildUserFromRow(user, row, knowRealm));
    }
    return users;
  }

  public UserGuid getGuidFromHash(String userGuid) throws CastException {
    return apiApp.getHttpServer().getServer().getJacksonMapperManager().getDeserializer(UserGuid.class).deserialize(userGuid);
  }

  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }


  public <T extends User> Future<T> getUserByIdentifier(String identifier, Realm realm) {

    if (identifier.isEmpty()) {
      return Future.failedFuture(TowerFailureException.builder()
        .setMessage("A user identifier can not be the empty string")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .build()
      );
    }

    try {
      UserGuid userGuid = this.apiApp.getJackson().getDeserializer(UserGuid.class).deserialize(identifier);
      return getUserByGuid(userGuid, realm);
    } catch (CastException e) {
      //
    }

    if (realm == null) {
      return Future.failedFuture(new InternalException("Without a user guid (" + identifier + ") as user identifier, the realm should be provided"));
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
      .compose(user -> {

        if (user == null) {
          return this.insertUserAndTrackEvent(realm, userInputProps, FlowType.SERVER_STARTUP, sqlConnection);
        }
        return this.updateUser(user, userInputProps);
      });
  }


  /**
   * A get that can be used in a SQlConnection with or without transaction
   * It was created initially to not block during the transaction on server startup to insert
   * Eraldy data.
   * @param userId - the user id
   * @param realm - the realm
   * @param sqlConnection - the sql connection
   * @return the user or null
   * @param <T> a user extension
   */
  public <T extends User> Future<T> getUserByLocalId(Long userId, Realm realm, SqlConnection sqlConnection) {

    assert userId != null;
    assert realm != null;

    return JdbcSelect.from(this.userTable)
      .addEqualityPredicate(UserCols.ID, userId)
      .addEqualityPredicate(UserCols.REALM_ID, realm.getGuid().getLocalId())
      .execute(sqlConnection, userRow -> this.buildUserFromRowSet(userRow, realm, sqlConnection));
  }

  private <T extends User> Future<T> buildUserFromRowSet(JdbcRowSet rowSet, Realm realm, SqlConnection sqlConnection) {

    if (rowSet.size() == 0) {
      return Future.succeededFuture();
    }

    JdbcRow row = rowSet.iterator().next();
    Long userId = row.getLong(UserCols.ID);

    /**
     * We don't know if a user is an organizational user before hands.
     * Therefore, we check based on the realm if the user is an organization user.
     * <p>
     * We could send to the login app, the type of user that we want
     * but, it would make difficult to see that the user exists
     * and to handle this case to show a proposition
     * <p>
     * OrgaUser is also a nice way to pass the information that the user is
     * more than a normal user in the code.
     */
    Future<OrgaUser> futureOrgaUser = Future.succeededFuture();
    if (realm.getGuid().getLocalId() == EraldyModel.REALM_LOCAL_ID) {
      futureOrgaUser = this.apiApp.getOrganizationUserProvider().createOrganizationUserObjectFromLocalIdOrNull(userId, sqlConnection);
    }

    return futureOrgaUser
      .compose(orgaUser -> {
        /**
         * If Orga user is null, this is a normal user
         */
        //noinspection unchecked
        T user = (T) Objects.requireNonNullElseGet(orgaUser, User::new);
        T userFromRow = buildUserFromRow(user, row, realm);
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

  private Future<User> insertUser(Realm realm, UserInputProps userInputProps, SqlConnection sqlConnection) {


    return this.apiApp.getRealmSequenceProvider()
      .getNextIdForTableAndRealm(sqlConnection, realm, this.userTable)
      .recover(err -> Future.failedFuture(
        TowerFailureException.builder()
          .setCauseException(err)
          .setMessage("UserProvider: Error on next sequence id" + err.getMessage())
          .build()
      ))
      .compose(seqUserId -> {

        LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
        User user = new User();
        user.setRealm(realm);
        user.setCreationTime(nowInUtc);
        user.setLastActiveTime(nowInUtc);
        user.setModificationTime(nowInUtc);
        JdbcInsert jdbcInsert = JdbcInsert.into(this.userTable)
          .addColumn(UserCols.CREATION_TIME, user.getCreationTime())
          .addColumn(UserCols.MODIFICATION_IME, user.getModificationTime())
          .addColumn(UserCols.LAST_ACTIVE_TIME, user.getLastActiveTime());

        this.updateGuid(user, seqUserId);
        jdbcInsert.addColumn(UserCols.ID, user.getGuid().getLocalId())
          .addColumn(UserCols.REALM_ID, user.getGuid().getRealmId());


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

  public JdbcTable getTable() {
    return this.userTable;
  }

  public String serializeUserGuid(UserGuid guid) {
    return this.apiApp.getHttpServer().getServer().getJacksonMapperManager().getSerializer(UserGuid.class).serialize(guid);
  }

  public <T extends User> Future<T> getUserByPassword(EmailAddress userEmail, String userPassword, Realm realm) {
    return this.jdbcPool.withConnection(sqlConnection -> getUserByPassword(userEmail, userPassword, realm, sqlConnection));
  }
}
