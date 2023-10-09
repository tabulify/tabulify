package net.bytle.tower.util;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.*;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoSecretException;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.ConfigAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around <a href="https://vertx.io/docs/vertx-mail-client/java/">...</a>
 * <p>
 * <p>
 * <p>
 * using gmailapi.google.com with HTTPREST
 * <img src="https://ci5.googleusercontent.com/proxy/pqO2HUsLE1uwh7gRdoZa0FC0xp0LDe1w12eQClRzTxBSBac9Qi40cEKBcOcA6fNK4vtS-6qOm4iqKv4UHrgSdmJ1vujnuJ2XpHedTtWpx--U6Tr2Bp9V1pWyR0N9HKxd9LMZGNTrwygVvg=s0-d-e1-ft#https://www.semrush.com/link_building/tracksrv/?id=fc815529-ffbf-4394-a855-e96e2e3f160b" style="width:0;height:0;opacity:0" class="CToWUd" data-bit="iit" jslog="138226; u014N:xr6bB; 53:W2ZhbHNlLDJd">
 * <p>
 * Tracking example:
 * <img src="https://www.semrush.com/link_building/tracksrv/?id=3Dfc815=529-ffbf-4394-a855-e96e2e3f160b" style=3D"width: 0; height: 0; opacity: 0">
 * <img src="https://yj227.keap-link004.com/v2/render/id/data/pixel.png" width="1" height="1" loading="eager">
 * <img src="https://hackernewsletter.us1.list-manage.com/track/open.php?u=3Dfaa8eb4ef3a111cef92c4f3d4&id=3D515437f042&e=3D6a04da4406" height="1" width="1" alt="">
 * Tracking with Ga:
 * <a href=" https://developers.google.com/analytics/devguides/collection/protocol/v1/emai">...</a>l
 * You will need to place an image tag within the email. We recommend placing the image at the bottom of the email, so it does not delay loading the email's main content.
 * <img src="https://www.google-analytics.com/collect?v=1&..."/>
 * <p>
 * List Unsubscribe
 * <code>
 * // .addHeader("X-publication-id", "yj2274736783")
 * // List-Unsubscribe-Post: List-Unsubscribe=One-Click
 * // List-Unsubscribe: <https://yj227.infusionsoft.com/app/optOut/noConfirm/123594159/cc0796985bd11722>, <mailto:unsubscribe-yj227-1867071-129101-123594159-value@infusionmail.com>
 * </code>
 */
