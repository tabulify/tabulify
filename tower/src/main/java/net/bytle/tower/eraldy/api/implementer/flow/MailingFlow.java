package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.pojo.input.MailingInputProps;
import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.tower.eraldy.model.manual.MailingJob;
import net.bytle.tower.eraldy.model.manual.MailingStatus;
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
        .setType(TowerFailureTypeEnum.BAD_STATUS_400)
        .setMessage("The mailing (" + mailing + ") can not be executed because the status is " + status)
        .build()
      );
    }

    return this.createJob(mailing)
      .compose(mailingJob -> {

        Future<Void> createRequest = Future.succeededFuture();
        if (status == MailingStatus.OPEN) {
          createRequest = this.createRequest(mailingJob);
        }
        return createRequest.compose(v -> this.executeRows(mailingJob));
      })
      .compose(Future::succeededFuture);


  }

  private Future<MailingJob> executeRows(MailingJob mailingJob) {
    return Future.succeededFuture(mailingJob);
  }

  private Future<MailingJob> createJob(Mailing mailing) {


    return this.getApp()
      .getMailingJobProvider()
      .insertMailingJob(mailing);


  }

  /**
   * Create the request: create the lines and change the status of the mailing
   */
  private Future<Void> createRequest(MailingJob mailingJob) {
    MailingInputProps mailingInputProps = new MailingInputProps();
    mailingInputProps.setStatusCode(MailingStatus.PROCESSING.getCode());
    return this.getApp()
      .getMailingProvider()
      .updateMailing(mailingJob.getMailing(), mailingInputProps)
      .compose(v->{
        this.getApp().getListUserProvider().getActiveListUsers(mailingJob.getMailing());
        return Future.succeededFuture();
      });

  }
}
