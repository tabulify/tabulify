package net.bytle.smtp;

import net.bytle.vertx.ConfigAccessor;

import java.util.concurrent.Callable;

public class SmtpConfig implements Callable<Void> {

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final SmtpVerticle verticle;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final ConfigAccessor configAccessor;

  public SmtpConfig(SmtpVerticle edgeVerticle, ConfigAccessor configAccessor) {
    this.verticle = edgeVerticle;
    this.configAccessor = configAccessor;
  }


  @Override
  public Void call(){

    return null;

  }

  public static SmtpConfig create(SmtpVerticle edgeVerticle, ConfigAccessor configAccessor) {
    return new SmtpConfig(edgeVerticle, configAccessor);
  }


}
