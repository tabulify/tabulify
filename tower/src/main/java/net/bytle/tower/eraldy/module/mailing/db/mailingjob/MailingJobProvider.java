package net.bytle.tower.eraldy.module.mailing.db.mailingjob;

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
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingJobInputProps;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.eraldy.module.mailing.model.MailingJobStatus;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;

import java.util.ArrayList;
import java.util.List;

public class MailingJobProvider {



  private static final String MAILING_JOB_GUID_PREFIX = "maj";
  private final EraldyApiApp apiApp;

  private final Pool jdbcPool;
  private final JdbcTable mailingJobTable;


  public MailingJobProvider(EraldyApiApp eraldyApiApp, JdbcSchema jobsSchema) {
    this.apiApp = eraldyApiApp;

    this.mailingJobTable =  JdbcTable.build(jobsSchema,"realm_mailing_job")
      .addPrimaryKeyColumn(MailingJobCols.ID)
      .addPrimaryKeyColumn(MailingJobCols.REALM_ID)
      .build();

    this.jdbcPool = jobsSchema.getJdbcClient().getPool();




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
        mailingJob.setItemToExecuteCount(userInCount);

        return this.jdbcPool
          .withTransaction(sqlConnection ->
            this.apiApp.getRealmSequenceProvider()
              .getNextIdForTableAndRealm(sqlConnection, mailing.getRealm(), this.mailingJobTable)
              .compose(nextId -> {

                mailingJob.setLocalId(nextId);
                this.updateGuid(mailingJob);

                return JdbcInsert.into(mailingJobTable)
                  .addColumn(MailingJobCols.ID, mailingJob.getLocalId())
                  .addColumn(MailingJobCols.REALM_ID, mailingJob.getMailing().getRealm().getLocalId())
                  .addColumn(MailingJobCols.MAILING_ID, mailingJob.getMailing().getLocalId())
                  .addColumn(MailingJobCols.STATUS_CODE, mailingJob.getStatus().getCode())
                  .addColumn(MailingJobCols.START_TIME, mailingJob.getStartTime())
                  .addColumn(MailingJobCols.ITEM_TO_EXECUTE_COUNT, mailingJob.getItemToExecuteCount())
                  .execute(sqlConnection);
              })
              .compose(rows -> Future.succeededFuture(mailingJob)));
      });

  }

  public void updateGuid(MailingJob mailingJob) {
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
    Guid mailingGuidObject;
    try {
      mailingGuidObject = this.apiApp.getMailingProvider().getGuidObject(mailingGuid);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing guid (" + mailingGuid + ") is not valid", e));
    }

    return this.apiApp.getAuthProvider()
      .getRealmByLocalIdWithAuthorizationCheck(mailingGuidObject.getRealmOrOrganizationId(), AuthUserScope.MAILING_JOBS_GET, routingContext)
      .compose(realm -> {

        Long mailingLocalId = mailingGuidObject.validateRealmAndGetFirstObjectId(realm.getLocalId());
        Mailing mailing = new Mailing();
        mailing.setRealm(realm);
        mailing.setLocalId(mailingLocalId);
        mailing.setGuid(mailingGuid);

        final String sql = "select * from " + this.mailingJobTable.getFullName() + " where " + MailingJobCols.MAILING_ID.getColumnName() + " = $1 and " + MailingJobCols.REALM_ID.getColumnName() + " = $2";
        Tuple tuple = Tuple.of(mailing.getLocalId(), mailing.getRealm().getLocalId());
        return this.jdbcPool
          .preparedQuery(sql)
          .execute(tuple)
          .recover(err -> Future.failedFuture(new InternalException("Getting mailing job for the list (" + tuple + ") failed. Error: " + err.getMessage() + ". Sql:\n" + sql, err)))
          .compose(rows -> {
            List<MailingJob> mailingList = new ArrayList<>();
            for (Row row : rows) {
              JdbcRow rowV = new JdbcRow(row);
              MailingJob mailingJob = this.buildFromRow(rowV, mailing);
              mailingList.add(mailingJob);
            }
            return Future.succeededFuture(mailingList);
          });
      });
  }


  private MailingJob buildFromRow(JdbcRow row, Mailing mailing) {

    MailingJob mailingJob = new MailingJob();

    /**
     * Ids
     */
    Long mailingJobId = row.getLong(MailingJobCols.ID);
    mailingJob.setLocalId(mailingJobId);
    Long mailingLocalId = row.getLong(MailingJobCols.MAILING_ID);
    Long realmLocalId = row.getLong(MailingJobCols.REALM_ID);
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
    mailingJob.setStatus(MailingJobStatus.fromStatusCodeFailSafe(row.getInteger(MailingJobCols.STATUS_CODE)));
    mailingJob.setStatusMessage(row.getString(MailingJobCols.STATUS_MESSAGE));
    mailingJob.setStartTime(row.getLocalDateTime(MailingJobCols.START_TIME));
    mailingJob.setEndTime(row.getLocalDateTime(MailingJobCols.END_TIME));
    mailingJob.setItemToExecuteCount(row.getLong(MailingJobCols.ITEM_TO_EXECUTE_COUNT));
    mailingJob.setItemSuccessCount(row.getLong(MailingJobCols.ITEM_SUCCESS_COUNT));
    mailingJob.setItemExecutionCount(row.getLong(MailingJobCols.ITEM_EXECUTION_COUNT));

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


        final String sql = "select * from " + this.mailingJobTable.getFullName() + " where " + MailingJobCols.ID.getColumnName() + " = $1 and " + MailingJobCols.REALM_ID.getColumnName() + " = $2";
        Tuple tuple = Tuple.of(guid.validateRealmAndGetFirstObjectId(realm.getLocalId()), realm.getLocalId());
        return this.jdbcPool
          .preparedQuery(sql)
          .execute(tuple)
          .recover(err -> Future.failedFuture(new InternalException("Getting the mailing job (" + tuple + ") failed. Error: " + err.getMessage() + ". Sql:\n" + sql, err)))
          .compose(rowSet -> {
            JdbcRowSet rows = new JdbcRowSet(rowSet);
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

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.mailingJobTable);

    /**
     * Status code
     */
    MailingJobStatus newStatus = mailingJobInputProps.getStatus();
    if (newStatus != null && newStatus != mailingJob.getStatus()) {
      jdbcUpdate.addUpdatedColumn(MailingJobCols.STATUS_CODE, newStatus.getCode());
      mailingJob.setStatus(newStatus);
    }

    /**
     * Status Message
     */
    String newStatusMessage = mailingJobInputProps.getStatusMessage();
    if (newStatusMessage != null) {
      jdbcUpdate.addUpdatedColumn(MailingJobCols.STATUS_MESSAGE, newStatusMessage);
      mailingJob.setStatusMessage(newStatusMessage);
    }

    if (jdbcUpdate.hasNoColumnToUpdate()) {
      return Future.succeededFuture(mailingJob);
    }


    jdbcUpdate.addPredicateColumn(MailingJobCols.REALM_ID,mailingJob.getMailing().getRealm().getLocalId());
    jdbcUpdate.addPredicateColumn(MailingJobCols.ID,mailingJob.getLocalId());

    return jdbcUpdate.execute(connection)
      .compose(rowSet->Future.succeededFuture(mailingJob));

  }
}
