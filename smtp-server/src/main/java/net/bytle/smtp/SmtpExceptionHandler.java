package net.bytle.smtp;

import io.vertx.core.Handler;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.ServerStartLogger;
import org.apache.logging.log4j.Logger;

/**
 * When an exception is thrown and not
 * captured such NPE (null pointer exception)
 */
public class SmtpExceptionHandler implements Handler<Throwable> {


  private static final Logger LOGGER = ServerStartLogger.START_LOGGER;

  public static Handler<Throwable> create() {
    return new SmtpExceptionHandler();
  }

  public static void logTheException(Throwable e) {
    if (JavaEnvs.IS_DEV) {
      e.printStackTrace();
    }
    if (e instanceof SmtpException) {
      LOGGER.error("A SMTP exception has occurred.", e);
      return;
    }
    LOGGER.error("A unforeseen exception has occurred.", e);
  }


  @Override
  public void handle(Throwable event) {
    logTheException(event);
  }

}
