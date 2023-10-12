package net.bytle.monitor;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import jakarta.mail.MessagingException;
import net.bytle.email.BMailMimeMessage;
import net.bytle.email.BMailSmtpConnectionParameters;
import net.bytle.vertx.ConfigMailSmtpParameters;
import net.bytle.vertx.ConfigManager;
import net.bytle.vertx.MailServiceSmtpProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;

public class MonitorMain extends AbstractVerticle {

  public static Logger LOGGER = LogManager.getLogger(MonitorMain.class);

  public static void main(String[] args) {

    LOGGER.info("Monitor main started");
    Vertx vertx = Vertx.vertx();
    DeploymentOptions options = new DeploymentOptions().setWorker(true);
    vertx.deployVerticle(MonitorMain.class, options);

  }

  @Override
  public void start(Promise<Void> startPromise) {

    LOGGER.info("Monitor promise starting");
    ConfigManager.config("monitor", vertx, JsonObject.of())
      .build()
      .getConfigAccessor()
      .onFailure(this::handleGeneralFailure)
      .onSuccess(configAccessor -> {
        try {
          LOGGER.info("Monitor Config");
          BMailSmtpConnectionParameters smtpInfo = ConfigMailSmtpParameters.createFromConfigAccessor(configAccessor);
          MailServiceSmtpProvider smtpMailProvider = MailServiceSmtpProvider.config(vertx, configAccessor, smtpInfo).create();

          LOGGER.info("Monitor Starting the API Tolen check");
          Future<MonitorReport> monitorReportFuture = MonitorApiToken.create(vertx, configAccessor)
            .check();

          LOGGER.info("Monitor Check Host");
          MonitorNetworkHost monitorHost = MonitorNetworkHost.createForName(MonitorNetworkTopology.BEAU_SERVER_NAME)
            .setIpv4(MonitorNetworkTopology.BEAU_SERVER_IPV4)
            .setIpv6(MonitorNetworkTopology.BEAU_SERVER_IPV6)
            .build();
          MonitorReport hostMonitorReport = MonitorDns.create()
            .checkPtr(monitorHost)
            .getMonitorReport();

          monitorReportFuture
            .onFailure(this::handleGeneralFailure)
            .onSuccess(tokenMonitorReport -> {
              LOGGER.info("Monitor Mailing");
              String emailText = "Api Token\n" +
                tokenMonitorReport.print() +
                "Host check\n" +
                hostMonitorReport.print();
              String mail = "nico@bytle.net";
              BMailMimeMessage email = smtpMailProvider.createBMailMessage()
                .setTo(mail)
                .setFrom("no-reply@bytle.net")
                .setSubject("Monitor " + LocalDate.now())
                .setBodyPlainText(emailText);
              try {
                smtpMailProvider.getBMailClient()
                  .sendMessage(email);
              } catch (MessagingException e) {
                this.handleGeneralFailure(e);
              }

              LOGGER.info("Monitor Successful - closing");
              vertx.close();

            });

        } catch (Exception e) {
          startPromise.fail(e);
          this.handleGeneralFailure(e);
        }
      });

  }

  private void handleGeneralFailure(Throwable e) {
    LOGGER.error(e);
    e.printStackTrace();
    vertx.close();
    System.exit(1);
  }
}
