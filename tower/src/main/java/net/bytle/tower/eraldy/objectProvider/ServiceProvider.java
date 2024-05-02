package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.ValidationException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.ServicePublicMixinWithRealm;
import net.bytle.tower.eraldy.model.openapi.Service;
import net.bytle.tower.eraldy.model.openapi.ServiceSmtp;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.util.Postgres;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.db.JdbcColumn;
import net.bytle.vertx.db.JdbcSchema;
import net.bytle.vertx.db.JdbcSchemaManager;
import net.bytle.vertx.db.JdbcTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage the get/upsert of a Service object asynchronously
 */
public class ServiceProvider {


  public static final String SMTP = "smtp";
  protected static final Logger LOGGER = LoggerFactory.getLogger(ServiceProvider.class);


  private static final String COL_PREFIX = "service";


  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  public static final String DATA_COLUMN = COL_PREFIX + COLUMN_PART_SEP + "data";
  public static final String TYPE_COLUMN = COL_PREFIX + COLUMN_PART_SEP + "type";
  public static final String REALM_COLUMN = COL_PREFIX + COLUMN_PART_SEP + "realm_id";
  private static final String ID_COLUMN = COL_PREFIX + COLUMN_PART_SEP + "id";
  public static final String URI_COLUMN = COL_PREFIX + COLUMN_PART_SEP + "uri";
  private static final String IMPERSONATED_PREFIX = "impersonated";
  public static final String IMPERSONATED_USER_COLUMN = COL_PREFIX + COLUMN_PART_SEP + IMPERSONATED_PREFIX + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  private static final String CREATION_COLUMN = COL_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MODIFICATION_COLUMN = COL_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;


  private final EraldyApiApp apiApp;
  private final Pool jdbcPool;
  private final ObjectMapper apiMapper;
  private final JdbcTable tableName;


  public ServiceProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {

    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();
    this.apiMapper = this.apiApp.getHttpServer().getServer().getJacksonMapperManager().jsonMapperBuilder()
      .addMixIn(Service.class, ServicePublicMixinWithRealm.class)
      .build();

    this.tableName = JdbcTable
      .build(jdbcSchema, "realm_service", new JdbcColumn[]{})
      .build();

  }




  /**
   * Utility function to return the unique uri
   *
   * @param serviceSmtp - the service smtp configuration
   * @return the unique uri for the smtp connection
   */
  public static String getSmtpUri(ServiceSmtp serviceSmtp) {
    return "smtp://" +
      (serviceSmtp.getUserName() != null ? serviceSmtp.getUserName() + "@" : "") +
      (serviceSmtp.getHost() != null ? serviceSmtp.getHost() : "localhost") +
      (serviceSmtp.getPort() != null ? ":" + serviceSmtp.getPort() : "");
  }


  /**
   * @param service the service to upsert
   * @return the realm with the id
   */
  public Future<Service> upsertService(Service service) {


    Realm realm = service.getRealm();
    if (realm == null) {
      return Future.failedFuture(new InternalError("The realm is mandatory when upsert / inserting a service."));
    }


    if (service.getLocalId() != null) {

      return updateService(service);

    }

    return updateByUriAndGetRowSet(service)
      .compose(rowSet -> {
        if (rowSet.rowCount() == 0) {
          return insertService(service);
        }
        if (rowSet.rowCount() > 1) {
          InternalException internalException = new InternalException("Service Update By Uri should update 1 row, not " + rowSet.rowCount());
          return Future.failedFuture(internalException);
        }
        Long serviceId = rowSet.iterator().next().getLong(ID_COLUMN);
        service.setLocalId(serviceId);
        return Future.succeededFuture(service);
      });

  }

