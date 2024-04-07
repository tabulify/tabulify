package net.bytle.tower.eraldy.api.implementer.flow.mailing;

import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.vertx.DateTimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A mailing execution that sends email
 */
public class MailingJob {

  static final Logger LOGGER = LogManager.getLogger(MailingJob.class);


  private final Mailing mailing;

  private MailingJobStatus executionStatusCode = MailingJobStatus.OPEN;

  /**
   * A counter of the number of errors on SMTP execution.
   * We log only the first 5 to not be overwhelmed with error.
   */
  private int rowFatalErrorExecutionCounter = 0;



  public MailingJob(Mailing mailing) {

    this.mailing = mailing;

    this.mailing.setCountEmailAddressTotal(0);
    this.mailing.setCountSmtpSuccess(0);
    this.mailing.setCountSmtpExecution(0);
    this.mailing.setLastJobExecutionTime(DateTimeUtil.getNowInUtc());


  }


}
