package net.bytle.smtp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


public class SmtpVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(SmtpVerticle.class);
  private static final String APPLICATION_NAME = "smtp";


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
          } catch (ConfigIllegalException e) {
            verticlePromise.fail(e);
            return;
          }


          /**
           * Build a net server for each smtp service
           */
          List<Future<NetServer>> netServers = new ArrayList<>();
          for (SmtpService smtpService : smtpServer.getSmtpServices()) {

            /**
             * Server Certificates
             */
            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
            for (SmtpHost host : smtpServer.getHostedHosts().values()) {
              String keyPath = host.getPrivateKeyPath();
              if (keyPath == null) {
                continue;
              }
              String certificatePath = host.getCertificatePath();
              if (certificatePath == null) {
                verticlePromise.fail(new ConfigIllegalException("The certificate of the host (" + host + ") is null but not its key"));
                return;
              }
              pemKeyCertOptions
                .addKeyPath(keyPath)
                .addCertPath(certificatePath);
            }
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
              .setPemKeyCertOptions(pemKeyCertOptions)
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
          Future.join(netServers)
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
