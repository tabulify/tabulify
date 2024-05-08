package net.bytle.tower.eraldy.module.mailing.db.mailingjob;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingJobInputProps;
import net.bytle.tower.eraldy.module.mailing.jackson.JacksonMailingJobGuidDeserializer;
import net.bytle.tower.eraldy.module.mailing.jackson.JacksonMailingJobGuidSerializer;
import net.bytle.tower.eraldy.module.mailing.model.*;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;
import net.bytle.vertx.guid.GuidDeSer;

import java.util.ArrayList;
import java.util.List;

public class MailingJobProvider {


  private static final String MAILING_JOB_GUID_PREFIX = "maj";
  private final EraldyApiApp apiApp;

  private final Pool jdbcPool;
  private final JdbcTable mailingJobTable;


  public MailingJobProvider(EraldyApiApp eraldyApiApp, JdbcSchema jobsSchema) {
    this.apiApp = eraldyApiApp;

    this.mailingJobTable = JdbcTable.build(jobsSchema, "realm_mailing_job", MailingJobCols.values())
      .addPrimaryKeyColumn(MailingJobCols.ID)
      .addPrimaryKeyColumn(MailingJobCols.REALM_ID)
      .build();

    this.jdbcPool = jobsSchema.getJdbcClient().getPool();

    GuidDeSer mailingJobGuidDeser = this.apiApp.getHttpServer().getServer().getHashId().getGuidDeSer(MAILING_JOB_GUID_PREFIX, 2);
    this.apiApp.getJackson()
      .addDeserializer(MailingJobGuid.class, new JacksonMailingJobGuidDeserializer(mailingJobGuidDeser))
      .addSerializer(MailingJobGuid.class, new JacksonMailingJobGuidSerializer(mailingJobGuidDeser));


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

                this.updateGuid(mailingJob, nextId);

                return JdbcInsert.into(mailingJobTable)
                  .addColumn(MailingJobCols.ID, mailingJob.getGuid().getJobId())
                  .addColumn(MailingJobCols.REALM_ID, mailingJob.getGuid().getRealmId())
                  .addColumn(MailingJobCols.MAILING_ID, mailingJob.getMailing().getGuid().getLocalId())
                  .addColumn(MailingJobCols.STATUS_CODE, mailingJob.getStatus().getCode())
                  .addColumn(MailingJobCols.START_TIME, mailingJob.getStartTime())
                  .addColumn(MailingJobCols.ITEM_TO_EXECUTE_COUNT, mailingJob.getItemToExecuteCount())
                  .execute(sqlConnection);
              })
              .compose(rows -> Future.succeededFuture(mailingJob)));
      });

  }

  public void updateGuid(MailingJob mailingJob, long localId) {

    mailingJob.setGuid(
      new MailingJobGuid.Builder()
        .setRealmId(mailingJob.getMailing().getRealm().getGuid().getLocalId())
        .setJobId(localId)
        .build()
    );
  }


  public Future<List<MailingJob>> getMailingJobsRequestHandler(Mailing mailing, RoutingContext routingContext) {

    return this.apiApp.getAuthProvider()
      .checkRealmAuthorization(routingContext, mailing.getRealm(), AuthUserScope.MAILING_JOBS_GET)
      .compose(v -> JdbcSelect.from(this.mailingJobTable)
        .addEqualityPredicate(MailingJobCols.MAILING_ID, mailing.getGuid().getLocalId())
        .addEqualityPredicate(MailingJobCols.REALM_ID, mailing.getGuid().getRealmId())
        .execute()
        .compose(rows -> {
          List<MailingJob> mailingList = new ArrayList<>();
          for (JdbcRow row : rows) {
            MailingJob mailingJob = this.buildFromRow(row, mailing);
            mailingList.add(mailingJob);
          }
          return Future.succeededFuture(mailingList);
        }));
  }


  private MailingJob buildFromRow(JdbcRow row, Mailing mailing) {

    MailingJob mailingJob = new MailingJob();

    /**
     * Ids
     */
    Long mailingJobId = row.getLong(MailingJobCols.ID);
    Long mailingLocalId = row.getLong(MailingJobCols.MAILING_ID);
    Long realmLocalId = row.getLong(MailingJobCols.REALM_ID);
    if (mailing != null) {
      if (!mailingLocalId.equals(mailing.getGuid().getLocalId())) {
        throw new InternalException("Inconsistency: The mailing local id (" + mailing.getGuid().getLocalId() + ") is not the same as in the database (" + mailingLocalId + ") for the mailing job (" + mailingJobId + ")");
      }
      if (!realmLocalId.equals(mailing.getRealm().getGuid().getLocalId())) {
        throw new InternalException("Inconsistency: The realm local id (" + mailing.getRealm().getGuid() + ") is the same as in the database (" + realmLocalId + ") for the mailing job (" + mailingJobId + ")");
      }
    } else {
      // Build it
      Realm realm = Realm.createFromAnyId(realmLocalId);
      mailing = new Mailing();
      mailing.setRealm(realm);
      MailingGuid mailingGuid = new MailingGuid();
      mailingGuid.setLocalId(mailingLocalId);
      mailingGuid.setRealmId(mailing.getRealm().getGuid().getLocalId());
      mailing.setGuid(mailingGuid);
    }
    mailingJob.setMailing(mailing);
    this.updateGuid(mailingJob, mailingJobId);

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

  public Future<MailingJob> getMailingJobRequestHandler(MailingJobGuid mailingJobGuid, RoutingContext routingContext) {

    return this.apiApp.getAuthProvider()
      .getRealmByLocalIdWithAuthorizationCheck(mailingJobGuid.getRealmId(), AuthUserScope.MAILING_JOBS_GET, routingContext)
      .compose(realm -> JdbcSelect.from(this.mailingJobTable)
        .addEqualityPredicate(MailingJobCols.ID, mailingJobGuid.getJobId())
        .addEqualityPredicate(MailingJobCols.REALM_ID, mailingJobGuid.getRealmId())
        .execute()
        .recover(err -> Future.failedFuture(new InternalException("Getting the mailing job (" + mailingJobGuid + ") failed. Error: " + err.getMessage(), err)))
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
        }));
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
      jdbcUpdate.setUpdatedColumnWithValue(MailingJobCols.STATUS_CODE, newStatus.getCode());
      mailingJob.setStatus(newStatus);
    }

    /**
     * Status Message
     */
    String newStatusMessage = mailingJobInputProps.getStatusMessage();
    if (newStatusMessage != null) {
      jdbcUpdate.setUpdatedColumnWithValue(MailingJobCols.STATUS_MESSAGE, newStatusMessage);
      mailingJob.setStatusMessage(newStatusMessage);
    }

    if (jdbcUpdate.hasNoColumnToUpdate()) {
      return Future.succeededFuture(mailingJob);
    }


    jdbcUpdate.addPredicateColumn(MailingJobCols.REALM_ID, mailingJob.getGuid().getRealmId());
    jdbcUpdate.addPredicateColumn(MailingJobCols.ID, mailingJob.getGuid().getJobId());

    return jdbcUpdate.execute(connection)
      .compose(rowSet -> Future.succeededFuture(mailingJob));

  }
}
