package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.tower.eraldy.model.manual.MailingJob;
import net.bytle.tower.eraldy.model.manual.MailingJobStatus;
import net.bytle.vertx.DateTimeUtil;

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

    this.jdbcPool = eraldyApiApp.getHttpServer().getServer().getPostgresClient().getPool();

    this.insertSql = "INSERT INTO\n" +
      MAILING_JOB_FULL_QUALIFIED_TABLE_NAME + " (\n" +
      "  " + MAILING_JOB_REALM_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_MAILING_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_STATUS_CODE_COLUMN + ",\n" +
      "  " + MAILING_JOB_START_TIME_COLUMN + ",\n" +
      "  " + MAILING_JOB_COUNT_ROW_TO_EXECUTE_COLUMN +
      "  )\n" +
      " values ($1, $2, $3, $4, $5)";


  }

  public Future<MailingJob> insertMailingJob(Mailing mailing) {

    MailingJob mailingJob = new MailingJob();
    mailingJob.setMailing(mailing);
    mailingJob.setStatus(MailingJobStatus.OPEN);
    mailingJob.setStartTime(DateTimeUtil.getNowInUtc());

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
                mailingJob.getRowCountToExecute()
              ));
          })
          .recover(e -> Future.failedFuture(new InternalException("Mailing Job creation Error: Sql Error " + e.getMessage(), e)))
          .compose(rows -> Future.succeededFuture(mailingJob)));
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
}
