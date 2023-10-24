package net.bytle.monitor;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
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

          LOGGER.info("Monitor Services Checks");
          CloudflareDns cloudflareDns = CloudflareDns.create(cloudflareApi);
          NetClientOptions options = new NetClientOptions();
          options.setSsl(true);
          NetClient sslNetClient = vertx.createNetClient(options);
          MonitorServices monitorServices = MonitorServices.create(cloudflareDns, sslNetClient, configAccessor);
          monitorReports.addAll(monitorServices.checkAll());

          LOGGER.info("Monitor Mailing");
          List<Future<MonitorReport>> futureMonitorReportResolved = new ArrayList<>();
          for (MonitorReport monitorReport : monitorReports) {
            futureMonitorReportResolved.add(monitorReport.resolve());
          }
          Future.join(futureMonitorReportResolved)
            .onSuccess(res -> {
              StringBuilder emailText = new StringBuilder();
              int failures = 0;
              for (int i = 0; i < res.size(); i++) {
                MonitorReport monitorReport = res.resultAt(i);
                failures += monitorReport.getFailures();
                emailText.append(monitorReport.print()).append(MonitorReport.CRLF);
              }
              String mail = "nico@bytle.net";
              String subject = "Monitor " + LocalDate.now();
              if (failures == 0) {
                subject += " - Success";
              } else {
                subject += " - " + failures + " failures";
              }
              BMailMimeMessage email = null;
              try {
                email = smtpMailProvider.createBMailMessage()
                  .setTo(mail)
                  .setFrom("no-reply@bytle.net")
                  .setSubject(subject)
                  .setBodyPlainText(emailText.toString())
                  .build();
              } catch (MessagingException e) {
                this.handleGeneralFailure(e);
              }
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
