package net.bytle.tower;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import net.bytle.tower.util.GlobalUtilityObjectsCreation;
import net.bytle.tower.util.PersistentLocalSessionStore;
import net.bytle.vertx.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verticles are chunks of code that get deployed and run by Vert.x.
 */
public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> verticlePromise) {


    LOGGER.info("Main Verticle Started");
    ConfigManager.config("tower",this.vertx, this.config())
      .build()
      .getConfigAccessor()
      .onFailure(verticlePromise::fail)
      .onSuccess(configAccessor -> {

        GlobalUtilityObjectsCreation globalUtilityObjectsCreation = GlobalUtilityObjectsCreation.create(vertx, configAccessor);
        vertx.executeBlocking(globalUtilityObjectsCreation)
          .onFailure(verticlePromise::fail)
          .compose(Void -> Future.succeededFuture())
          .onSuccess(Void -> {

            /**
             * New verticle config
             */
            int instances = 1; // May be Runtime.getRuntime().availableProcessors();
            DeploymentOptions deploymentOptions = new DeploymentOptions()
              .setInstances(instances)
              .setConfig(configAccessor.getConfig());

            /**
             * Deploy HTTP verticle
             */
            vertx
              .deployVerticle(new VerticleHttpServer(), deploymentOptions)
              .onFailure(verticlePromise::fail)
              .onSuccess(result -> verticlePromise.complete());

          });
      });


  }

  /**
   * This stop runs when we send a SIGKill (ie CTRL+C in the terminal, kill on Linux)
   * <p>
   * <a href="https://vertx.io/docs/vertx-core/java/#_asynchronous_verticle_start_and_stop">...</a>
   * <p>
   * IntelliJ's behavior:
   * As of 2023.1 (partially in earlier versions as well), the "Stop" button should work as described:
   * * try performing the graceful shutdown aka SIGTERM on the first press,
   * * hard terminate process (aka SIGKILL) on the second press,
   * This only works in Run mode, not for Debug (since the debugger may be paused
   * at the moment of stop, and resuming it on pressing "Stop Debug" may be unexpected).
   * <a href="https://youtrack.jetbrains.com/issue/RIDER-35566">Ref</a>
   */
  @Override
  public void stop(Promise<Void> stopPromise) {

    /**
     * We use println because the LOGGER does not show always (Why ??)
     */
    String msg = "Main Verticle Stopped";
    LOGGER.info(msg);
    System.out.println(msg + " (ptln)");

    vertx.executeBlocking(p -> {

      System.out.println("Flushing Session Data");
      PersistentLocalSessionStore.get()
        .flush()
        .close();

      stopPromise.complete();
      //stopPromise.fail(); otherwise

    });

  }

}