  private Future<Service> updateService(Service service) {

    if (service.getLocalId() != null) {
      String insertSql = "UPDATE \n" +
        this.tableName.getFullName() + " \n" +
        "set \n" +
        "  " + URI_COLUMN + " = $1,\n" +
        "  " + TYPE_COLUMN + " = $2,\n" +
        "  " + DATA_COLUMN + " = $3,\n" +
        "  " + IMPERSONATED_USER_COLUMN + " = $4\n" +
        "where\n" +
        "  " + REALM_COLUMN + "= $5\n" +
        " AND " + ID_COLUMN + "= $6";

      Tuple parameters = Tuple.of(
        service.getUri(),
        service.getType(),
        this.getDatabaseObject(service),
        service.getImpersonatedUser() != null ? service.getImpersonatedUser().getGuid().getLocalId() : null,
        service.getRealm().getGuid().getLocalId(),
        service.getLocalId()
      );
      return jdbcPool
        .preparedQuery(insertSql)
        .execute(parameters)
        .onFailure(e -> LOGGER.error("Service Insertion Error:" + e.getMessage() + ". Sql: " + insertSql, e))
        .compose(ok -> Future.succeededFuture(service));
    }

    if (service.getUri() == null) {
      InternalException internalException = new InternalException("A service cannot be updated without a id or uri");
      return Future.failedFuture(internalException);
    }

    /**
     * We don't use a upsert SQL statement
     * to keep the service id sequence without gap and inline with the
     * partition constraint of Postgres
     * See identifier.md
     */
    return this.updateByUriAndGetRowSet(service)
      .compose(rowSet -> {
        if (rowSet.rowCount() == 0) {
          return insertService(service);
        }
        if (rowSet.rowCount() > 1) {
          InternalException internalException = new InternalException("The service update has modified more than one row (" + rowSet.rowCount() + ")");
          return Future.failedFuture(internalException);
        }
        Long serviceId = rowSet.iterator().next().getLong(ID_COLUMN);
        service.setLocalId(serviceId);
        return Future.succeededFuture(service);
      });
  }

  private Future<RowSet<Row>> updateByUriAndGetRowSet(Service service) {

    String updateSql = "UPDATE \n" +
      this.tableName.getFullName() + " \n" +
      "set \n" +
      "  " + TYPE_COLUMN + " = $1,\n" +
      "  " + DATA_COLUMN + " = $2,\n" +
      "  " + IMPERSONATED_USER_COLUMN + " = $3,\n" +
      "  " + MODIFICATION_COLUMN + " = $4\n" +
      "where\n" +
      "  " + REALM_COLUMN + "= $5\n" +
      " AND " + URI_COLUMN + "= $6\n" +
      " RETURNING " + ID_COLUMN;

    Tuple parameters = Tuple.of(
      service.getType(),
      this.getDatabaseObject(service),
      service.getImpersonatedUser() != null ? service.getImpersonatedUser().getGuid().getLocalId() : null,
      DateTimeService.getNowInUtc(),
      service.getRealm().getGuid().getLocalId(),
      service.getUri()
    );
    return jdbcPool
      .preparedQuery(updateSql)
      .execute(parameters)
      .onFailure(e -> LOGGER.error("Service Update Error:" + e.getMessage() + ". Sql: " + updateSql, e));
  }

  private JsonObject getDatabaseObject(Service service) {
    return JsonObject.mapFrom(service.getData());
  }

  private Future<Service> insertService(Service service) {

    String insertSql = "INSERT INTO\n" +
      this.tableName.getFullName() + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + URI_COLUMN + ",\n" +
      "  " + TYPE_COLUMN + ",\n" +
      "  " + DATA_COLUMN + ",\n" +
      "  " + IMPERSONATED_USER_COLUMN + ",\n" +
      "  " + CREATION_COLUMN +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6, $7)\n";

