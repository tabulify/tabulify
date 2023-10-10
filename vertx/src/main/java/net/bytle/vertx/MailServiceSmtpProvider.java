package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.*;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoSecretException;
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

  private final BMailInternetAddress ABUSE_EMAIL;

  private final BMailInternetAddress BOUNCE_EMAIL ;
  /**
   * see <a href="https://policy.hubspot.com/abuse-complaints">...</a>
   */
  public static final String X_REPORT_ABUSE_TO_MAIL_HEADER = "X-Report-Abuse-To";

  public static final String X_MAILER = "X-Mailer";


  private final MailServiceSmtpProviderConfig providerConfig;


  /**
   * All clients
   */
  private final Map<String, MailClient> transactionalMailClientsMap = new HashMap<>();


  private Boolean useWiserAsTransactionalClient = false;
  /**
   * A client used in test
   */
  private MailClient wiserMailClient;





  public MailServiceSmtpProvider(MailServiceSmtpProviderConfig providerConfig) {
    this.providerConfig = providerConfig;
    this.ABUSE_EMAIL = providerConfig.adminAddress;
    this.BOUNCE_EMAIL = providerConfig.adminAddress;
  }

  public static MailServiceSmtpProvider get(Vertx vertx) {
    MailServiceSmtpProvider mailPool = smtpPoolMap.get(vertx);
    if (mailPool == null) {
      throw new InternalException("The smtp service should exist");
    }
    return mailPool;
  }

  public static MailServiceSmtpProviderConfig config(Vertx vertx, ConfigAccessor jsonConfig, MailSmtpInfo mailSmtpInfo) {

    MailServiceSmtpProviderConfig MailServiceSmtpProviderConfig = new MailServiceSmtpProviderConfig(vertx);

    /**
     * Data from conf file
     */
    String dkimSelector = jsonConfig.getString(MAIL_DKIM_SELECTOR);
    if (dkimSelector != null) {
      MailServiceSmtpProviderConfig.setDkimSelector(dkimSelector);
      LOGGER.info("Mail: The mail dkim selector (" + MAIL_DKIM_SELECTOR + ") was found with the value " + dkimSelector);
    } else {
      LOGGER.info("Mail: The mail dkim selector (" + MAIL_DKIM_SELECTOR + ") was not found with the value.");
    }
    String dkimPrivateKey = jsonConfig.getString(MAIL_DKIM_PRIVATE);
    if (dkimPrivateKey != null) {
      MailServiceSmtpProviderConfig.setDkimPrivateKey(dkimPrivateKey);
      LOGGER.info("Mail: The mail dkim private key (" + MAIL_DKIM_PRIVATE + ") was found");
    } else {
      LOGGER.info("Mail: A mail dkim private key (" + MAIL_DKIM_PRIVATE + ") was NOT found");
    }


    Integer port = mailSmtpInfo.getPort();
    if (port != null) {
      MailServiceSmtpProviderConfig.defaultSmtpPort = port;
      LOGGER.info("Mail: The mail default port key was set with the value (" + port + ").");
    }

    String hostname = mailSmtpInfo.getHost();
    if (hostname != null) {
      MailServiceSmtpProviderConfig.defaultSmtpHostname = hostname;
      LOGGER.info("Mail: The mail default hostname was set with the value (" + hostname + ").");
    }

    StartTLSOptions startTls = mailSmtpInfo.getStartTlsOption();
    if (startTls != null) {
      MailServiceSmtpProviderConfig.defaultSmtpStartTlsOption = startTls;
      LOGGER.info("Mail: The mail default startTLS was set with the value (" + startTls.toString().toLowerCase() + ").");
    }

    String username = mailSmtpInfo.getUserName();
    if (username != null) {
      MailServiceSmtpProviderConfig.defaultSmtpUserName = username;
      LOGGER.info("Mail: The mail default username was set with the value (" + username + ").");
    }

    String password = mailSmtpInfo.getPassword();
    if (password != null) {
      MailServiceSmtpProviderConfig.defaultSmtpUserPassword = password;
      LOGGER.info("Mail: The mail password was set (xxxxx).");
    }

    BMailInternetAddress adminAddress = mailSmtpInfo.getAdminEmail();
    if(adminAddress!=null){
      MailServiceSmtpProviderConfig.adminAddress = adminAddress;
      LOGGER.info("Mail: The admin email was set to ("+adminAddress+')');
    }

    return MailServiceSmtpProviderConfig;

  }


  public MailClient getTransactionalMailClientForUser(String email) {


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
      MailConfig mailConfig = getMailConfig(senderDomain);
      /**
       * Same as {@link WiserConfiguration#WISER_PORT}
       */
      mailConfig.setPort(1081);
      this.wiserMailClient = MailClient.create(this.providerConfig.vertx, mailConfig);
      return this.wiserMailClient;
    }

    MailClient mailClient = transactionalMailClientsMap.get(senderDomain);
    if (mailClient != null) {
      return mailClient;
    }

    MailConfig config = getMailConfig(senderDomain);
    mailClient = MailClient.create(this.providerConfig.vertx, config);
    transactionalMailClientsMap.put(senderDomain, mailClient);
    return mailClient;

  }

  private MailConfig getMailConfig(String senderDomain) {
    // https://github.com/vert-x3/vertx-mail-client/blob/master/src/test/java/io/vertx/ext/mail/MailWithDKIMSignTest.java
    // https://github.com/gaol/vertx-mail-client/wiki/DKIM-Implementation
    DKIMSignOptions dkimSignOptions = new DKIMSignOptions()
      .setPrivateKey(this.providerConfig.dkimPrivateKey)
      //.setAuid("@example.com") user agent identifier (default @sdid)
      .setSelector(this.providerConfig.dkimSelector)
      .setSdid(senderDomain);

    MailConfig config = new MailConfig()
      .setUserAgent(MAIL_AGENT_NAME)
      .setMaxPoolSize(1)
      .setEnableDKIM(true)
      .setDKIMSignOption(dkimSignOptions);

    /**
     * transactional email server are local for now
     */
    if (this.providerConfig.defaultSmtpHostname != null) {
      config.setHostname(this.providerConfig.defaultSmtpHostname);
    }
    if (this.providerConfig.defaultSmtpPort != null) {
      config.setPort(this.providerConfig.defaultSmtpPort);
    }
    if (this.providerConfig.defaultSmtpStartTlsOption != null) {
      config.setStarttls(this.providerConfig.defaultSmtpStartTlsOption);
    }
    if (this.providerConfig.defaultSmtpUserName != null) {
      config.setUsername(this.providerConfig.defaultSmtpUserName);
    }
    if (this.providerConfig.defaultSmtpUserPassword != null) {
      config.setPassword(this.providerConfig.defaultSmtpUserPassword);
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
      .setBounceAddress(BOUNCE_EMAIL.getAddress())
      .addHeader(ERRORS_TO_MAIL_HEADER, this.getAdminInternetAddress().getAddress())
      .addHeader(X_REPORT_ABUSE_TO_MAIL_HEADER, ABUSE_EMAIL.getAddress())
      .addHeader(X_MAILER, "combostrap.com");
    /**
     * Example
     * X-Mailer: Customer.io (dgTQ1wUDAN7xBd3xBQGGMVPhnYtKqXGPZwNilOc=; +https://whatis.customeriomail.com)
     */


  }

  private BMailInternetAddress getAdminInternetAddress() {
    return this.providerConfig.adminAddress;
  }


  public static class MailServiceSmtpProviderConfig {
    private final Vertx vertx;
    public BMailInternetAddress adminAddress;
    String dkimSelector = DEFAULT_DKIM_SELECTOR;
    String dkimPrivateKey;
    /**
     * the port of the smtp service
     * It's used primarily to set the port of the wiser smtp service
     * during test
     */
    Integer defaultSmtpPort;
    String defaultSmtpHostname;
    StartTLSOptions defaultSmtpStartTlsOption;
    String defaultSmtpUserName;
    String defaultSmtpUserPassword;

    public MailServiceSmtpProviderConfig(Vertx vertx) {
      this.vertx = vertx;
    }

    public MailServiceSmtpProvider create() throws NoSecretException {

      if (this.dkimPrivateKey == null) {
        throw new NoSecretException("A Dkim private key is mandatory to sign email. You can set set it in the conf with the attribute (" + MAIL_DKIM_PRIVATE + ")");
      }

      MailServiceSmtpProvider mailPoolService = new MailServiceSmtpProvider(this);
      smtpPoolMap.put(vertx, mailPoolService);
      return mailPoolService;

    }

    /**
     * @param dkimSelector - the dkim selector used to select the public key (test in test and combo in production)
     * @return the config for chaining
     */
    public MailServiceSmtpProviderConfig setDkimSelector(String dkimSelector) {
      if (dkimSelector == null) {
        throw new InternalException("The dkim selector cannot be null");
      }
      this.dkimSelector = dkimSelector;
      return this;
    }

    public MailServiceSmtpProviderConfig setDkimPrivateKey(String dkimPrivateKey) {
      this.dkimPrivateKey = dkimPrivateKey;
      return this;
    }



  }


}
