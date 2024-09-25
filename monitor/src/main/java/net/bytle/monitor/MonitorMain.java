package net.bytle.monitor;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.ext.web.client.WebClient;
import jakarta.mail.MessagingException;
import net.bytle.email.BMailMimeMessage;
import net.bytle.ovh.OvhApiClient;
import net.bytle.vertx.ConfigManager;
import net.bytle.vertx.MainLauncher;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerSmtpClientService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MonitorMain extends AbstractVerticle {

  public static Logger LOGGER = LogManager.getLogger(MonitorMain.class);

  public static void main(String[] args) {

    LOGGER.info("Monitor main started");
    DeploymentOptions options = new DeploymentOptions()
      .setThreadingModel(ThreadingModel.WORKER);
    MainLauncher mainLauncher = new MainLauncher();
    mainLauncher.beforeDeployingVerticle(options);
    mainLauncher.dispatch(new String[]{"run", MonitorMain.class.getName()});

  }

  @Override
  public void start(Promise<Void> startPromise) {

    vertx.exceptionHandler(throwable -> {
      this.handleGeneralFailure(throwable);
      vertx.close();
    });

    LOGGER.info("Monitor promise starting");
    ConfigManager.config("monitor", vertx, JsonObject.of())
      .build()
      .getConfigAccessor()
      .onFailure(this::handleGeneralFailure)
      .onSuccess(configAccessor -> {
        try {

          Server server = Server.create("monitor", "monitor", vertx, configAccessor)
            .enableSmtpClient("eraldy.com")
            .build();

          LOGGER.info("Api client wrapper");
          WebClient webClient = WebClient.create(vertx);
          CloudflareApi cloudflareApi = CloudflareApi.create(webClient, configAccessor);
          OvhApiClient ovhApi = OvhApiClient.builder("ovh", webClient)
            .withConfigAccessor(configAccessor)
            .build();

          LOGGER.info("Monitor Config");


          List<MonitorReport> monitorReports = new ArrayList<>();
          LOGGER.info("Monitor Starting the API Token check");
          MonitorReport apiTokenReport = MonitorApiToken
            .create(cloudflareApi, configAccessor)
            .check();
          monitorReports.add(apiTokenReport);

          LOGGER.info("Monitor Services Checks");
          CloudflareDns cloudflareDns = CloudflareDns.create(cloudflareApi);
          NetClientOptions options = new NetClientOptions();
          options.setSsl(true);
          NetClient sslNetClient = vertx.createNetClient(options);
          MonitorServices monitorServices = MonitorServices.create(cloudflareDns, sslNetClient, configAccessor);
          monitorReports.addAll(monitorServices.checkAll());

          LOGGER.info("Monitor Backup");
          String ovhVpsServiceName = "vps-427a1b7c.vps.ovh.ca";
          MonitorBackup monitorBackup = MonitorBackup.create();
          monitorReports.add(monitorBackup.check(ovhApi, ovhVpsServiceName));


          LOGGER.info("Resolving and Mailing the result");
          List<Future<MonitorReport>> futureMonitorReportResolved = new ArrayList<>();
          for (MonitorReport monitorReport : monitorReports) {
            futureMonitorReportResolved.add(monitorReport.resolve());
          }
          Future.join(futureMonitorReportResolved)
            .onFailure(this::handleGeneralFailure)
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
              TowerSmtpClientService smtpMailProvider = server.getSmtpClient();
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
