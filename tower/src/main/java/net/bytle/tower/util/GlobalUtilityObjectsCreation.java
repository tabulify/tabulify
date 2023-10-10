package net.bytle.tower.util;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.NoSecretException;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.MailServiceSmtpProvider;
import net.bytle.vertx.MailSmtpInfo;
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
    return new GlobalUtilityObjectsCreation(vertx,config);
  }

  public void init() throws DbMigrationException, NoSecretException, ConfigIllegalException {

    INIT_LOGGER.info("Start creation of JDBC Pool");
    JdbcConnectionInfo jdbcConnectionInfo = JdbcConnectionInfo.createFromJson(configAccessor);
    JdbcPoolCs jdbcPools = JdbcPoolCs.createFromJdbcConnectionInfo(vertx, jdbcConnectionInfo);
    jdbcPools.init();

    INIT_LOGGER.info("Add the SMTP Logger");
    MailSmtpInfo mailSmtpInfo = MailSmtpInfo.createFromConfigAccessor(configAccessor);
    Log4jConfigure.configureOnVertxInit(mailSmtpInfo);

    INIT_LOGGER.info("Start Instantiation of URL Data Encryption");
    JsonToken
      .config(vertx, configAccessor)
      .create();

    INIT_LOGGER.info("Start Instantiation of Symmetric Secret Data Encryption");
    CryptoSymmetricUtil
      .config(vertx, configAccessor)
      .create();

    INIT_LOGGER.info("Start Instantiation of Hash Id");
    HashId
      .config(vertx, configAccessor)
      .create();

    INIT_LOGGER.info("Start Instantiation of Template Engine");
    TemplateEngine
      .config(vertx, configAccessor)
      .create();

    INIT_LOGGER.info("Start Instantiation of Email Engine");
    MailServiceSmtpProvider
      .config(vertx, configAccessor, mailSmtpInfo)
      .create();

    INIT_LOGGER.info("Start instantiation of JWT authentication");
    JwtAuthManager.init(vertx, configAccessor);

    INIT_LOGGER.info("Start instantiation of the password Hash manager");
    PasswordHashManager.init(configAccessor);

    INIT_LOGGER.info("Enable date time in JSON jackson");
    JacksonMapperManager.initVertxJacksonMapper();

    INIT_LOGGER.info("Build MixPanel tracker");
    AnalyticsTracker.createFromJsonObject(configAccessor);

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
