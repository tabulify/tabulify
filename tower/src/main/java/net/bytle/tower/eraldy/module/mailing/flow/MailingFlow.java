package net.bytle.tower.eraldy.module.mailing.flow;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingInputProps;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingJobInputProps;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.eraldy.module.mailing.model.MailingJobStatus;
import net.bytle.tower.eraldy.module.mailing.model.MailingStatus;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.WebFlowAbs;

import java.util.Arrays;

public class MailingFlow extends WebFlowAbs {


  public MailingFlow(EraldyApiApp towerApp) {
    super(towerApp);

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
        .setType(TowerFailureTypeEnum.BAD_STATE_400)
        .setMessage("The mailing (" + mailing + ") can not be executed because the status is " + status)
        .build()
      );
    }

    Future<Void> createRequest = Future.succeededFuture();
    if (status == MailingStatus.OPEN) {
      createRequest = this.getApp().getMailingProvider().createRequest(mailing);
    }

    return createRequest
      .compose(v -> this.createJob(mailing))
      .compose(this::executeRows)
      .compose(Future::succeededFuture);


  }

  private Future<MailingJob> executeRows(MailingJob mailingJob) {

    return this.getApp().getMailingRowProvider().getRows(mailingJob)
      .compose(rowSet -> {
        if (rowSet.rowCount() == 0) {
          return this.closeJobAndMailing(mailingJob, "No rows to process anymore");
        }
        return Future.succeededFuture(mailingJob);
      });

  }

  private Future<MailingJob> closeJobAndMailing(MailingJob mailingJob, String statusMessage) {
    return this.closeJob(mailingJob, statusMessage, true);
  }

  private Future<MailingJob> closeJob(MailingJob mailingJob, String statusMessage, boolean closeMailing) {


    return this.getApp()
      .getHttpServer()
      .getServer()
      .getPostgresClient()
      .getPool()
      .withTransaction(connection -> {
        /**
         * Update Mailing Job
         */
        MailingJobInputProps mailingJobInputProps = new MailingJobInputProps();
        mailingJobInputProps.setStatusMessage(statusMessage);
        mailingJobInputProps.setStatus(MailingJobStatus.COMPLETED);
        return this.getApp()
          .getMailingJobProvider()
          .updateMailingJob(connection, mailingJob, mailingJobInputProps)
          .compose(v -> {
            if (!closeMailing) {
              return Future.succeededFuture(mailingJob);
            }
            MailingInputProps mailingInputProps = new MailingInputProps();
            mailingInputProps.setStatus(MailingStatus.COMPLETED);
            return Future.failedFuture(new InternalException("Close Mailing not yet implemented"));
          });
      });

  }

  private Future<MailingJob> createJob(Mailing mailing) {


    return this.getApp()
      .getMailingJobProvider()
      .insertMailingJob(mailing);


  }


  /**
   * @return the maximum number of failure on a row
   */
  public int getMaxCountFailureOnRow() {
    return 2;
  }

}
