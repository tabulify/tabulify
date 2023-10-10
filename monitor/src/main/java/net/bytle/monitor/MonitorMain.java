package net.bytle.monitor;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.TextParseException;

import java.util.concurrent.ExecutionException;

public class MonitorMain extends AbstractVerticle {

  public static Logger LOGGER = LogManager.getLogger(MonitorMain.class);

  public static void main(String[] args) throws TextParseException, ExecutionException, InterruptedException, ConfigIllegalException {

    LOGGER.info("Monitor main started");
    Vertx vertx = Vertx.vertx();
    DeploymentOptions options = new DeploymentOptions().setWorker(true);
    vertx.deployVerticle(MonitorMain.class, options);

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    LOGGER.info("Monitor promise starting");
    ConfigManager.config("monitor", vertx, JsonObject.of())
      .build()
      .getConfigAccessor()
      .onFailure(startPromise::fail)
      .onSuccess(configAccessor -> {
        try {
          LOGGER.info("Monitor api token check starting");
          MonitorApiToken.create(vertx, configAccessor)
            .check();
        } catch (ConfigIllegalException e) {
          startPromise.fail(e);
        }
      });

  }
}
