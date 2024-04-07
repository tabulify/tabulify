package net.bytle.tower.eraldy.api.implementer.flow.mailing;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bytle.tower.eraldy.model.openapi.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A unit of execution for a email
 * that can be re-executed if there is a fatal error (timeout, servfail DNS, ...)
 */
public class MailingJobEmail implements Handler<Promise<MailingJobEmail>> {

  static final Logger LOGGER = LogManager.getLogger(MailingJobEmail.class);

  private int executionCount = 0;


  private MailingJobEmailStatus userStatus = MailingJobEmailStatus.OPEN;
  private String statusMessage;

  public MailingJobEmail(MailingJob mailingJob, User user) {

  }

  @Override
  public void handle(Promise<MailingJobEmail> event) {

  }

}
