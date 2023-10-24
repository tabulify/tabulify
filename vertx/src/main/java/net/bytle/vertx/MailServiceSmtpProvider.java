package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.*;
import net.bytle.email.*;
import net.bytle.exception.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.ext.mail.StartTLSOptions.OPTIONAL;
import static io.vertx.ext.mail.StartTLSOptions.REQUIRED;
import static net.bytle.email.BMailMimeMessageHeader.X_REPORT_ABUSE_TO;

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
  }

  public static MailServiceSmtpProvider get(Vertx vertx) {
    MailServiceSmtpProvider mailPool = smtpPoolMap.get(vertx);
    if (mailPool == null) {
      throw new InternalException("The smtp service should exist");
    }
    return mailPool;
  }

  public static MailServiceSmtpProviderConfig config(Vertx vertx, ConfigAccessor jsonConfig, BMailSmtpConnectionParameters mailSmtpConfig) {

    MailServiceSmtpProviderConfig mailServiceSmtpProviderConfig = new MailServiceSmtpProviderConfig(vertx);

    /**
     * Data from conf file
     */
    String dkimSelector = jsonConfig.getString(MAIL_DKIM_SELECTOR);
    if (dkimSelector != null) {
      mailServiceSmtpProviderConfig.setDkimSelector(dkimSelector);
      LOGGER.info("Mail: The mail dkim selector (" + MAIL_DKIM_SELECTOR + ") was found with the value " + dkimSelector);
    } else {
      LOGGER.warn("Mail: The mail dkim selector (" + MAIL_DKIM_SELECTOR + ") was not found with the value.");
    }
    String dkimPrivateKey = jsonConfig.getString(MAIL_DKIM_PRIVATE);
    if (dkimPrivateKey != null) {
      mailServiceSmtpProviderConfig.setDkimPrivateKey(dkimPrivateKey);
      LOGGER.info("Mail: The mail dkim private key (" + MAIL_DKIM_PRIVATE + ") was found");
    } else {
      LOGGER.warn("Mail: A mail dkim private key (" + MAIL_DKIM_PRIVATE + ") was NOT found");
    }

    mailServiceSmtpProviderConfig.smtpConnectionParameters = mailSmtpConfig;

    Integer port = mailSmtpConfig.getPort();
    if (port != null) {
      LOGGER.info("Mail: The mail default port key was set with the value (" + port + ").");
    }

    String hostname = mailSmtpConfig.getHost();
    if (hostname != null) {
      LOGGER.info("Mail: The mail default hostname was set with the value (" + hostname + ").");
    }

    BMailStartTls startTls = mailSmtpConfig.getStartTlsOption();
    if (startTls != null) {
      LOGGER.info("Mail: The mail default startTLS was set with the value (" + startTls.toString().toLowerCase() + ").");
    }

    String username = mailSmtpConfig.getUserName();
    if (username != null) {
      LOGGER.info("Mail: The mail default username was set with the value (" + username + ").");
    }

    String password = mailSmtpConfig.getPassword();
    if (password != null) {
      LOGGER.info("Mail: The mail password was set (xxxxx).");
    }

    BMailInternetAddress adminAddress = mailSmtpConfig.getSender();
    if (adminAddress != null) {
      LOGGER.info("Mail: The sender email was set to (" + adminAddress + ')');
    }

    return mailServiceSmtpProviderConfig;

  }


  public MailClient getVertxMailClientForSenderWithSigning(String senderDomain) {


    if (useWiserAsTransactionalClient) {
      if (this.wiserMailClient != null) {
        return this.wiserMailClient;
      }
      MailConfig mailConfig = getMailConfigWithDkim(senderDomain);
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

    MailConfig config = getMailConfigWithDkim(senderDomain);
    mailClient = MailClient.create(this.providerConfig.vertx, config);
    transactionalMailClientsMap.put(senderDomain, mailClient);
    return mailClient;

  }

  private MailConfig getMailConfigWithDkim(String senderDomain) {
    // https://github.com/vert-x3/vertx-mail-client/blob/master/src/test/java/io/vertx/ext/mail/MailWithDKIMSignTest.java
    // https://github.com/gaol/vertx-mail-client/wiki/DKIM-Implementation
    DKIMSignOptions dkimSignOptions = new DKIMSignOptions()
      .setPrivateKey(this.providerConfig.dkimPrivateKey)
      //.setAuid("@example.com") user agent identifier (default @sdid)
      .setSelector(this.providerConfig.dkimSelector)
      .setSdid(senderDomain);

    MailConfig vertxMailConfig = new MailConfig()
      .setUserAgent(MAIL_AGENT_NAME)
      .setMaxPoolSize(1)
      .setEnableDKIM(true)
      .setDKIMSignOption(dkimSignOptions);

    /**
     * transactional email server are local for now
     */
    String host = this.providerConfig.smtpConnectionParameters.getHost();
    if (host != null) {
      vertxMailConfig.setHostname(host);
    }
    Integer port = this.providerConfig.smtpConnectionParameters.getPort();
    if (port != null) {
      vertxMailConfig.setPort(port);
    }
    BMailStartTls startTlsOption = this.providerConfig.smtpConnectionParameters.getStartTlsOption();
    if (startTlsOption != null) {
      StartTLSOptions mailClientStartTlsValue;
      switch (startTlsOption) {
        case NONE:
          mailClientStartTlsValue = StartTLSOptions.DISABLED;
          break;
        case ENABLE:
          mailClientStartTlsValue = OPTIONAL;
          break;
        case REQUIRE:
          mailClientStartTlsValue = REQUIRED;
          break;
        default:
          throw new InternalException("Should not happen");
      }
      vertxMailConfig.setStarttls(mailClientStartTlsValue);
    }
    String userName = this.providerConfig.smtpConnectionParameters.getUserName();
    if (userName != null) {
      vertxMailConfig.setUsername(userName);
    }
    String password = this.providerConfig.smtpConnectionParameters.getPassword();
    if (password != null) {
      vertxMailConfig.setPassword(password);
    }
    // MailClient.createShared(vertx, config, "poolName");
    return vertxMailConfig;
  }

  public MailServiceSmtpProvider useWiserSmtpServerAsSmtpDestination(Boolean b) {
    this.useWiserAsTransactionalClient = b;
    return this;
  }

  public MailMessage createVertxMailMessage() {

    MailMessage mailMessage = new MailMessage();
    BMailInternetAddress sender = this.providerConfig.smtpConnectionParameters.getSender();
    if (sender != null) {
      mailMessage.setBounceAddress(sender.getAddress());
    }
    return mailMessage;

  }


  public BMailMimeMessage.builder createBMailMessage() {
    BMailInternetAddress sender = this.providerConfig.smtpConnectionParameters.getSender();
    BMailMimeMessage.builder mimeMessage = BMailMimeMessage.createFromBuilder();
    if (sender != null) {
      mimeMessage
        .addHeader(BMailMimeMessageHeader.ERRORS_TO, sender.getAddress())
        .addHeader(X_REPORT_ABUSE_TO, sender.getAddress());
    }
    return mimeMessage;
  }

  public BMailSmtpClient getBMailClient() {

    return BMailSmtpClient
      .createFrom(this.providerConfig.smtpConnectionParameters)
      .build();

  }


  public static class MailServiceSmtpProviderConfig {
    private final Vertx vertx;
    public BMailSmtpConnectionParameters smtpConnectionParameters;
    String dkimSelector = DEFAULT_DKIM_SELECTOR;
    String dkimPrivateKey;

    public MailServiceSmtpProviderConfig(Vertx vertx) {
      this.vertx = vertx;
    }

    public MailServiceSmtpProvider create() {

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
