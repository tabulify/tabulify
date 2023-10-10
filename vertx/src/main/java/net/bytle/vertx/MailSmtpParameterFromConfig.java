package net.bytle.vertx;

import io.vertx.ext.mail.StartTLSOptions;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.email.BMailSmtpConnectionParameters;
import net.bytle.email.BMailStartTls;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An object to move the SMTP data around
 */
public class MailSmtpParameterFromConfig {


  static Logger LOGGER = LogManager.getLogger(MailSmtpParameterFromConfig.class);
  private static final String MAIL_SMTP_DEFAULT_PORT = "mail.smtp.default.port";
  private static final String MAIL_SMTP_DEFAULT_HOSTNAME = "mail.smtp.default.hostname";
  private static final String MAIL_SMTP_DEFAULT_STARTTLS = "mail.smtp.default.starttls";
  private static final String MAIL_SMTP_DEFAULT_USERNAME = "mail.smtp.default.username";
  private static final String MAIL_SMTP_DEFAULT_PASSWORD = "mail.smtp.default.password";
  private static final String MAIL_SMTP_ADMIN_EMAIL = "mail.smtp.admin.email";






  public static BMailSmtpConnectionParameters createFromConfigAccessor(ConfigAccessor configAccessor) throws ConfigIllegalException {


    BMailSmtpConnectionParameters mailSmtpInfoConfig = new BMailSmtpConnectionParameters();
    Integer port;
    try {
      port = configAccessor.getInteger(MAIL_SMTP_DEFAULT_PORT);
    } catch (Exception e) {
      throw new InternalException("The config (" + MAIL_SMTP_DEFAULT_PORT + ") could not be cast to an integer. Error:" + e.getMessage(), e);
    }
    if (port != null) {
      mailSmtpInfoConfig.setDefaultSmtpPort(port);
      LOGGER.info("Mail: The mail default port key (" + MAIL_SMTP_DEFAULT_PORT + ") was found with the value (" + port + ").");
    } else {
      LOGGER.info("Mail: The mail default port key (" + MAIL_SMTP_DEFAULT_PORT + ") was NOT found.");
    }

    String hostname = configAccessor.getString(MAIL_SMTP_DEFAULT_HOSTNAME);
    if (hostname != null) {
      mailSmtpInfoConfig.setHostname(hostname);
      LOGGER.info("Mail: The mail default hostname key (" + MAIL_SMTP_DEFAULT_HOSTNAME + ") was found with the value (" + hostname + ").");
    } else {
      LOGGER.info("Mail: The mail default hostname key (" + MAIL_SMTP_DEFAULT_HOSTNAME + ") was NOT found.");
    }

    String startTls = configAccessor.getString(MAIL_SMTP_DEFAULT_STARTTLS);
    if (startTls != null) {
      BMailStartTls startTlsObject;
      try {
        StartTLSOptions startTlsMaiLClientObject = Casts.cast(startTls, StartTLSOptions.class);
        switch (startTlsMaiLClientObject){
          case DISABLED:
            startTlsObject = BMailStartTls.NONE;
            break;
          case REQUIRED:
            startTlsObject = BMailStartTls.REQUIRE;
            break;
          case OPTIONAL:
            startTlsObject = BMailStartTls.ENABLE;
            break;
          default:
            throw new InternalException("The startls value ("+startTlsMaiLClientObject+") was not processed");
        }
      } catch (CastException e) {
        try {
          startTlsObject = Casts.cast(startTls, BMailStartTls.class);
        } catch (CastException ex) {
          throw IllegalArgumentExceptions.createWithInputNameAndValue("The mail config (" + MAIL_SMTP_DEFAULT_STARTTLS + ") value (" + startTls + ") is not a valid start tls value. You may choose one of the following values. " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(BMailStartTls.class), MAIL_SMTP_DEFAULT_STARTTLS, startTls);
        }
      }
      mailSmtpInfoConfig.setDefaultStartTls(startTlsObject);
      LOGGER.info("Mail: The mail default startTLS key (" + MAIL_SMTP_DEFAULT_STARTTLS + ") was found with the value (" + startTlsObject.toString().toLowerCase() + ").");
    } else {
      LOGGER.info("Mail: The mail default startTLS key (" + MAIL_SMTP_DEFAULT_STARTTLS + ") was NOT found.");
    }

    String username = configAccessor.getString(MAIL_SMTP_DEFAULT_USERNAME);
    if (username != null) {
      mailSmtpInfoConfig.setUserName(username);
      LOGGER.info("Mail: The mail default username key (" + MAIL_SMTP_DEFAULT_USERNAME + ") was found with the value (" + username + ").");
    } else {
      LOGGER.info("Mail: The mail default username key (" + MAIL_SMTP_DEFAULT_USERNAME + ") was NOT found.");
    }

    String password = configAccessor.getString(MAIL_SMTP_DEFAULT_PASSWORD);
    if (password != null) {
      mailSmtpInfoConfig.setDefaultSmtpPassword(password);
      LOGGER.info("Mail: The mail default password key (" + MAIL_SMTP_DEFAULT_PASSWORD + ") was found.");
    } else {
      LOGGER.info("Mail: The mail default password key (" + MAIL_SMTP_DEFAULT_PASSWORD + ") was NOT found.");
    }

    String adminEmail = configAccessor.getString(MAIL_SMTP_ADMIN_EMAIL);
    if (adminEmail != null) {
      try {
        mailSmtpInfoConfig.setAdminEmail(BMailInternetAddress.of(adminEmail));
      } catch (AddressException e) {
        throw new ConfigIllegalException("The admin email configuration (" + MAIL_SMTP_ADMIN_EMAIL + ") value (" + adminEmail + ") is not a valid email");
      }
    }
    return mailSmtpInfoConfig;
  }




}
