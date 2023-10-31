package net.bytle.tower.eraldy.objectProvider;


import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.ValidationException;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import jakarta.mail.internet.AddressException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.EraldyRealm;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.PasswordHashManager;
import net.bytle.tower.util.Postgres;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.JdbcPostgresPool;
import net.bytle.vertx.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manage the get/upsert of a {@link User} object asynchronously
 */
public class UserProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(UserProvider.class);

  protected static final String TABLE_NAME = "realm_user";
  protected static final String QUALIFIED_TABLE_NAME = JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME;

  private static final Map<Vertx, UserProvider> mapUserProviderByVertx = new HashMap<>();

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

  public static final String GUID_PREFIX = "usr";

  private final Vertx vertx;
  private static final String CREATION_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;


  public UserProvider(Vertx routingContext) {
    this.vertx = routingContext;
  }

  public static UserProvider createFrom(Vertx vertx) {
    UserProvider publicationProvider;
    publicationProvider = UserProvider.mapUserProviderByVertx.get(vertx);
    if (publicationProvider != null) {
      return publicationProvider;
    }
    publicationProvider = new UserProvider(vertx);
    UserProvider.mapUserProviderByVertx.put(vertx, publicationProvider);
    return publicationProvider;
  }


  public User toPublicCloneWithoutRealm(User user) {
    return toPublicClone(user, false);
  }

  public User toPublicCloneWithRealm(User user) {
    return toPublicClone(user, true);
  }

  private User toPublicClone(User user, Boolean withRealm) {
    User userClone = JsonObject.mapFrom(user).mapTo(User.class);
    if (withRealm) {
      Realm publicRealm = RealmProvider.createFrom(vertx).toPublicClone(user.getRealm());
      userClone.setRealm(publicRealm);
    } else {
      userClone.setRealm(null);
    }
    userClone.setLocalId(null);
    return userClone;
  }


  /**
   * This function will update the user of the userId is known
   * Otherwise, it will try to update it by email.
   * If the update is not successful, the user is {@link #insertUser(User)  inserted}.
   *
   * @param user the publication to upsert
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

  public Future<User> insertUser(User user) {


    String sql = "INSERT INTO\n" +
      JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + EMAIL_COLUMN + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + CREATION_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5)\n";

    return JdbcPostgresPool.getJdbcPool()
      .withTransaction(sqlConnection -> SequenceProvider.getNextIdForTableAndRealm(sqlConnection, TABLE_NAME, user.getRealm().getLocalId())
        .onFailure(error -> LOGGER.error("UserProvider: Error on next sequence id" + error.getMessage(), error))
        .compose(userId -> {
          user.setLocalId(userId);
          JsonObject databaseJsonObject = this.toDatabaseJsonObject(user);
          return sqlConnection
            .preparedQuery(sql)
            .execute(Tuple.of(
                user.getRealm().getLocalId(),
                user.getLocalId(),
                user.getEmail().toLowerCase(),
                databaseJsonObject,
                DateTimeUtil.getNowUtc()
              )
            );
        }))
      .onFailure(error -> LOGGER.error("Insert User Error:" + error.getMessage() + ". Sql: " + sql, error))
      .compose(rows -> Future.succeededFuture(user));
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
      futureResponse = JdbcPostgresPool.getJdbcPool()
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
      futureResponse = JdbcPostgresPool.getJdbcPool()
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

      JsonObject pgJsonObject = this.toDatabaseJsonObject(user);
      return JdbcPostgresPool.getJdbcPool()
        .preparedQuery(sql)
        .execute(Tuple.of(
          user.getEmail().toLowerCase(),
          pgJsonObject,
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
    JsonObject dataJsonObject = this.toDatabaseJsonObject(user);
    return JdbcPostgresPool.getJdbcPool()
      .preparedQuery(updateSql)
      .execute(Tuple.of(
        dataJsonObject,
        DateTimeUtil.getNowUtc(),
        user.getEmail().toLowerCase(),
        user.getRealm().getLocalId()
      ))
      .onFailure(error -> LOGGER.error("User Update by handle error: Error:" + error.getMessage() + ", Sql: " + updateSql, error));
  }

  private JsonObject toDatabaseJsonObject(User user) {
    User userClone = JsonObject.mapFrom(user).mapTo(User.class);
    /**
     * Remove the data that are already in the row
     * We keep the guid for backup :)
     */
    userClone.setRealm(null);
    userClone.setModificationTime(null);
    userClone.setCreationTime(null);
    return JsonObject.mapFrom(userClone);
  }

  /**
   * @param realm - the realmId
   * @return the realm
   */
  public Future<List<User>> getUsers(Realm realm) {

    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();
    return jdbcPool.preparedQuery(
        "SELECT * FROM " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
          " where " + REALM_COLUMN + " = $1"
      )

      .execute(Tuple.of(realm.getLocalId()))
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(userRows -> {

        List<Future<?>> futureUsers = new ArrayList<>();
        for (Row row : userRows) {
          Future<User> user = getUserFromRow(row, realm);
          futureUsers.add(user);
        }
        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future
          .all(futureUsers)
          .onFailure(e -> LOGGER.error("Error on getting all users", e))
          .map(CompositeFuture::<User>list);

      });
  }

  /**
   * @param row        - the resulting row
   * @param knownRealm - the realm that was part of the query or null if unknown
   */
  private Future<User> getUserFromRow(Row row, Realm knownRealm) {

    Future<Realm> realmFuture = Future.succeededFuture(knownRealm);
    if (knownRealm == null) {
      Long realmId = row.getLong(REALM_COLUMN);
      realmFuture = RealmProvider.createFrom(this.vertx)
        .getRealmFromId(realmId);
    }
    return realmFuture
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(realm -> {

        JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);
        User user = Json.decodeValue(jsonAppData.toBuffer(), User.class);

        Long id = row.getLong(ID_COLUMN);
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
    if(user.getGuid()!=null){
      return;
    }
    String guid = Guid.getGuid(GUID_PREFIX, user.getRealm(), user.getLocalId(), vertx);
    user.setGuid(guid);
  }

  public Future<User> getUserById(Long userId, Realm realm) {
    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();
    String sql = "SELECT * FROM  " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE \n" +
      " " + ID_COLUMN + " = $1\n" +
      " AND " + REALM_COLUMN + " = $2";
    return jdbcPool.preparedQuery(
        sql)
      .execute(Tuple.of(userId, realm.getLocalId()))
      .onFailure(t -> LOGGER.error("Error while retrieving the user by id. Sql: " + sql, t))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getUserFromRow(row, realm);

      });
  }


  public Future<User> getUserByEmail(String userEmail, String realmHandle) {
    return RealmProvider.createFrom(this.vertx)
      .getRealmFromHandle(realmHandle)
      .onFailure(err -> LOGGER.error("getUserByEmail: Error while trying to retrieve the realm", err))
      .compose(realm -> getUserByEmail(userEmail, realm));
  }

  /**
   * @param userEmail - the email
   * @param realm     - the realm
   * @return the user or null
   */
  public Future<User> getUserByEmail(String userEmail, Realm realm) {
    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();

    String sql = "SELECT * FROM  " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " +
      EMAIL_COLUMN + " = $1\n" +
      " AND " + REALM_COLUMN + " = $2";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(userEmail.toLowerCase(), realm.getLocalId()))
      .onFailure(t -> LOGGER.error("Error while retrieving the user by email and realm. Sql: \n" + sql, t))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          // return Future.failedFuture(new NotFoundException("the user id (" + userId + ") was not found"));
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getUserFromRow(row, realm);
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
      throw ValidationException.create("The user email and id cannot be both null", "userEmail", null);
    }

    Future<User> userFuture;
    if (userId != null) {
      userFuture = this.getUserById(userId, realm);
    } else {
      userFuture = this.getUserByEmail(userEmail, realm);
    }
    return userFuture;
  }

  public Future<User> getUserFromGuidOrEmail(String userGuid, String userEmail, Realm realm) {
    if (userGuid == null && userEmail == null) {
      throw ValidationException.create("The user email and id cannot be both null", "userEmail", null);
    }

    Long userId = null;
    if (userGuid != null) {
      try {
        userId = Guid.getIdFromGuidAndRealm(userGuid, realm, vertx);
      } catch (CastException e) {
        throw ValidationException.create("The user guid is not valid", "userGuid", userGuid);
      }
    }
    return getUserFromIdOrEmail(userId, userEmail, realm);
  }

  public Future<User> getUserByGuid(String guid) {

    Guid guidObject;
    try {
      guidObject = Guid.createObjectFromRealmIdAndOneObjectId(GUID_PREFIX, guid, vertx);
    } catch (CastException e) {
      throw ValidationException.create("The user guid is not valid", "userGuid", guid);
    }

    return RealmProvider.createFrom(vertx)
      .getRealmFromId(guidObject.getRealmId())
      .compose(realm -> this.getUserById(guidObject.getFirstObjectId(), realm));


  }

  /**
   * @param userRequested  - the user requested
   * @param realmRequested - the realm requested (without id)
   * @return the realm future
   */
  public Future<Realm> getUserRealmAndUpdateUserIdEventuallyFromRequestData(Realm realmRequested, User userRequested) {

    String userGuid = userRequested.getGuid();
    String userEmail = userRequested.getEmail();
    String realmHandle = realmRequested.getHandle();
    String realmGuid = realmRequested.getGuid();
    Future<Realm> realmFuture;
    if (userGuid == null) {
      if (userEmail == null) {
        throw ValidationException.create("The userEmail and the userGuid cannot be both null", "userEmail", null);
      }
      if (realmHandle == null && realmGuid == null) {
        throw ValidationException.create("With the userEmail, a realm Handle or Guid should be given", "realmHandle", null);
      }
      realmFuture = RealmProvider.createFrom(vertx)
        .getRealmFromGuidOrHandle(realmGuid, realmHandle, Realm.class);
    } else {

      Guid guid;
      try {
        guid = Guid.createObjectFromRealmIdAndOneObjectId(GUID_PREFIX, userGuid, vertx);
      } catch (CastException e) {
        return Future.failedFuture(new IllegalArgumentException("The user guid is not valid (" + userGuid + ")"));
      }

      long realmId = guid.getRealmId();

      realmFuture = RealmProvider.createFrom(vertx)
        .getRealmFromId(realmId);

      long userIdFromGuid = guid.getFirstObjectId();
      userRequested.setLocalId(userIdFromGuid);

    }
    return realmFuture;
  }

  /**
   * @param user - the user to create or the user data to patch
   * @return the user
   */
  public Future<User> createOrPatchIfNull(User user) {
    return this.getUserFromIdOrEmail(null, user.getEmail(), user.getRealm())
      .onFailure(error -> LOGGER.error("patchOrCreate: Error on getUserFromIdOrEmail sequence id" + error.getMessage(), error))
      .compose(dbUser -> {
        if (dbUser != null) {
          boolean patched = false;
          if (dbUser.getName() == null && user.getName() != null) {
            dbUser.setName(user.getName());
            patched = true;
          }
          if (dbUser.getFullname() == null && user.getFullname() != null) {
            dbUser.setFullname(user.getFullname());
            patched = true;
          }
          if (dbUser.getBio() == null && user.getBio() != null) {
            dbUser.setBio(user.getBio());
            patched = true;
          }
          if (dbUser.getLocation() == null && user.getLocation() != null) {
            dbUser.setLocation(user.getLocation());
            patched = true;
          }
          if (dbUser.getWebsite() == null && user.getWebsite() != null) {
            dbUser.setWebsite(user.getWebsite());
            patched = true;
          }
          if (dbUser.getAvatar() == null && user.getAvatar() != null) {
            dbUser.setAvatar(user.getAvatar());
            patched = true;
          }
          if (patched) {
            return this.updateUser(dbUser);
          }
          return Future.succeededFuture(dbUser);
        }
        return insertUser(user);
      });
  }

  public Future<User> updatePassword(User user, String password) {

    String sql;

    if (user.getLocalId() == null) {
      throw new InternalException("The user id should not be null.");
    }
    if (user.getRealm() == null) {
      throw new InternalException("The realm should not be null.");
    }
    if (user.getRealm().getLocalId() == null) {
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

    return JdbcPostgresPool.getJdbcPool()
      .preparedQuery(sql)
      .execute(Tuple.of(
        passwordHashed,
        DateTimeUtil.getNowUtc(),
        user.getLocalId(),
        user.getRealm().getLocalId()
      ))
      .onFailure(error -> LOGGER.error("User Password Update: Error:" + error.getMessage() + ". Sql: " + sql, error))
      .compose(ok -> Future.succeededFuture(user));

  }

  /**
   * @param userEmail    - the user email
   * @param userPassword - the password in clear
   * @param realm        - the realm
   * @return a user if the user handle, realm and password combination are good
   */
  public Future<User> getUserByPassword(String userEmail, String userPassword, Realm realm) {

    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();
    String hashedPassword = PasswordHashManager.get().hash(userPassword);

    String sql = "SELECT * FROM  " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME +
      " WHERE " +
      EMAIL_COLUMN + " = $1\n" +
      " AND " + REALM_COLUMN + " = $2" +
      " AND " + PASSWORD_COLUMN + " = $3";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(userEmail, realm.getLocalId(), hashedPassword))
      .onFailure(t -> LOGGER.error("Error while retrieving the user by email and realm. Sql: \n" + sql, t))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          // return Future.failedFuture(new NotFoundException("the user id (" + userId + ") was not found"));
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getUserFromRow(row, realm);
      });

  }

  public User toTemplateCloneWithoutRealm(User user) {
    User templateClone = toPublicCloneWithoutRealm(user);
    try {
      templateClone.setName(UsersUtil.getNameOrNameFromEmail(user));
    } catch (NotFoundException | AddressException e) {
      // should not happen
      throw new InternalException(e);
    }
    return templateClone;
  }

  public Future<User> getEraldyUserById(Long ownerId) {
    return getUserById(ownerId, EraldyRealm.get().getRealm());
  }

  public Future<List<User>> getRecentUsersCreatedFromRealm(Realm realm) {
    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();

    String sql = "SELECT *\n" +
      "FROM  " + QUALIFIED_TABLE_NAME + "\n" +
      "INNER JOIN " + RealmProvider.QUALIFIED_TABLE_NAME + "\n" +
      "ON " + QUALIFIED_TABLE_NAME + "." + REALM_COLUMN + " = " + RealmProvider.QUALIFIED_TABLE_NAME + "." + RealmProvider.ID_COLUMN + "\n" +
      "WHERE\n" +
      REALM_COLUMN + " = $1\n" +
      "order by " + CREATION_COLUMN + " desc\n" +
      "limit 10";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(realm.getLocalId()))
      .onFailure(t -> LOGGER.error("Error while retrieving the recent users of a realm. Sql: \n" + sql, t))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          // return Future.failedFuture(new NotFoundException("the user id (" + userId + ") was not found"));
          return Future.succeededFuture(new ArrayList<>());
        }
        return getUsersFromRows(userRows, realm);
      });
  }

  private Future<List<User>> getUsersFromRows(RowSet<Row> userRows, Realm knowRealm) {
    List<Future<User>> users = new ArrayList<>();
    for (Row row : userRows) {
      users.add(getUserFromRow(row, knowRealm));
    }
    return Future.all(users)
      .compose(results -> {
        List<User> list = results.list();
        return Future.succeededFuture(list);
      });
  }
}
