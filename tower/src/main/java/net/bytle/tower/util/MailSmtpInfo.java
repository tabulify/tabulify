package net.bytle.tower.util;

import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.vertx.ConfigAccessor;

import static net.bytle.tower.util.GlobalUtilityObjectsCreation.INIT_LOGGER;

/**
 * An object to move the SMTP data around
 */
public class MailSmtpInfo {


  private static final String MAIL_SMTP_DEFAULT_PORT = "mail.smtp.default.port";
  private static final String MAIL_SMTP_DEFAULT_HOSTNAME = "mail.smtp.default.hostname";
  private static final String MAIL_SMTP_DEFAULT_STARTTLS = "mail.smtp.default.starttls";
  private static final String MAIL_SMTP_DEFAULT_USERNAME = "mail.smtp.default.username";
  private static final String MAIL_SMTP_DEFAULT_PASSWORD = "mail.smtp.default.password";
  private static final int DEFAULT_SMTP_PORT = 25;
  private Integer port;
  private String host;
  private StartTLSOptions startTlsOption;
  private String smtpUserName;
  private String smtpUserPassword;

  public static MailSmtpInfo createFromJson(ConfigAccessor jsonConfig) {

    config config = new config();
    Integer port;
    try {
      port = jsonConfig.getInteger(MAIL_SMTP_DEFAULT_PORT);
    } catch (Exception e) {
      throw new InternalException("The config (" + MAIL_SMTP_DEFAULT_PORT + ") could not be cast to an integer. Error:" + e.getMessage(), e);
    }
    if (port != null) {
      config.setDefaultSmtpPort(port);
      INIT_LOGGER.info("Mail: The mail default port key (" + MAIL_SMTP_DEFAULT_PORT + ") was found with the value (" + port + ").");
    } else {
      INIT_LOGGER.info("Mail: The mail default port key (" + MAIL_SMTP_DEFAULT_PORT + ") was NOT found.");
    }

    String hostname = jsonConfig.getString(MAIL_SMTP_DEFAULT_HOSTNAME);
    if (hostname != null) {
      config.setDefaultSmtpHostname(hostname);
      INIT_LOGGER.info("Mail: The mail default hostname key (" + MAIL_SMTP_DEFAULT_HOSTNAME + ") was found with the value (" + hostname + ").");
    } else {
      INIT_LOGGER.info("Mail: The mail default hostname key (" + MAIL_SMTP_DEFAULT_HOSTNAME + ") was NOT found.");
    }

    String startTls = jsonConfig.getString(MAIL_SMTP_DEFAULT_STARTTLS);
    if (startTls != null) {
      StartTLSOptions startTlsObject;
      try {
        startTlsObject = Casts.cast(startTls, StartTLSOptions.class);
      } catch (CastException e) {
        throw ValidationException.create("The mail config (" + MAIL_SMTP_DEFAULT_STARTTLS + ") value (" + startTls + ") is not a valid start tls value. You may choose one of the following values. " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(StartTLSOptions.class), MAIL_SMTP_DEFAULT_STARTTLS, startTls);
      }
      config.setDefaultStartTls(startTlsObject);
      INIT_LOGGER.info("Mail: The mail default startTLS key (" + MAIL_SMTP_DEFAULT_STARTTLS + ") was found with the value (" + startTlsObject.toString().toLowerCase() + ").");
    } else {
      INIT_LOGGER.info("Mail: The mail default startTLS key (" + MAIL_SMTP_DEFAULT_STARTTLS + ") was NOT found.");
    }

    String username = jsonConfig.getString(MAIL_SMTP_DEFAULT_USERNAME);
    if (username != null) {
      config.setDefaultSmtpUserName(username);
      INIT_LOGGER.info("Mail: The mail default username key (" + MAIL_SMTP_DEFAULT_USERNAME + ") was found with the value (" + username + ").");
    } else {
      INIT_LOGGER.info("Mail: The mail default username key (" + MAIL_SMTP_DEFAULT_USERNAME + ") was NOT found.");
    }

    String password = jsonConfig.getString(MAIL_SMTP_DEFAULT_PASSWORD);
    if (password != null) {
      config.setDefaultSmtpPassword(password);
      INIT_LOGGER.info("Mail: The mail default password key (" + MAIL_SMTP_DEFAULT_PASSWORD + ") was found.");
    } else {
      INIT_LOGGER.info("Mail: The mail default password key (" + MAIL_SMTP_DEFAULT_PASSWORD + ") was NOT found.");
    }
    return config.create();
  }

  public String getHost() {
    return this.host;
  }

  public String getPassword() {
    return this.smtpUserPassword;
  }

  public String getUserName() {
    return this.smtpUserName;
  }

  public Integer getPort() {
    return this.port == null ? DEFAULT_SMTP_PORT : this.port;
  }

  public StartTLSOptions getStartTlsOption() {
    return startTlsOption;
  }


  public static class config {

    private Integer defaultSmtpPort;
    private String defaultSmtpHostname;
    private StartTLSOptions defaultSmtpStartTlsOption;
    private String defaultSmtpUserName;
    private String defaultSmtpUserPassword;

    public config() {
    }

    public MailSmtpInfo create() {


      MailSmtpInfo mailSmtpInfo = new MailSmtpInfo();
      mailSmtpInfo.port = this.defaultSmtpPort;
      mailSmtpInfo.host = this.defaultSmtpHostname;
      mailSmtpInfo.startTlsOption = this.defaultSmtpStartTlsOption;
      mailSmtpInfo.smtpUserName = this.defaultSmtpUserName;
      mailSmtpInfo.smtpUserPassword = this.defaultSmtpUserPassword;

      return mailSmtpInfo;

    }

    /**
     * @param dkimSelector - the dkim selector used to select the public key (test in test and combo in production)
     * @return the config for chaining
     */


    /**
     * @param localSmtpPort - the port of the local smtp service
     * @return the config for chaining
     * It's used primarily to set the port of the wiser smtp service
     * during test
     */
    public config setDefaultSmtpPort(Integer localSmtpPort) {
      this.defaultSmtpPort = localSmtpPort;
      return this;
    }

    public config setDefaultSmtpHostname(String hostname) {
      this.defaultSmtpHostname = hostname;
      return this;
    }

    public config setDefaultStartTls(StartTLSOptions startTLSOptions) {
      this.defaultSmtpStartTlsOption = startTLSOptions;
      return this;
    }

    public config setDefaultSmtpUserName(String username) {
      this.defaultSmtpUserName = username;
      return this;
    }

    public config setDefaultSmtpPassword(String password) {
      this.defaultSmtpUserPassword = password;
      return this;
    }

  }
}
