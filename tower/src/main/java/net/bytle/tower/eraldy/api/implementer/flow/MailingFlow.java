package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
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
      .compose(v-> this.createJob(mailing))
      .compose(this::executeRows)
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



}
