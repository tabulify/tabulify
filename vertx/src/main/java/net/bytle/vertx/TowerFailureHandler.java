package net.bytle.vertx;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Handler;
import io.vertx.ext.mail.MailMessage;
import net.bytle.exception.Exceptions;

/**
 * Handler for the failures
 * that happen on the Vertx threads
 */
public class TowerFailureHandler implements Handler<Throwable> {

  private final Counter failureCounter;

  private final TowerSmtpClientService mailProvider;

  public TowerFailureHandler(Server server) {


    PrometheusMeterRegistry metricsRegistry;
    Counter failureCounterTemp;

      metricsRegistry = server
        .getMetricsRegistry();
      failureCounterTemp = metricsRegistry
        .counter("vertx_failure");

    failureCounter = failureCounterTemp;
    this.mailProvider = server.getSmtpClient();


    Boolean sendEmailOnErrorConfig = server.getConfigAccessor().getBoolean(SYS_ERROR_EMAIL_CONF, false);
    this.setSendMailOnError(sendEmailOnErrorConfig);

  }

  @Override
  public void handle(Throwable thrown) {

    if (this.failureCounter != null) {
      this.failureCounter.increment();
    }

    /**
     * <p>
     * For info, note that Log4j may also send email
     * <a href="https://logging.apache.org/log4j/log4j-2.7/manual/appenders.html#SMTPAppender">...</a>
     */


    /**
     * Log - the stack trace should be logged
     */
    ContextFailureLogger.CONTEXT_FAILURE_LOGGER.error(thrown.getMessage(), thrown);

    /**
     * Send the email
     */
    if (this.sendEmailOnError) {

      String stackTraceAsString = Exceptions.getStackTraceAsString(thrown);
      MailMessage mailMessage = mailProvider
        .createVertxMailMessage()
        .setFrom(SysAdmin.getEmail())
        .setTo(SysAdmin.getEmail())
        .setSubject("Tower: An error has occurred. " + thrown.getMessage())
        .setText(stackTraceAsString);

      mailProvider
        .getVertxMailClientForSenderWithSigning(SysAdmin.getEmail())
        .sendMail(mailMessage)
        .onFailure(t -> ContextFailureLogger.CONTEXT_FAILURE_LOGGER.error("Error while sending the error email. Message:" + t.getMessage(), t));

    }

  }

  static final String SYS_ERROR_EMAIL_CONF = "sys.on.error.send.email";

  private Boolean sendEmailOnError;

  public TowerFailureHandler setSendMailOnError(boolean b) {
    this.sendEmailOnError = b;
    return this;
  }
}
