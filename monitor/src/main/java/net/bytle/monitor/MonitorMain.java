package net.bytle.monitor;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import net.bytle.email.BMailMimeMessage;
import net.bytle.email.BMailSmtpConnectionParameters;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.ConfigManager;
import net.bytle.vertx.MailServiceSmtpProvider;
import net.bytle.vertx.MailSmtpParameterFromConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
          LOGGER.info("Monitor api token check starting");
          BMailSmtpConnectionParameters smtpInfo = MailSmtpParameterFromConfig.createFromConfigAccessor(configAccessor);
          MailServiceSmtpProvider smtpMailProvider = MailServiceSmtpProvider.config(vertx, configAccessor, smtpInfo).create();

          Future<MonitorReport> monitorReportFuture = MonitorApiToken.create(vertx, configAccessor)
            .check();

          monitorReportFuture
            .onFailure(this::handleGeneralFailure)
            .onSuccess(monitor -> {
              String emailBody = monitor.print();
              String mail = "nico@bytle.net";
              BMailMimeMessage email = smtpMailProvider.createBMailMessage()
                .setTo(mail)
                .setFrom("monitor@bytle.net")
                .setSubject("Monitor")
                .setBodyPlainText(emailBody);
              smtpMailProvider.getBMailClient();



              vertx.close();
            });


        } catch (ConfigIllegalException e) {
          startPromise.fail(e);
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
