package net.bytle.edge;

import io.vertx.core.Vertx;
import net.bytle.vertx.ServerStartLogger;

public class EdgeMain {



  public static void main(String[] args) {



    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new EdgeVerticle())
      .onFailure(e -> {
        ServerStartLogger.START_LOGGER.error("Be careful that you need to change the working directory\n" +
          "   * to the tower module\n" +
          "(test are running in the module but main are running in the root of the project)");
        e.printStackTrace();
        System.exit(1);
      })
      .onSuccess(s -> ServerStartLogger.START_LOGGER.info("Edge verticle started"));

  }

}
