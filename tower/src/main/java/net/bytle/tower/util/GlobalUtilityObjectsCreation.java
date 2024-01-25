package net.bytle.tower.util;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.NoSecretException;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object to create global utility object
 * in:
 * * async (vertx) with {@link #handle(Promise)}
 * * sync (test) with {@link #init()}
 */
public class GlobalUtilityObjectsCreation implements Handler<Promise<Void>> {

  public static final Logger INIT_LOGGER = LoggerFactory.getLogger(GlobalUtilityObjectsCreation.class);

  private final Vertx vertx;
  private final ConfigAccessor configAccessor;

  public GlobalUtilityObjectsCreation(Vertx vertx, ConfigAccessor configAccessor) {
    this.vertx = vertx;
    this.configAccessor = configAccessor;

  }

  public static GlobalUtilityObjectsCreation create(Vertx vertx, ConfigAccessor config) {
    return new GlobalUtilityObjectsCreation(vertx, config);
  }

  public void init() throws DbMigrationException, NoSecretException, ConfigIllegalException {


    INIT_LOGGER.info("Start Instantiation of Symmetric Secret Data Encryption");
    CryptoSymmetricUtil
      .config(vertx, configAccessor)
      .create();

    INIT_LOGGER.info("Start Instantiation of Template Engine");
    TemplateEngine
      .config(vertx, configAccessor)
      .create();

    INIT_LOGGER.info("Start instantiation of the password Hash manager");
    PasswordHashManager.init(configAccessor);

  }

  @Override
  public void handle(Promise<Void> event) {

    try {
      this.init();
      event.complete();
    } catch (DbMigrationException | NoSecretException | ConfigIllegalException e) {
      event.fail(e);
    }

  }

}
