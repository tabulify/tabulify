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
import java.util.ArrayList;
import java.util.List;

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
          CloudflareApi cloudflareApi = CloudflareApi.create(vertx, configAccessor);

          List<MonitorReport> monitorReports = new ArrayList<>();

          LOGGER.info("Monitor Starting the API Token check");
          MonitorReport apiTokenReport = MonitorApiToken.create(cloudflareApi, configAccessor).check();
          monitorReports.add(apiTokenReport);

          LOGGER.info("Monitor Dns Checks");
          CloudflareDns cloudflareDns = CloudflareDns.create(cloudflareApi);
          MonitorDns monitorDns = MonitorDns.create(cloudflareDns);
          monitorReports.addAll(monitorDns.checkAll());

          LOGGER.info("Monitor Mailing");
          List<Future<String>> futureMonitorPrints = new ArrayList<>();
          for (MonitorReport monitorReport : monitorReports) {
            futureMonitorPrints.add(monitorReport.print());
          }
          Future.join(futureMonitorPrints)
            .onSuccess(res -> {
              StringBuilder emailText = new StringBuilder();
              for (Object s : res.list()) {
                emailText.append(s.toString()).append(MonitorReport.CRLF);
              }
              String mail = "nico@bytle.net";
              BMailMimeMessage email = smtpMailProvider.createBMailMessage()
                .setTo(mail)
                .setFrom("no-reply@bytle.net")
                .setSubject("Monitor " + LocalDate.now())
                .setBodyPlainText(emailText.toString());
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
