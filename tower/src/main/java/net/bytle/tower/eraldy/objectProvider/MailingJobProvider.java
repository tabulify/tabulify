package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.graphql.pojo.input.MailingJobInputProps;
import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.tower.eraldy.model.manual.MailingJob;
import net.bytle.tower.eraldy.model.manual.MailingJobStatus;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.JdbcClient;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailingJobProvider {

  private static final String MAILING_JOB_FULL_QUALIFIED_TABLE_NAME = "cs_jobs.realm_mailing_job";
  private static final String MAILING_JOB_PREFIX = "mailing_job";
  private static final String MAILING_JOB_REALM_ID_COLUMN = MAILING_JOB_PREFIX + "_realm_id";
  private static final String MAILING_JOB_ID_COLUMN = MAILING_JOB_PREFIX + "_id";
  private static final String MAILING_JOB_MAILING_ID_COLUMN = MAILING_JOB_PREFIX + "_mailing_id";
  private static final String MAILING_JOB_STATUS_CODE_COLUMN = MAILING_JOB_PREFIX + "_status_code";
  private static final String MAILING_JOB_STATUS_MESSAGE_COLUMN = MAILING_JOB_PREFIX + "_status_message";
  private static final String MAILING_JOB_START_TIME_COLUMN = MAILING_JOB_PREFIX + "_start_time";
  private static final String MAILING_JOB_END_TIME_MESSAGE_COLUMN = MAILING_JOB_PREFIX + "_end_time";
  private static final String MAILING_JOB_COUNT_ROW_TO_EXECUTE_COLUMN = MAILING_JOB_PREFIX + "_count_row_to_execute";
  private static final String MAILING_JOB_COUNT_ROW_SUCCESS_COLUMN = MAILING_JOB_PREFIX + "_count_row_success";
  private static final String MAILING_JOB_COUNT_ROW_EXECUTION_COLUMN = MAILING_JOB_PREFIX + "_count_row_execution";
  private static final String MAILING_JOB_GUID_PREFIX = "maj";
  private final EraldyApiApp apiApp;
  private final String insertSql;

  private final Pool jdbcPool;


  public MailingJobProvider(EraldyApiApp eraldyApiApp) {
    this.apiApp = eraldyApiApp;

    JdbcClient postgresClient = eraldyApiApp.getHttpServer().getServer().getPostgresClient();
    this.jdbcPool = postgresClient.getPool();

    this.insertSql = "INSERT INTO\n" +
      MAILING_JOB_FULL_QUALIFIED_TABLE_NAME + " (\n" +
      "  " + MAILING_JOB_REALM_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_MAILING_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_STATUS_CODE_COLUMN + ",\n" +
      "  " + MAILING_JOB_START_TIME_COLUMN + ",\n" +
      "  " + MAILING_JOB_COUNT_ROW_TO_EXECUTE_COLUMN +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6)";


  }

  public Future<MailingJob> insertMailingJob(Mailing mailing) {

    MailingJob mailingJob = new MailingJob();
    mailingJob.setMailing(mailing);
    mailingJob.setStatus(MailingJobStatus.OPEN);
    mailingJob.setStartTime(DateTimeService.getNowInUtc());

    return this.apiApp.getMailingProvider().getListAtRequestTime(mailing)
      .compose(emailRecipientList -> {

        /**
         * Number of rows to execute
         */
        Long userInCount = emailRecipientList.getUserInCount();
        if (userInCount == null || userInCount == 0) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.BAD_STATE_400)
              .setMessage("The list ( " + emailRecipientList + ") has no subscribers, we can't execute the mailing (" + mailing + ").")
              .build()
          );
        }
        mailingJob.setCountRowToExecute(userInCount);

        return this.jdbcPool
          .withTransaction(sqlConnection ->
            this.apiApp.getRealmSequenceProvider()
              .getNextIdForTableAndRealm(sqlConnection, mailing.getRealm(), MAILING_JOB_FULL_QUALIFIED_TABLE_NAME)
              .compose(nextId -> {

                mailingJob.setLocalId(nextId);
                this.updateGuid(mailingJob);
                return sqlConnection
                  .preparedQuery(insertSql)
                  .execute(Tuple.of(
                    mailingJob.getMailing().getRealm().getLocalId(),
                    mailingJob.getLocalId(),
                    mailingJob.getMailing().getLocalId(),
                    mailingJob.getStatus().getCode(),
                    mailingJob.getStartTime(),
                    mailingJob.getCountRowToExecute()
                  ));
              })
              .recover(e -> Future.failedFuture(new InternalException("Mailing Job creation Error: Sql Error " + e.getMessage(), e)))
              .compose(rows -> Future.succeededFuture(mailingJob)));
      });

  }

  private void updateGuid(MailingJob mailingJob) {
    mailingJob.setGuid(
      this.apiApp
        .createGuidFromRealmAndObjectId(
          MAILING_JOB_GUID_PREFIX,
          mailingJob.getMailing().getRealm().getLocalId(),
          mailingJob.getLocalId()
        )
        .toString()
    );
  }


  public Future<List<MailingJob>> getMailingJobsRequestHandler(String mailingGuid, RoutingContext routingContext) {
    Guid guid;
    try {
      guid = this.apiApp.getMailingProvider().getGuidObject(mailingGuid);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing guid (" + mailingGuid + ") is not valid", e));
    }

    return this.apiApp.getAuthProvider()
      .getRealmByLocalIdWithAuthorizationCheck(guid.getRealmOrOrganizationId(), AuthUserScope.MAILING_JOBS_GET, routingContext)
      .compose(realm -> {

        Long mailingJobLocalId = guid.validateRealmAndGetFirstObjectId(realm.getLocalId());
        Mailing mailing = new Mailing();
        mailing.setRealm(realm);
        mailing.setLocalId(mailingJobLocalId);
        mailing.setGuid(mailingGuid);

        final String sql = "select * from " + MAILING_JOB_FULL_QUALIFIED_TABLE_NAME + " where " + MAILING_JOB_MAILING_ID_COLUMN + " = $1 and " + MAILING_JOB_REALM_ID_COLUMN + " = $2";
        Tuple tuple = Tuple.of(mailingJobLocalId, realm.getLocalId());
        return this.jdbcPool
          .preparedQuery(sql)
          .execute(tuple)
          .recover(err -> Future.failedFuture(new InternalException("Getting mailing job for the list (" + tuple + ") failed. Error: " + err.getMessage() + ". Sql:\n" + sql, err)))
          .compose(rows -> {
            List<MailingJob> mailingList = new ArrayList<>();
            for (Row row : rows) {
              MailingJob mailingJob = this.buildFromRow(row, mailing);
              mailingList.add(mailingJob);
            }
            return Future.succeededFuture(mailingList);
          });
      });
  }


  private MailingJob buildFromRow(Row row, Mailing mailing) {

    MailingJob mailingJob = new MailingJob();

    /**
     * Ids
     */
    Long mailingJobId = row.getLong(MAILING_JOB_ID_COLUMN);
    mailingJob.setLocalId(mailingJobId);
    Long mailingLocalId = row.getLong(MAILING_JOB_MAILING_ID_COLUMN);
    Long realmLocalId = row.getLong(MAILING_JOB_REALM_ID_COLUMN);
    if (mailing != null) {
      if (!mailingLocalId.equals(mailing.getLocalId())) {
        throw new InternalException("Inconsistency: The mailing local id (" + mailing.getLocalId() + ") is not the same as in the database (" + mailingLocalId + ") for the mailing job (" + mailingJobId + ")");
      }
      if (!realmLocalId.equals(mailing.getRealm().getLocalId())) {
        throw new InternalException("Inconsistency: The realm local id (" + mailing.getRealm().getLocalId() + ") is the same as in the database (" + realmLocalId + ") for the mailing job (" + mailingJobId + ")");
      }
    } else {
      // Build it
      Realm realm = new Realm();
      realm.setLocalId(realmLocalId);
      mailing = new Mailing();
      mailing.setLocalId(mailingLocalId);
      mailing.setRealm(realm);
    }
    mailingJob.setMailing(mailing);
    this.updateGuid(mailingJob);

    /**
     * Props
     */
    mailingJob.setStatus(MailingJobStatus.fromStatusCodeFailSafe(row.getInteger(MAILING_JOB_STATUS_CODE_COLUMN)));
    mailingJob.setStatusMessage(row.getString(MAILING_JOB_STATUS_MESSAGE_COLUMN));
    mailingJob.setStartTime(row.getLocalDateTime(MAILING_JOB_START_TIME_COLUMN));
    mailingJob.setEndTime(row.getLocalDateTime(MAILING_JOB_END_TIME_MESSAGE_COLUMN));
    mailingJob.setCountRowToExecute(row.getLong(MAILING_JOB_COUNT_ROW_TO_EXECUTE_COLUMN));
    mailingJob.setCountRowSuccess(row.getLong(MAILING_JOB_COUNT_ROW_SUCCESS_COLUMN));
    mailingJob.setCountRowExecution(row.getLong(MAILING_JOB_COUNT_ROW_EXECUTION_COLUMN));

    return mailingJob;
  }

  public Future<MailingJob> getMailingJobRequestHandler(String mailingJobGuid, RoutingContext routingContext) {
    Guid guid;
    try {
      guid = this.getGuidObject(mailingJobGuid);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing job guid (" + mailingJobGuid + ") is not valid", e));
    }

    return this.apiApp.getAuthProvider()
      .getRealmByLocalIdWithAuthorizationCheck(guid.getRealmOrOrganizationId(), AuthUserScope.MAILING_JOBS_GET, routingContext)
      .compose(realm -> {


        final String sql = "select * from " + MAILING_JOB_FULL_QUALIFIED_TABLE_NAME + " where " + MAILING_JOB_ID_COLUMN + " = $1 and " + MAILING_JOB_REALM_ID_COLUMN + " = $2";
        Tuple tuple = Tuple.of(guid.validateRealmAndGetFirstObjectId(realm.getLocalId()), realm.getLocalId());
        return this.jdbcPool
          .preparedQuery(sql)
          .execute(tuple)
          .recover(err -> Future.failedFuture(new InternalException("Getting the mailing job (" + tuple + ") failed. Error: " + err.getMessage() + ". Sql:\n" + sql, err)))
          .compose(rows -> {

            if (rows.rowCount() == 0) {
              return Future.failedFuture(TowerFailureException.builder()
                .setType(TowerFailureTypeEnum.NOT_FOUND_404)
                .setMessage("The mailing job (" + mailingJobGuid + ") does not exist")
                .build()
              );
            }

            if (rows.rowCount() != 1) {
              return Future.failedFuture(TowerFailureException.builder()
                .setMessage("The mailing job (" + mailingJobGuid + ") has too much rows (" + rows.rowCount() + ")")
                .build()
              );
            }

            MailingJob mailingJob = this.buildFromRow(rows.iterator().next(), null);

            return Future.succeededFuture(mailingJob);
          });
      });
  }

  private Guid getGuidObject(String mailingJobGuid) throws CastException {
    return this.apiApp
      .createGuidFromHashWithOneRealmIdAndOneObjectId(
        MAILING_JOB_GUID_PREFIX,
        mailingJobGuid
      );
  }

  /**
   * @param connection - because mailing job and mailing may need to update together
   */
  public Future<MailingJob> updateMailingJob(SqlConnection connection, MailingJob mailingJob, MailingJobInputProps mailingJobInputProps) {

    Map<String, Object> updates = new HashMap<>();

    /**
     * Status code
     */
    MailingJobStatus newStatus = mailingJobInputProps.getStatus();
    if (newStatus != null && newStatus != mailingJob.getStatus()) {
      updates.put(MAILING_JOB_STATUS_CODE_COLUMN, newStatus.getCode());
      mailingJob.setStatus(newStatus);
    }

    /**
     * Status Message
     */
    String newStatusMessage = mailingJobInputProps.getStatusMessage();
    if (newStatusMessage != null) {
      updates.put(MAILING_JOB_STATUS_MESSAGE_COLUMN, newStatusMessage);
      mailingJob.setStatusMessage(newStatusMessage);
    }

    if(updates.isEmpty()){
      return Future.succeededFuture(mailingJob);
    }
    StringBuilder updateSql = new StringBuilder();
    List<Object> values = new ArrayList<>();
    updateSql.append("update ")
      .append(MAILING_JOB_FULL_QUALIFIED_TABLE_NAME)
      .append("set ")
    ;

    List<String> equalityStatements = new ArrayList<>();
    for(Map.Entry<String,Object> entry: updates.entrySet()){

      values.add(entry.getValue());
      equalityStatements.add(entry.getKey()+" = $"+values.size());

    }
    updateSql.append(String.join(",",equalityStatements));
    String preparedQuery = updateSql.toString();
    return connection
      .preparedQuery(preparedQuery)
      .execute(Tuple.from(values))
      .recover(e->Future.failedFuture(new InternalException("Mailing Job Update error. Error: "+e.getMessage()+"\nSQL:\n"+preparedQuery,e)))
      .compose(rowSet->Future.succeededFuture(mailingJob));

  }
}