    return jdbcPool
      .withTransaction(sqlConnection -> this.apiApp.getRealmSequenceProvider().getNextIdForTableAndRealm(sqlConnection, service.getRealm(), this.tableName)
        .onFailure(error -> LOGGER.error("ServiceProvider: Error on next sequence id" + error.getMessage(), error))
        .compose(serviceId -> {
          service.setLocalId(serviceId);
          Tuple parameters = Tuple.of(
            service.getRealm().getGuid().getLocalId(),
            service.getLocalId(),
            service.getUri(),
            service.getType(),
            this.getDatabaseObject(service),
            service.getImpersonatedUser() != null ? service.getImpersonatedUser().getGuid().getLocalId() : null,
            DateTimeService.getNowInUtc()
          );
          return sqlConnection
            .preparedQuery(insertSql)
            .execute(parameters
            );
        }))
      .onFailure(error -> LOGGER.error("Insert Service Error:" + error.getMessage() + ". Sql: " + insertSql, error))
      .compose(rowSet -> Future.succeededFuture(service));

  }


  public Future<List<Service>> getServices(Realm realm) {


    return jdbcPool
      .preparedQuery(
        "SELECT * FROM " + this.tableName.getFullName() +
          " WHERE \n" +
          REALM_COLUMN + " = $1"
      )
      .execute(Tuple.of(realm.getGuid().getLocalId()))
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(serviceRows -> {

        /**
         * the {@link CompositeFuture#all(List)}  all function } does not
         * take other thing than a raw future
         */
        List<Future<Service>> futureServices = new ArrayList<>();
        for (Row row : serviceRows) {
          Future<Service> futurePublication = getServiceFromRow(row, realm);
          futureServices.add(futurePublication);
        }

        /**
         * https://vertx.io/docs/vertx-core/java/#_future_coordination
         * https://stackoverflow.com/questions/71936229/vertx-compositefuture-on-completion-of-all-futures
         */
        return Future
          .all(futureServices)
          .onFailure(FailureStatic::failFutureWithTrace)
          .map(CompositeFuture::<Service>list);
      });
  }

  private Future<Service> getServiceFromRow(Row row, Realm realm) {

    Long impersonatedUserId = row.getLong(IMPERSONATED_USER_COLUMN);

    Future<User> futureImpersonatedUser = Future.succeededFuture();
    if (impersonatedUserId != null) {
      futureImpersonatedUser = apiApp.getUserProvider()
        .getUserByLocalId(impersonatedUserId, realm);
    }
    Future<Realm> realmFuture = Future.succeededFuture(realm);
    if (realm == null) {
      Long realmId = row.getLong(REALM_COLUMN);
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromLocalId(realmId);
    }
    return Future.all(futureImpersonatedUser, realmFuture)
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(compositeFuture -> {
        User impersonatedUser = compositeFuture.resultAt(0);
        Realm realmResult = compositeFuture.resultAt(1);
        if (impersonatedUser == null && impersonatedUserId != null) {
          return Future.failedFuture("The impersonated user (" + impersonatedUserId + ") does not exist");
        }
        Long serviceId = row.getLong(ID_COLUMN);
        String serviceUri = row.getString(URI_COLUMN);
        String serviceType = row.getString(TYPE_COLUMN);

        JsonObject jsonAppData = Postgres.getFromJsonB(row, DATA_COLUMN);

        Service service = new Service();
        service.setLocalId(serviceId);
        service.setUri(serviceUri);
        service.setType(serviceType);
        if (impersonatedUser != null) {
          service.setImpersonatedUser(impersonatedUser);
        }
        service.setData(jsonAppData);
        service.setRealm(realmResult);
        return Future.succeededFuture(service);
      });

  }

  public Future<Service> getServiceByGuidOrUri(String guid, String uri, Realm realm) {
    if (guid == null && uri == null) {
      throw ValidationException.create("The service uri and guid cannot be both null", "userEmail", null);
    }

    Long serviceId = null;
    return getServiceFromIdOrUri(serviceId, uri, realm);
  }


  private Future<Service> getServiceFromIdOrUri(Long serviceId, String serviceUri, Realm realm) {
    if (serviceId == null && serviceUri == null) {
      throw ValidationException.create("The service uri and id cannot be both null", "userEmail", null);
    }

    if (serviceId != null) {
      return this.getServiceById(serviceId, realm);
    }

    return this.getServiceByUri(serviceUri, realm);

  }

  private Future<Service> getServiceByUri(String serviceUri, Realm realm) {

    String selectSql = "SELECT * FROM " +
      this.tableName.getFullName() +
      " WHERE\n" +
      " " + URI_COLUMN + " = $1\n" +
      " AND " + REALM_COLUMN + " = $2\n";
    return jdbcPool
      .preparedQuery(selectSql)
      .execute(Tuple.of(
        serviceUri,
        realm.getGuid().getLocalId()
      ))
      .onFailure(e -> LOGGER.error("Error in selecting the service by URI. Error" + e.getMessage() + ". Sql\n" + selectSql, e))
      .compose(userRows -> {

        if (userRows.size() == 0) {
          // return Future.failedFuture(new NotFoundException("the user id (" + userId + ") was not found"));
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getServiceFromRow(row, realm);

      });
  }

  private Future<Service> getServiceById(Long serviceId, Realm realm) {


    String sql = "SELECT * FROM " +
      this.tableName.getFullName() + "\n" +
      "WHERE \n" +
      " " + REALM_COLUMN + " = $1\n" +
      " AND " + ID_COLUMN + " = $2?";

    Tuple parameters = Tuple.of(
      realm.getGuid().getLocalId(),
      serviceId
    );
    return jdbcPool
      .preparedQuery(sql)
      .execute(parameters)
      .onFailure(FailureStatic::failFutureWithTrace)
      .compose(userRows -> {

        if (userRows.size() == 0) {
          // return Future.failedFuture(new NotFoundException("the user id (" + userId + ") was not found"));
          return Future.succeededFuture();
        }

        Row row = userRows.iterator().next();
        return getServiceFromRow(row, realm);

      });
  }


  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }
}
