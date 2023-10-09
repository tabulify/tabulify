package net.bytle.smtp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.type.Casts;
import net.bytle.vertx.ConfigManager;
import net.bytle.vertx.ServerStartLogger;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


public class SmtpVerticle extends AbstractVerticle {

  private static final Logger LOGGER = ServerStartLogger.START_LOGGER;
  private static final String APPLICATION_NAME = "smtp";

  public static final String DEV_KEY_PEM = "../tower/cert/key.pem";
  public static final String DEV_CERT_PEM = "../tower/cert/cert.pem";


  public static void main(String[] args) {

    Launcher.executeCommand("run", SmtpVerticle.class.getName());

  }

  @Override
  public void start(Promise<Void> verticlePromise) {


    LOGGER.info("Smtp Verticle Started");
    ConfigManager.config(APPLICATION_NAME, this.vertx, this.config())
      .build()
      .getConfigAccessor()
      .onFailure(verticlePromise::fail)
      .onSuccess(configAccessor -> vertx.executeBlocking(SmtpConfig.create(this, configAccessor))
        .onFailure(verticlePromise::fail)
        .onSuccess(Void -> {

          /**
           * Smtp Server Configuration
           */
          SmtpServer smtpServer;
          try {
            smtpServer = SmtpServer.create(this, configAccessor);
          } catch (IllegalConfiguration e) {
            verticlePromise.fail(e);
            return;
          }

          /**
           * Build a net server for each smtp service
           */
          List<Future<NetServer>> netServers = new ArrayList<>();
          for (SmtpService smtpService : smtpServer.getSmtpServices()) {

            /**
             * Note on protcol: The protocol is TLS, there is no old SSL allowed
             * The default are = { "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3" }
             * SSLv3 is NOT enabled due to POODLE vulnerability http:/en.wikipedia.org/wiki/POODLE
             * "SSLv2Hello" is NOT enabled since it's disabled by default since JDK7
             */
            /**
             * Note on StartTls: {@link SmtpSslOptionsWithStartTLS} was created to be able to use the STARTTLS option
             * of Netty in order to upgrade the server connection
             * and then send a reply (but unfortunately, it does not work
             * because Vertx wraps it and the wrapper set it back to non-enabled (false)
             * SmtpSslOptionsWithStartTLS sslEngineOptions = SmtpSslOptionsWithStartTLS.create();
             * then
             * NetServerOptions.setSslEngineOptions(sslEngineOptions)
             */
            NetServerOptions serverOption = new NetServerOptions()
              .setPort(smtpService.getListeningPort())
              .setPemKeyCertOptions(
                /**
                 * We have for now only one certificate, but
                 * we may add more than one by {@link SmtpService#getHostedHosts() hosted hosts}
                 */
                new PemKeyCertOptions()
                  .addKeyPath(DEV_KEY_PEM)
                  .addCertPath(DEV_CERT_PEM)
              )
              .setSsl(smtpService.getIsTlsEnabled())
              // SNI returns the certificate for the indicated server name in a SSL connection
              .setSni(smtpService.getIsSniEnabled())
              .setIdleTimeout(smtpServer.getIdleTimeoutSecond())
              .setSslHandshakeTimeout(smtpServer.getHandShakeTimeoutSecond());


            Future<NetServer> futureNetServer = vertx.createNetServer(serverOption)
              .exceptionHandler(SmtpExceptionHandler.create())
              .connectHandler(smtpService::handle)
              .listen();
            netServers.add(futureNetServer);

          }

          /**
           * Promise handling
           */
          Future.all(netServers)
            .onSuccess(result -> {
              try {
                for (NetServer netServer : Casts.castToList(result.list(), NetServer.class)) {
                  LOGGER.info("Smtp server is now listening on port " + netServer.actualPort());
                }
                verticlePromise.complete();
              } catch (CastException e) {
                verticlePromise.fail(e);
              }
            })
            .onFailure(verticlePromise::fail);
        }));

  }


}
