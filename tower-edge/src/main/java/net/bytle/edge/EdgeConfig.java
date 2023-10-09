package net.bytle.edge;


import net.bytle.s3.AwsBucket;
import net.bytle.vertx.ConfigAccessor;

import java.util.concurrent.Callable;

public class EdgeConfig implements Callable<Void> {

  private final EdgeVerticle verticle;
  private final ConfigAccessor configAccessor;

  public EdgeConfig(EdgeVerticle edgeVerticle, ConfigAccessor configAccessor) {
    this.verticle = edgeVerticle;
    this.configAccessor = configAccessor;
  }


  @Override
  public Void call() throws Exception {


    AwsBucket.init(this.verticle.getVertx(), this.configAccessor);
    return null;

  }

  public static EdgeConfig create(EdgeVerticle edgeVerticle, ConfigAccessor configAccessor) {
    return new EdgeConfig(edgeVerticle, configAccessor);
  }


}