public class MailServiceSmtpProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceSmtpProvider.class);

  /**
   * example of generated message id: combostrap_70165127_1668601688544_0
   */
  public static final String MAIL_AGENT_NAME = "combostrap";
  public static final String MAIL_DKIM_SELECTOR = "mail.dkim.selector";

  static private final Map<Vertx, MailServiceSmtpProvider> smtpPoolMap = new HashMap<>();
  /**
   * PKCS8 Private Key Base64 String
   */
  public static final String MAIL_DKIM_PRIVATE = "mail.dkim.private_key";

  public static final String DEFAULT_DKIM_SELECTOR = "combo";
  /**
   * Errors-To: Address to which notifications
   * Not internet standard, Non-standard, discouraged
   * <a href="https://www.rfc-editor.org/rfc/rfc2076.html#section-3.4">...</a>
   */
  public static final String ERRORS_TO_MAIL_HEADER = "Errors-To";
  /**
   * Return-Path: Trace information ???
   * Email header that indicates where and how bounced emails will be processed.
   * <a href="https://www.rfc-editor.org/rfc/rfc1123">...</a>
   * This header is also referred to as a bounce address or reverse path
   */
  public static final String RETURN_PATH_MAIL_HEADER = "Return-Path";
  private static final String BOUNCE_EMAIL = SysAdmin.ADMIN_USER.getEmail();
  /**
   * see <a href="https://policy.hubspot.com/abuse-complaints">...</a>
   */
  public static final String X_REPORT_ABUSE_TO_MAIL_HEADER = "X-Report-Abuse-To";
  private static final String ABUSE_EMAIL = SysAdmin.ADMIN_USER.getEmail();
  public static final String X_MAILER = "X-Mailer";

  private final Vertx vertx;
  private String dkimSelector;

  /**
   * All clients
   */
  private final Map<String, MailClient> transactionalMailClientsMap = new HashMap<>();
  private String dkimPrivateKey;
  private Integer defaultSmtpPort = null;
  private String defaultSmtpHostname = null;
  private Boolean useWiserAsTransactionalClient = false;
  /**
   * A client used in test
   */
  private MailClient wiserMailClient;
  private StartTLSOptions defaultSmtpStartTlsOption = null;
  private String defaultSmtpUserName = null;
  private String defaultSmtpUserPassword = null;


  public MailServiceSmtpProvider(Vertx vertx) {
    this.vertx = vertx;

  }

  public static MailServiceSmtpProvider get(Vertx vertx) {
    MailServiceSmtpProvider mailPool = smtpPoolMap.get(vertx);
    if (mailPool == null) {
      throw new InternalException("The smtp service should exist");
    }
    return mailPool;
  }

  public static config config(Vertx vertx, ConfigAccessor jsonConfig, MailSmtpInfo mailSmtpInfo) {

    config config = new config(vertx);

    /**
     * Data from conf file
     */
    String dkimSelector = jsonConfig.getString(MAIL_DKIM_SELECTOR);
    if (dkimSelector != null) {
      config.setDkimSelector(dkimSelector);
      LOGGER.info("Mail: The mail dkim selector (" + MAIL_DKIM_SELECTOR + ") was found with the value " + dkimSelector);
    } else {
      LOGGER.info("Mail: The mail dkim selector (" + MAIL_DKIM_SELECTOR + ") was not found with the value.");
    }
    String dkimPrivateKey = jsonConfig.getString(MAIL_DKIM_PRIVATE);
    if (dkimPrivateKey != null) {
      config.setDkimPrivateKey(dkimPrivateKey);
      LOGGER.info("Mail: The mail dkim private key (" + MAIL_DKIM_PRIVATE + ") was found");
    } else {
      LOGGER.info("Mail: A mail dkim private key (" + MAIL_DKIM_PRIVATE + ") was NOT found");
    }


    Integer port = mailSmtpInfo.getPort();
    if (port != null) {
      config.setDefaultSmtpPort(port);
      LOGGER.info("Mail: The mail default port key was set with the value (" + port + ").");
    }

    String hostname = mailSmtpInfo.getHost();
    if (hostname != null) {
      config.setDefaultSmtpHostname(hostname);
      LOGGER.info("Mail: The mail default hostname was set with the value (" + hostname + ").");
    }

    StartTLSOptions startTls = mailSmtpInfo.getStartTlsOption();
    if (startTls != null) {
      config.setDefaultStartTls(startTls);
      LOGGER.info("Mail: The mail default startTLS was set with the value (" + startTls.toString().toLowerCase() + ").");
    }

    String username = mailSmtpInfo.getUserName();
    if (username != null) {
      config.setDefaultSmtpUserName(username);
      LOGGER.info("Mail: The mail default username was set with the value (" + username + ").");
    }

    String password = mailSmtpInfo.getPassword();
    if (password != null) {
      config.setDefaultSmtpPassword(password);
      LOGGER.info("Mail: The mail password was set (xxxxx).");
    }

    return config;

  }


  public MailClient getTransactionalMailClientForUser(User user) {


    String email = user.getEmail();
    BMailInternetAddress bMailAddress;
    try {
      bMailAddress = BMailInternetAddress.of(email);
    } catch (AddressException e) {
      throw new InternalException("The user address is not valid (" + email + ")." + e.getMessage(), e);
    }
    String senderDomain = bMailAddress.getDomain();

    if (useWiserAsTransactionalClient) {
      if (this.wiserMailClient != null) {
        return this.wiserMailClient;
      }
      MailConfig config = getMailConfig(senderDomain);
      /**
       * Same as {@link WiserConfiguration#WISER_PORT}
       */
      config.setPort(1081);
      this.wiserMailClient = MailClient.create(vertx, config);
      return this.wiserMailClient;
    }

    MailClient mailClient = transactionalMailClientsMap.get(senderDomain);
    if (mailClient != null) {
      return mailClient;
    }

    MailConfig config = getMailConfig(senderDomain);
    mailClient = MailClient.create(vertx, config);
    transactionalMailClientsMap.put(senderDomain, mailClient);
    return mailClient;

  }

  private MailConfig getMailConfig(String senderDomain) {
    // https://github.com/vert-x3/vertx-mail-client/blob/master/src/test/java/io/vertx/ext/mail/MailWithDKIMSignTest.java
    // https://github.com/gaol/vertx-mail-client/wiki/DKIM-Implementation
    DKIMSignOptions dkimSignOptions = new DKIMSignOptions()
      .setPrivateKey(this.dkimPrivateKey)
      //.setAuid("@example.com") user agent identifier (default @sdid)
      .setSelector(this.dkimSelector)
      .setSdid(senderDomain);

    MailConfig config = new MailConfig()
      .setUserAgent(MAIL_AGENT_NAME)
      .setMaxPoolSize(1)
      .setEnableDKIM(true)
      .setDKIMSignOption(dkimSignOptions);

    /**
     * transactional email server are local for now
     */
    if (this.defaultSmtpHostname != null) {
      config.setHostname(this.defaultSmtpHostname);
    }
    if (this.defaultSmtpPort != null) {
      config.setPort(this.defaultSmtpPort);
    }
    if (this.defaultSmtpStartTlsOption != null) {
      config.setStarttls(this.defaultSmtpStartTlsOption);
    }
    if (this.defaultSmtpUserName != null) {
      config.setUsername(this.defaultSmtpUserName);
    }
    if (this.defaultSmtpUserPassword != null) {
      config.setPassword(this.defaultSmtpUserPassword);
    }
    // MailClient.createShared(vertx, config, "poolName");
    return config;
  }

  public MailServiceSmtpProvider useWiserSmtpServerAsSmtpDestination(Boolean b) {
    this.useWiserAsTransactionalClient = b;
    return this;
  }

  public MailMessage createMailMessage() {
    return new MailMessage()
      .addHeader(ERRORS_TO_MAIL_HEADER, SysAdmin.ADMIN_USER.getEmail())
      .addHeader(RETURN_PATH_MAIL_HEADER, BOUNCE_EMAIL)
      .addHeader(X_REPORT_ABUSE_TO_MAIL_HEADER, ABUSE_EMAIL)
      .addHeader(X_MAILER, "combostrap.com");
    /**
     * Example
     * X-Mailer: Customer.io (dgTQ1wUDAN7xBd3xBQGGMVPhnYtKqXGPZwNilOc=; +https://whatis.customeriomail.com)
     */


  }




  public static class config {
    private final Vertx vertx;
    private String dkimSelector = DEFAULT_DKIM_SELECTOR;
    private String dkimPrivateKey;
    private Integer defaultSmtpPort;
    private String defaultSmtpHostname;
    private StartTLSOptions defaultSmtpStartTlsOption;
    private String defaultSmtpUserName;
    private String defaultSmtpUserPassword;

    public config(Vertx vertx) {
      this.vertx = vertx;
    }

    public MailServiceSmtpProvider create() throws NoSecretException {

      if (this.dkimPrivateKey == null) {
        throw new NoSecretException("A Dkim private key is mandatory to sign email. You can set set it in the conf with the attribute (" + MAIL_DKIM_PRIVATE + ")");
      }

      MailServiceSmtpProvider mailPoolService = new MailServiceSmtpProvider(vertx);
      mailPoolService.dkimPrivateKey = this.dkimPrivateKey;
      mailPoolService.dkimSelector = this.dkimSelector;
      mailPoolService.defaultSmtpPort = this.defaultSmtpPort;
      mailPoolService.defaultSmtpHostname = this.defaultSmtpHostname;
      mailPoolService.defaultSmtpStartTlsOption = this.defaultSmtpStartTlsOption;
      mailPoolService.defaultSmtpUserName = this.defaultSmtpUserName;
      mailPoolService.defaultSmtpUserPassword = this.defaultSmtpUserPassword;
      smtpPoolMap.put(vertx, mailPoolService);
      return mailPoolService;

    }

    /**
     * @param dkimSelector - the dkim selector used to select the public key (test in test and combo in production)
     * @return the config for chaining
     */
    public config setDkimSelector(String dkimSelector) {
      if (dkimSelector == null) {
        throw new InternalException("The dkim selector cannot be null");
      }
      this.dkimSelector = dkimSelector;
      return this;
    }

    public config setDkimPrivateKey(String dkimPrivateKey) {
      this.dkimPrivateKey = dkimPrivateKey;
      return this;
    }

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
