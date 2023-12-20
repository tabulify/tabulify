package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import jakarta.mail.internet.AddressException;
import net.bytle.exception.AssertionException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.PasswordHashManager;
import net.bytle.tower.util.Postgres;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Manage the get/upsert of a {@link User} object asynchronously
 */
public class UserProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(UserProvider.class);

  protected static final String TABLE_NAME = "realm_user";
  protected static final String QUALIFIED_TABLE_NAME = JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME;


  private static final String TABLE_PREFIX = "user";
  public static final String EMAIL_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "email";
  public static final String PASSWORD_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "password";


  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String DISABLED_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "disabled";
  private static final String DATA = "data";

  public static final String DATA_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + DATA;
  private static final String REALM_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + RealmProvider.ID_COLUMN;
  protected static final String ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "id";
  private static final String MODIFICATION_TIME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;

  public static final String USR_GUID_PREFIX = "usr";

  private static final String CREATION_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private final EraldyApiApp apiApp;
  private final PgPool jdbcPool;
  /**
   * Mapper for the database string
   */
  private final JsonMapper databaseMapper;
  /**
   * Mapper for the API
   */
  private final JsonMapper apiMapper;


  public UserProvider(EraldyApiApp apiApp) {

    this.apiApp = apiApp;
    Server server = this.apiApp.getApexDomain().getHttpServer().getServer();
    this.jdbcPool = server.getJdbcPool();
    this.databaseMapper = server.getJacksonMapperManager().jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .build();
    this.apiMapper = this.apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager().jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .build();
  }


  public User toPublicCloneWithoutRealm(User user) {
    return toPublicClone(user);
  }

  private User toPublicClone(User user) {
    User userClone = JsonObject.mapFrom(user).mapTo(User.class);
    userClone.setRealm(null);
    userClone.setLocalId(null);
    return userClone;
  }


  /**
   * This function will update the user of the userId is known
   * Otherwise, it will try to update it by email.
   * If the update is not successful, the user is {@link #insertUser(User)  inserted}.
   *
   * @param user the user to upsert
   * @return the user
   */
  public Future<User> upsertUser(User user) {

    Realm userRealm = user.getRealm();
    Long userId = user.getLocalId();

    if (userId == null) {
      if (user.getEmail() == null) {
        return Future.failedFuture(new IllegalArgumentException("Without a user guid, the email should not be null"));
      }
      if (userRealm == null) {
        return Future.failedFuture(new IllegalArgumentException("With a user email, a realm should be given"));
      }
    }


    if (userId != null) {

      return this.updateUser(user);

    }


    /**
     * Update then insert if needed
     * <p>
     * We don't use the SQL upsert statement
     * to not create a gap in the sequence,
     * we don't use a database sequence.
     * See identifier.md for more information
     */
    return updateUserByEmailAndReturnRowSet(user)
      .compose(rows -> {
        if (rows.size() == 0) {
          return insertUser(user);
        }
        /**
         * The row has only the id updated
         */
        Long resultUserId = rows.iterator().next().getLong(ID_COLUMN);
        user.setLocalId(resultUserId);
        this.computeGuid(user);
        return Future.succeededFuture(user);
      });

  }

  /**
   * Package private because the insertion should be driven by {@link AuthProvider#insertUserFromLoginAuthUserClaims(AuthUser, RoutingContext)}
   *
   * @param user - the user to insert (sign-up)
   * @return a user
   */
  Future<User> insertUser(User user) {


    String sql = "INSERT INTO\n" +
      QUALIFIED_TABLE_NAME + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + EMAIL_COLUMN + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + CREATION_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5)\n";

    return jdbcPool
      .withTransaction(sqlConnection -> SequenceProvider.getNextIdForTableAndRealm(sqlConnection, TABLE_NAME, user.getRealm().getLocalId())
        .compose(
          userId -> {
            user.setLocalId(userId);
            this.computeGuid(user);
            String databaseJsonString = this.toDatabaseJsonString(user);
            return sqlConnection
              .preparedQuery(sql)
              .execute(Tuple.of(
                  user.getRealm().getLocalId(),
                  user.getLocalId(),
                  user.getEmail().toLowerCase(),
                  databaseJsonString,
                  DateTimeUtil.getNowUtc()
                )
              );
          },
          err -> Future.failedFuture(
            TowerFailureException.builder()
              .setCauseException(err)
              .setMessage("UserProvider: Error on next sequence id" + err.getMessage())
              .build()
          )
        ))

      .compose(
        rows -> Future.succeededFuture(user),
        error -> Future.failedFuture(
          TowerFailureException.builder()
            .setCauseException(error)
            .setMessage("Insert User Error:" + error.getMessage() + ". Sql: " + sql)
            .build()
        ));
  }

  @SuppressWarnings("unused")
  private Future<Boolean> exists(User user) {
    String sql;
    Future<RowSet<Row>> futureResponse;
    if (user.getLocalId() != null) {

      sql = "select " + ID_COLUMN +
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
        " where " +
        ID_COLUMN + " = $1 " +
        "AND " + REALM_COLUMN + " = $2 ";
      futureResponse = jdbcPool
        .preparedQuery(sql)
        .execute(Tuple.of(
          user.getLocalId(),
          user.getRealm().getLocalId()
        ));
    } else {
      String email = user.getEmail();
      if (email == null) {
        String failureMessage = "An id or email should be given to check the existence of a user";
        InternalException internalException = new InternalException(failureMessage);
        return Future.failedFuture(internalException);
      }
      sql = "select " + ID_COLUMN +
        " from " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
        " where " +
        EMAIL_COLUMN + " = $1 " +
        "AND " + REALM_COLUMN + " = $2 ";
      futureResponse = jdbcPool
        .preparedQuery(sql)
        .execute(Tuple.of(
          email,
          user.getRealm().getLocalId()
        ));
    }
    return futureResponse
      .onFailure(t -> LOGGER.error("userProvider: exist: Error while executing the following sql:\n" + sql, t))
      .compose(rows -> {
        if (rows.size() == 1) {
          return Future.succeededFuture(true);
        } else {
          return Future.succeededFuture(false);
        }
      });
  }

  /**
   * Replace the database user with the new user object
   * You use {@link #patchUserIfPropertyValueIsNull(User, User)}
   *
   * @param user - the new user data
   * @return the user
   */
  private Future<User> updateUser(User user) {
    String sql;

    if (user.getLocalId() != null) {
      sql = "UPDATE \n" +
        JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " \n" +
        "set \n" +
        "  " + EMAIL_COLUMN + " = $1,\n" +
        "  " + DATA_COLUMN + " = $2,\n" +
        "  " + MODIFICATION_TIME_COLUMN + " = $3\n" +
        "where\n" +
        "  " + ID_COLUMN + "= $4\n" +
        "AND " + REALM_COLUMN + " = $5 ";

      String pgJsonString = this.toDatabaseJsonString(user);
      return jdbcPool
        .preparedQuery(sql)
        .execute(Tuple.of(
          user.getEmail().toLowerCase(),
          pgJsonString,
          DateTimeUtil.getNowUtc(),
          user.getLocalId(),
          user.getRealm().getLocalId()
        ))
        .onFailure(error -> LOGGER.error("User Update: Error:" + error.getMessage() + ". Sql: " + sql, error))
        .compose(ok -> Future.succeededFuture(user));
    }

    if (user.getEmail() == null) {
      InternalException internalException = new InternalException("A id or email is mandatory to update a user");
      return Future.failedFuture(internalException);
    }
    return updateUserByEmailAndReturnRowSet(user)
      .compose(rowSet -> {
        if (rowSet.size() != 1) {
          NoSuchElementException noSuchElementException = new NoSuchElementException("No user was found with the handle (" + user.getEmail() + ")");
          return Future.failedFuture(noSuchElementException);
        }
        Long userId = rowSet.iterator().next().getLong(ID_COLUMN);
        user.setLocalId(userId);
        return Future.succeededFuture(user);
      });
  }

  private Future<RowSet<Row>> updateUserByEmailAndReturnRowSet(User user) {
    String email = user.getEmail();
    if (email == null) {
      InternalException internalException = new InternalException("A email is mandatory");
      return Future.failedFuture(internalException);
    }
    final String updateSql = "UPDATE \n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " \n" +
      "set \n" +
      "  " + DATA_COLUMN + " = $1,\n" +
      "  " + MODIFICATION_TIME_COLUMN + " = $2\n" +
      "where\n" +
      "  " + EMAIL_COLUMN + "= $3\n" +
      "AND " + REALM_COLUMN + " = $4\n" +
      "RETURNING " + ID_COLUMN;
    String dataJsonString = this.toDatabaseJsonString(user);
    return jdbcPool
      .preparedQuery(updateSql)
      .execute(Tuple.of(
        dataJsonString,
        DateTimeUtil.getNowUtc(),
        user.getEmail().toLowerCase(),
        user.getRealm().getLocalId()
      ))
      .onFailure(error -> LOGGER.error("User Update by handle error: Error:" + error.getMessage() + ", Sql: " + updateSql, error));
  }

  private String toDatabaseJsonString(User user) {
    try {
      return this.databaseMapper.writeValueAsString(user);
    } catch (JsonProcessingException e) {
      throw new InternalException("Could not transform user as json string for database", e);
    }
  }

  /**
   * @param realm      - the realmId
   * @param pageId     - the page identifier
   * @param pageSize   - the page size
   * @param searchTerm - the search term (for now works only on email search)
   * @return the realm
   */
  public Future<List<User>> getUsers(Realm realm, Long pageId, Long pageSize, String searchTerm) {
    String searchTermFiltering = "";
    Tuple parametersTuples = Tuple.of(realm.getLocalId(), pageSize, pageId, pageSize, pageId + 1);
    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
      searchTermFiltering = " AND " + EMAIL_COLUMN + " like $6";
      parametersTuples.addString("%" + searchTerm + "%");
    }
    String sql = "select *" +
      " from (" +
      "   SELECT " +
      "      ROW_NUMBER() OVER (ORDER BY user_creation_time DESC) AS rn," +
      "      *" +
      "   FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      "   where " + REALM_COLUMN + " = $1" +
      searchTermFiltering +
      "  ) as userNumbered" +
      " where rn >= 1 + $2::BIGINT * $3::BIGINT" +
      "  and rn < $4::BIGINT * $5::BIGINT + 1";
    return jdbcPool.preparedQuery(sql)
      .execute(parametersTuples)
      .compose(userRows -> {

          List<Future<?>> futureUsers = new ArrayList<>();
          for (Row row : userRows) {
            Future<User> user = getUserFromRow(row, User.class, realm);
            futureUsers.add(user);
          }
          /**
           * https://vertx.io/docs/vertx-core/java/#_future_coordination
           * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
           */
          return Future
            .all(futureUsers)
            .map(CompositeFuture::<User>list);

        },
        err -> Future.failedFuture(new InternalException("Error while retrieving the users: " + err.getMessage() + ". Sql:\n" + sql, err))
      );
  }

  /**
   * @param row        - the resulting row
   * @param userClass  - the user class to return
   * @param knownRealm - the realm that was part of the query or null if unknown
   */
  <T extends User> Future<T> getUserFromRow(Row row, Class<T> userClass, Realm knownRealm) {


    Long userRealmId = row.getLong(REALM_COLUMN);
    Long id = row.getLong(ID_COLUMN);

    /**
     * OrganizationUser realm check
     */
    try {
      this.apiApp.getOrganizationUserProvider().checkOrganizationUserRealmId(userClass, userRealmId);
    } catch (AssertionException e) {
      return Future.failedFuture(TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
        .setMessage("You can't build a organization user from this row. User: " + id + ", realm: " + userRealmId + ")")
        .setCauseException(e)
        .build()
      );
    }


    boolean validRealm = knownRealm != null && knownRealm.getLocalId().equals(userRealmId);
    Future<Realm> realmFuture;
    if (validRealm) {
      realmFuture = Future.succeededFuture(knownRealm);
    } else {
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromId(userRealmId);
    }
    return realmFuture
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(realm -> {


        JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);
        T user = Json.decodeValue(jsonAppData.toBuffer(), userClass);

        user.setLocalId(id);
        user.setRealm(realm);
        this.computeGuid(user);
        user.setEmail(row.getString(EMAIL_COLUMN));
        Boolean disabled = row.getBoolean(DISABLED_COLUMN);
        if (disabled == null) {
          disabled = false;
        }
        user.setDisabled(disabled);
        user.setCreationTime(row.getLocalDateTime(CREATION_COLUMN));
        user.setModificationTime(row.getLocalDateTime(MODIFICATION_TIME_COLUMN));
        return Future.succeededFuture(user);
      });

  }


  private void computeGuid(User user) {
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
   * @param realmId   - the realm id
   * @param userClass - the user class to return
   * @param realm     - an optional realm to use when building the user (Maybe null)
   * @return the user or null if not found
   */
  public <T extends User> Future<T> getUserById(Long userId, Long realmId, Class<T> userClass, Realm realm) {

    assert userId != null;
    assert realmId != null;

    String sql = "SELECT * FROM  " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE \n" +
      " " + ID_COLUMN + " = $1\n" +
      " AND " + REALM_COLUMN + " = $2";
    return jdbcPool.preparedQuery(
        sql)
      .execute(Tuple.of(userId, realmId))
      .onFailure(t -> LOGGER.error("Error while retrieving the user by id. Sql: " + sql, t))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getUserFromRow(row, userClass, realm);

      });
  }


  public Future<? extends User> getUserByEmail(String userEmail, String realmIdentifier) {
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .onFailure(err -> LOGGER.error("getUserByEmail: Error while trying to retrieve the realm", err))
      .compose(realm -> {
          Class<? extends User> userClass;
          if (this.apiApp.isEraldyRealm(realm)) {
            userClass = OrganizationUser.class;
          } else {
            userClass = User.class;
          }
          return getUserByEmail(userEmail, realm.getLocalId(), userClass, realm);
        }
      );
  }

  /**
   * @param userEmail    - the email
   * @param realmLocalId - the realm local id
   * @param userClass    - the type of user
   * @param realm        - the realm to use to build the user (maybe null)
   * @return the user or null if not found
   */
  public <T extends User> Future<T> getUserByEmail(String userEmail, Long realmLocalId, Class<T> userClass, Realm realm) {

    assert userEmail != null;
    assert realmLocalId != null;

    String sql = "SELECT * FROM  " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " +
      EMAIL_COLUMN + " = $1\n" +
      " AND " + REALM_COLUMN + " = $2";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(userEmail.toLowerCase(), realmLocalId))
      .onFailure(t -> LOGGER.error("Error while retrieving the user by email and realm. Sql: \n" + sql, t))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          // return Future.failedFuture(new NotFoundException("the user id (" + userId + ") was not found"));
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getUserFromRow(row, userClass, realm);
      });

  }

  /**
   * @param user the user to retrieve or to insert with at minimal:
   *             * id or email
   *             * and the realm
   * @return the user by id if given or by email, create the user if it does not exist with the provided email
   */
  public Future<User> getOrCreateUserFromEmail(User user) {

    Long userId = user.getLocalId();
    String userEmail = user.getEmail();
    Realm realm = user.getRealm();
    return getUserFromIdOrEmail(userId, userEmail, realm)
      .compose(dbUser -> {
        if (dbUser != null) {
          return Future.succeededFuture(dbUser);
        }

        if (userId != null) {
          throw ValidationException.create("The user with the id was not found", "id", userId);
        }

        return this.upsertUser(user);

      });
  }

  public Future<User> getUserFromIdOrEmail(Long userId, String userEmail, Realm realm) {
    if (userId == null && userEmail == null) {
      throw new IllegalArgumentException("The user email and id cannot be both null");
    }
    if (realm == null || realm.getLocalId() == null) {
      throw new IllegalArgumentException("The realm local id cannot be null");
    }

    Future<User> userFuture;
    if (userId != null) {
      userFuture = this.getUserById(userId, realm.getLocalId(), User.class, realm);
    } else {
      userFuture = this.getUserByEmail(userEmail, realm.getLocalId(), User.class, realm);
    }
    return userFuture;
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

  public <T extends User> Future<T> getUserByGuid(String guid, Class<T> userClass, Realm realm) {

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
        .getRealmFromId(guidObject.getRealmOrOrganizationId());
    }
    return futureRealm
      .compose(realmResult -> {
        if (realmResult == null) {
          return Future.failedFuture(new InternalException("The realm was not found"));
        }
        return this.getUserById(guidObject.validateRealmAndGetFirstObjectId(realmResult.getLocalId()), realmResult.getLocalId(), userClass, realmResult);
      });


  }

  /**
   * @param userRequested   - the user requested
   * @param realmIdentifier - the realm requested
   * @return the realm future
   */
  public Future<Realm> getUserRealmAndUpdateUserIdEventuallyFromRequestData(String realmIdentifier, User userRequested) {

    String userGuid = userRequested.getGuid();
    String userEmail = userRequested.getEmail();

    Future<Realm> realmFuture;
    if (userGuid == null) {
      if (userEmail == null) {
        throw ValidationException.create("The userEmail and the userGuid cannot be both null", "userEmail", null);
      }
      if (realmIdentifier == null) {
        throw ValidationException.create("With the userEmail, a realm Handle or Guid should be given", "realmHandle", null);
      }
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromIdentifier(realmIdentifier, Realm.class);
    } else {

      Guid guid;
      try {
        guid = this.getGuidFromHash(userGuid);
      } catch (CastException e) {
        return Future.failedFuture(new IllegalArgumentException("The user guid is not valid (" + userGuid + ")"));
      }

      long realmId = guid.getRealmOrOrganizationId();

      realmFuture = this.apiApp
        .getRealmProvider()
        .getRealmFromId(realmId);

      long userIdFromGuid = guid.validateRealmAndGetFirstObjectId(realmId);
      userRequested.setLocalId(userIdFromGuid);

    }
    return realmFuture;
  }

  /**
   * This function will set property to the user only
   * if there is none (ie if the value is null).
   * It's used to enhance the actual user profile from Oauth data.
   *
   * @param dbUser    - the actual db user
   * @param patchUser - the user with the patch data
   * @return the database user patched
   */
  public Future<User> patchUserIfPropertyValueIsNull(User dbUser, User patchUser) {

    assert dbUser != null;
    assert patchUser != null;

    boolean patched = false;
    if (dbUser.getGivenName() == null && patchUser.getGivenName() != null) {
      dbUser.setGivenName(patchUser.getGivenName());
      patched = true;
    }
    if (dbUser.getFullName() == null && patchUser.getFullName() != null) {
      dbUser.setFullName(patchUser.getFullName());
      patched = true;
    }
    if (dbUser.getBio() == null && patchUser.getBio() != null) {
      dbUser.setBio(patchUser.getBio());
      patched = true;
    }
    if (dbUser.getLocation() == null && patchUser.getLocation() != null) {
      dbUser.setLocation(patchUser.getLocation());
      patched = true;
    }
    if (dbUser.getWebsite() == null && patchUser.getWebsite() != null) {
      dbUser.setWebsite(patchUser.getWebsite());
      patched = true;
    }
    if (dbUser.getAvatar() == null && patchUser.getAvatar() != null) {
      dbUser.setAvatar(patchUser.getAvatar());
      patched = true;
    }
    if (patched) {
      return this.updateUser(dbUser);
    }
    return Future.succeededFuture(dbUser);
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
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " \n" +
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
        DateTimeUtil.getNowUtc(),
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
  Future<? extends User> getUserByPassword(String userEmail, String userPassword, Realm realm) {

    String hashedPassword = PasswordHashManager.get().hash(userPassword);

    String sql = "SELECT * FROM  " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " +
      EMAIL_COLUMN + " = $1\n" +
      " AND " + REALM_COLUMN + " = $2" +
      " AND " + PASSWORD_COLUMN + " = $3";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(userEmail, realm.getLocalId(), hashedPassword))
      .compose(
        userRows -> {
          if (userRows.size() == 0) {
            return Future.succeededFuture();
          }
          Class<? extends User> userClass;
          if (this.apiApp.isEraldyRealm(realm)) {
            userClass = OrganizationUser.class;
          } else {
            userClass = User.class;
          }
          Row row = userRows.iterator().next();
          return getUserFromRow(row, userClass, realm);
        },
        err -> Future.failedFuture(TowerFailureException.builder()
          .setMessage("Error while retrieving the user by email, password and realm. Sql: \n" + sql)
          .setCauseException(err)
          .build())
      );

  }

  public User toTemplateCloneWithoutRealm(User user) {
    User templateClone = toPublicCloneWithoutRealm(user);
    try {
      templateClone.setGivenName(UsersUtil.getNameOrNameFromEmail(user));
    } catch (NotFoundException | AddressException e) {
      // should not happen
      throw new InternalException(e);
    }
    return templateClone;
  }


  @SuppressWarnings("unused")
  private Future<List<User>> getUsersFromRows(RowSet<Row> userRows, Realm knowRealm) {
    List<Future<User>> users = new ArrayList<>();
    for (Row row : userRows) {
      users.add(getUserFromRow(row, User.class, knowRealm));
    }
    return Future.all(users)
      .compose(results -> {
        List<User> list = results.list();
        return Future.succeededFuture(list);
      });
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

  public Future<User> getUserByIdentifier(String identifier, Realm realm) {

    if (this.isGuid(identifier)) {
      return getUserByGuid(identifier, User.class, realm);
    } else {
      if (realm == null) {
        return Future.failedFuture(new InternalException("With a user email (" + identifier + ") as user identifier, the realm should be provided"));
      }
      return getUserByEmail(identifier, realm.getLocalId(), User.class, realm);
    }
  }


  public Guid createUserGuid(long realmId, Long userId) {
    return this.apiApp.createGuidFromRealmAndObjectId(USR_GUID_PREFIX, realmId, userId);
  }
}
