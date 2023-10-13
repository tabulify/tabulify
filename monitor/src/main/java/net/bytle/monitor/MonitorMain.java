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
import java.util.Arrays;
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

          List<MonitorReport> monitorReports = new ArrayList<>();

          LOGGER.info("Monitor Starting the API Tolen check");
          Future<MonitorReport> apiTokenFuture = MonitorApiToken.create(vertx, configAccessor)
            .check();


          MonitorDns monitorDns = MonitorDns.create();
          LOGGER.info("Monitor Check Host");
          MonitorNetworkHost monitorBeauHost = MonitorNetworkHost.createForName("beau.bytle.net")
            .setIpv4("192.99.55.226")
            .setIpv6("2607:5300:201:3100::85b")
            .build();
          MonitorNetworkHost monitorCanaHost = MonitorNetworkHost.createForName("cana.bytle.net")
            .setIpv4("51.79.86.27")
            .build();
          List<MonitorNetworkHost> mailers = new ArrayList<>();
          mailers.add(monitorBeauHost);
          mailers.add(monitorCanaHost);

          @SuppressWarnings("unused") MonitorNetworkHost monitorOegHost = MonitorNetworkHost.createForName("oeg.bytle.net")
            .setIpv4("143.176.206.82")
            .build();
          String mailerLabels = "mailers";
          String mailerDomain = "combostrap.com";
          LOGGER.info("Monitor Check Mailers");
          monitorReports.add(monitorDns.checkMailersARecord(mailers, mailerLabels + "." + mailerDomain));
          monitorReports.add(monitorDns.checkMailersPtr(mailers));


          LOGGER.info("Monitor Check Dns");
          String datacadamiaCom = "datacadamia.com";

          List<String> domains = Arrays.asList(
            "bytle.net",
            mailerDomain,
            datacadamiaCom,
            "eraldy.com",
            "gerardnico.com",
            "persso.com",
            "tabulify.com"
          );

          monitorReports.addAll(monitorDns.checkSpf(mailerDomain, mailerLabels, domains));


          apiTokenFuture
            .onFailure(this::handleGeneralFailure)
            .onSuccess(tokenMonitorReport -> {
              LOGGER.info("Monitor Mailing");
              monitorReports.add(tokenMonitorReport);
              StringBuilder emailText = new StringBuilder();
              for (MonitorReport monitorReport : monitorReports) {
                emailText
                  .append(monitorReport.print())
                  .append("\r\n");
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
