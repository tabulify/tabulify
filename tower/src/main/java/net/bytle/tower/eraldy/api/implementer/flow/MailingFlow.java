package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.tower.eraldy.model.manual.MailingJob;
import net.bytle.tower.eraldy.model.manual.MailingJobStatus;
import net.bytle.tower.eraldy.model.manual.MailingStatus;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.WebFlowAbs;

import java.util.Arrays;

public class MailingFlow extends WebFlowAbs {


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


  private final Pool jdbcPool;

  public MailingFlow(EraldyApiApp towerApp) {
    super(towerApp);
    this.jdbcPool = towerApp.getHttpServer().getServer().getPostgresClient().getPool();
  }

  @Override
  public EraldyApiApp getApp() {
    return (EraldyApiApp) super.getApp();
  }

  @Override
  public FlowType getFlowType() {
    return FlowType.MAILING;
  }

  public Future<MailingJob> execute(Mailing mailing) {

    MailingStatus status = mailing.getStatus();

    if (Arrays.asList(MailingStatus.COMPLETED, MailingStatus.PAUSED, MailingStatus.CANCELED).contains(status)) {
      return Future.failedFuture(TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.BAD_STATUS_400)
        .setMessage("The mailing (" + mailing + ") can not be executed because the status is " + status)
        .build()
      );
    }

    return this.createJob(mailing)
      .compose(mailingJob -> {
        Integer countRow = mailing.getCountRow();
        Future<Void> createRows = Future.succeededFuture();
        if (countRow == null) {
          // no line item has been created
          createRows = this.createRows(mailingJob);
        }
        return createRows.compose(v -> this.executeRows(mailingJob));
      })
      .compose(Future::succeededFuture);


  }

  private Future<MailingJob> executeRows(MailingJob mailingJob) {
    return Future.succeededFuture(mailingJob);
  }

  private Future<MailingJob> createJob(Mailing mailing) {


    final String insertSql = "INSERT INTO\n" +
      MAILING_JOB_FULL_QUALIFIED_TABLE_NAME + " (\n" +
      "  " + MAILING_JOB_REALM_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_MAILING_ID_COLUMN + ",\n" +
      "  " + MAILING_JOB_STATUS_CODE_COLUMN + ",\n" +
      "  " + MAILING_JOB_START_TIME_COLUMN + ",\n" +
      "  " + MAILING_JOB_COUNT_ROW_TO_EXECUTE_COLUMN +
      "  )\n" +
      " values ($1, $2, $3, $4, $5)";

    MailingJob mailingJob = new MailingJob();
    mailingJob.setMailing(mailing);
    mailingJob.setStatus(MailingJobStatus.OPEN);
    mailingJob.setStartTime(DateTimeUtil.getNowInUtc());

    return this.jdbcPool
      .withTransaction(sqlConnection ->
        this.getApp().getRealmSequenceProvider()
          .getNextIdForTableAndRealm(sqlConnection, mailing.getRealm(), MAILING_JOB_FULL_QUALIFIED_TABLE_NAME)
          .compose(nextId -> {

            mailingJob.setLocalId(nextId);
            mailingJob.setGuid(
              this
                .getApp()
                .createGuidFromRealmAndObjectId(MAILING_JOB_GUID_PREFIX, mailing.getRealm().getLocalId(), nextId)
                .toString()
            );

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

  private Future<Void> createRows(MailingJob mailing) {
    return Future.succeededFuture();
  }
}
