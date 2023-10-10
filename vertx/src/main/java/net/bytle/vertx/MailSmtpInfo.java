package net.bytle.vertx;

import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.json.schema.ValidationException;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An object to move the SMTP data around
 */
public class MailSmtpInfo {


  static Logger LOGGER = LogManager.getLogger(MailSmtpInfo.class);
  private static final String MAIL_SMTP_DEFAULT_PORT = "mail.smtp.default.port";
  private static final String MAIL_SMTP_DEFAULT_HOSTNAME = "mail.smtp.default.hostname";
  private static final String MAIL_SMTP_DEFAULT_STARTTLS = "mail.smtp.default.starttls";
  private static final String MAIL_SMTP_DEFAULT_USERNAME = "mail.smtp.default.username";
  private static final String MAIL_SMTP_DEFAULT_PASSWORD = "mail.smtp.default.password";
  private static final String MAIL_SMTP_ADMIN_EMAIL = "mail.smtp.admin.email";
  private static final int DEFAULT_SMTP_PORT = 25;
  private final MailSmtpInfoConfig config;


  public MailSmtpInfo(MailSmtpInfoConfig MailSmtpInfoConfig) {
    this.config = MailSmtpInfoConfig;
  }

  public static MailSmtpInfo createFromJson(ConfigAccessor jsonConfig) throws ConfigIllegalException {


    MailSmtpInfoConfig MailSmtpInfoConfig = new MailSmtpInfoConfig();
    Integer port;
    try {
      port = jsonConfig.getInteger(MAIL_SMTP_DEFAULT_PORT);
    } catch (Exception e) {
      throw new InternalException("The config (" + MAIL_SMTP_DEFAULT_PORT + ") could not be cast to an integer. Error:" + e.getMessage(), e);
    }
    if (port != null) {
      MailSmtpInfoConfig.setDefaultSmtpPort(port);
      LOGGER.info("Mail: The mail default port key (" + MAIL_SMTP_DEFAULT_PORT + ") was found with the value (" + port + ").");
    } else {
      LOGGER.info("Mail: The mail default port key (" + MAIL_SMTP_DEFAULT_PORT + ") was NOT found.");
    }

    String hostname = jsonConfig.getString(MAIL_SMTP_DEFAULT_HOSTNAME);
    if (hostname != null) {
      MailSmtpInfoConfig.setHostname(hostname);
      LOGGER.info("Mail: The mail default hostname key (" + MAIL_SMTP_DEFAULT_HOSTNAME + ") was found with the value (" + hostname + ").");
    } else {
      LOGGER.info("Mail: The mail default hostname key (" + MAIL_SMTP_DEFAULT_HOSTNAME + ") was NOT found.");
    }

    String startTls = jsonConfig.getString(MAIL_SMTP_DEFAULT_STARTTLS);
    if (startTls != null) {
      StartTLSOptions startTlsObject;
      try {
        startTlsObject = Casts.cast(startTls, StartTLSOptions.class);
      } catch (CastException e) {
        throw ValidationException.create("The mail config (" + MAIL_SMTP_DEFAULT_STARTTLS + ") value (" + startTls + ") is not a valid start tls value. You may choose one of the following values. " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(StartTLSOptions.class), MAIL_SMTP_DEFAULT_STARTTLS, startTls);
      }
      MailSmtpInfoConfig.setDefaultStartTls(startTlsObject);
      LOGGER.info("Mail: The mail default startTLS key (" + MAIL_SMTP_DEFAULT_STARTTLS + ") was found with the value (" + startTlsObject.toString().toLowerCase() + ").");
    } else {
      LOGGER.info("Mail: The mail default startTLS key (" + MAIL_SMTP_DEFAULT_STARTTLS + ") was NOT found.");
    }

    String username = jsonConfig.getString(MAIL_SMTP_DEFAULT_USERNAME);
    if (username != null) {
      MailSmtpInfoConfig.setUserName(username);
      LOGGER.info("Mail: The mail default username key (" + MAIL_SMTP_DEFAULT_USERNAME + ") was found with the value (" + username + ").");
    } else {
      LOGGER.info("Mail: The mail default username key (" + MAIL_SMTP_DEFAULT_USERNAME + ") was NOT found.");
    }

    String password = jsonConfig.getString(MAIL_SMTP_DEFAULT_PASSWORD);
    if (password != null) {
      MailSmtpInfoConfig.setDefaultSmtpPassword(password);
      LOGGER.info("Mail: The mail default password key (" + MAIL_SMTP_DEFAULT_PASSWORD + ") was found.");
    } else {
      LOGGER.info("Mail: The mail default password key (" + MAIL_SMTP_DEFAULT_PASSWORD + ") was NOT found.");
    }

    String adminMEmail = jsonConfig.getString(MAIL_SMTP_ADMIN_EMAIL);
    if (adminMEmail != null) {
      try {
        MailSmtpInfoConfig.setAdminEmail(BMailInternetAddress.of(adminMEmail));
      } catch (AddressException e) {
        throw new ConfigIllegalException("The admin email configuration (" + MAIL_SMTP_ADMIN_EMAIL + ") value (" + adminMEmail + ") is not a valid email");
      }
    }
    return MailSmtpInfoConfig.create();
  }

  public String getHost() {
    return this.config.hostname;
  }

  public String getPassword() {
    return this.config.password;
  }

  public String getUserName() {
    return this.config.userName;
  }

  public Integer getPort() {
    return this.config.defaultSmtpPort == null ? DEFAULT_SMTP_PORT : this.config.defaultSmtpPort;
  }

  public StartTLSOptions getStartTlsOption() {
    return this.config.defaultSmtpStartTlsOption;
  }

  public BMailInternetAddress getAdminEmail() {
    return this.config.adminEmail;
  }


  public static class MailSmtpInfoConfig {

    private Integer defaultSmtpPort;
    private String hostname;
    private StartTLSOptions defaultSmtpStartTlsOption;
    private String userName;
    private String password;
    private BMailInternetAddress adminEmail;

    public MailSmtpInfoConfig() {
    }

    public MailSmtpInfo create() {

      return new MailSmtpInfo(this);

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
    public MailSmtpInfoConfig setDefaultSmtpPort(Integer localSmtpPort) {
      this.defaultSmtpPort = localSmtpPort;
      return this;
    }

    public MailSmtpInfoConfig setHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public MailSmtpInfoConfig setDefaultStartTls(StartTLSOptions startTLSOptions) {
      this.defaultSmtpStartTlsOption = startTLSOptions;
      return this;
    }

    public MailSmtpInfoConfig setUserName(String username) {
      this.userName = username;
      return this;
    }

    public MailSmtpInfoConfig setDefaultSmtpPassword(String password) {
      this.password = password;
      return this;
    }

    public MailSmtpInfoConfig setAdminEmail(BMailInternetAddress adminEmail) {
      this.adminEmail = adminEmail;
      return this;
    }
  }
}
