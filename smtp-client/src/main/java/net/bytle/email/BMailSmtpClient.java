package net.bytle.email;

import jakarta.mail.*;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.Booleans;
import net.bytle.type.UriEnhanced;

import java.net.URI;
import java.util.List;
import java.util.Properties;

import static net.bytle.email.BMailSmtpConnectionAttribute.PROTOCOL;

/**
 * Adapted from the sample smtpsend.java
 * <p>
 * This code wraps a {@link Session}
 * and the  {@link Transport}
 * <p>
 * Wrapper around d Session object that authenticates the user, and controls access to the
 * message store and transport.
 * The SMTP configuration
 */
public class BMailSmtpClient {


  private static final String MAIL_PROPERTY_PREFIX = "mail";
  private static final String MAIL_SMTP_PROPERTY_PREFIX = MAIL_PROPERTY_PREFIX + "." + BMailSmtpProtocol.SMTP;

  private static final String MAIL_SMTPS_PROPERTY_PREFIX = MAIL_PROPERTY_PREFIX + "." + BMailSmtpProtocol.SMTPS;
  private static final int PORT_465 = 465;
  private final Session smtpSession;
  private final BMailSmtpClient.config config;


  public BMailSmtpClient(config config) {
    this.config = config;
    Properties mailProps = this.getSmtpTransportProperties();
    Authenticator authenticator = null;
    if (config.username != null) {

      /**
       * Transmit the username, password
       * It must be given explicitly in order
       * to use the static function {@link Transport#send(Message)}
       * Note that it's also possible to pass them directly (not tested) {@link Transport#connect(String, String, String)}  }
       * See  https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
       */
      if (config.password == null) {
        throw new RuntimeException("Smtp Authentication is required but the password is null");
      }
      authenticator = new jakarta.mail.Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(config.username, config.password);
        }
      };
    }

    this.smtpSession = Session.getInstance(mailProps, authenticator);
    if (config.debugLogging) {
      this.smtpSession.setDebug(true);
    }

  }

  public static BMailSmtpClient.config create() {
    return new config();
  }

  public static BMailSmtpClient.config createFrom(BMailSmtpConnectionParameters smtpConnectionParameters) {
    return new config()
      .setHost(smtpConnectionParameters.getHost())
      .setPort(smtpConnectionParameters.getPort())
      .setStartTls(smtpConnectionParameters.getStartTlsOption())
      .setUsername(smtpConnectionParameters.getUserName())
      .setPassword(smtpConnectionParameters.getPassword())
      .setSenderEmail(smtpConnectionParameters.getSender())
      ;
  }


  /**
   * For all SMTP properties
   * <a href="https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html">...</a>
   * <p>
   * For others:
   * see <a href="https://javaee.github.io/javamail/docs/api/overview-summary.html">...</a>
   */
  public Properties getSmtpTransportProperties() {

    /**
     * The config properties
     */
    Properties smtpServerProps = new Properties();

    /**
     * Mail global config
     */
    if (config.debugLogging) {
      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".debug", true);
      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".debug.auth", true);
      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".debug.auth.username", true);
      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".debug.auth.password", true);
    }

    /**
     * Config by Protocol
     */
    String smtpProtocolConfigurationPrefix;
    if (this.config.isDirectTlsConnection) {

      // mail.transport.protocol specifies the default message transport protocol
      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".transport.protocol", BMailSmtpProtocol.SMTPS.toString());

      smtpProtocolConfigurationPrefix = MAIL_SMTPS_PROPERTY_PREFIX;
      /**
       * The {@link BMailSmtpConnection} use the {@link Transport#send(Message)} that
       * will use the default transport protocol, which remains "smtp" ie {@link com.sun.mail.smtp.SMTPTransport}
       * To enable SMTP connections over SSL, set the "mail.smtp.ssl.enable" property to "true".
       * It will use the {@link com.sun.mail.smtp.SMTPSSLTransport}
       */
      smtpServerProps.put(MAIL_SMTP_PROPERTY_PREFIX + ".ssl.enable", true);

    } else {

      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".transport.protocol", BMailSmtpProtocol.SMTP.toString());
      smtpProtocolConfigurationPrefix = MAIL_SMTP_PROPERTY_PREFIX;

    }

    // smtpServerProps.put(smtpProtocolConfigurationPrefix +".connectiontimeout", "");
    // smtpServerProps.put(smtpProtocolConfigurationPrefix +".timeout", "");
    // smtpServerProps.put(smtpProtocolConfigurationPrefix +".writetimeout", "");
    smtpServerProps.put(smtpProtocolConfigurationPrefix + ".quitwait", "false");


    smtpServerProps.put(smtpProtocolConfigurationPrefix + "." + BMailSmtpConnectionAttribute.HOST, config.smtpHost);
    smtpServerProps.put(smtpProtocolConfigurationPrefix + "." + BMailSmtpConnectionAttribute.PORT, config.port);


    if (config.username != null) {
      smtpServerProps.put(smtpProtocolConfigurationPrefix + "." + BMailSmtpConnectionAttribute.AUTH, true);
      smtpServerProps.put(smtpProtocolConfigurationPrefix + "." + BMailSmtpConnectionAttribute.USER, config.username);
    }

    /**
     * May be null for direct SSL/TLS connection
     */
    if(config.startTls!=null) {
      switch (config.startTls) {
        case NONE:
          smtpServerProps.put(smtpProtocolConfigurationPrefix + ".starttls.enable", "false");
          break;
        case ENABLE:
          smtpServerProps.put(smtpProtocolConfigurationPrefix + ".starttls.enable", "true");
          break;
        case REQUIRE:
          // STARTTLS plaintext fallback is disabled by setting mail.smtp.starttls.required
          smtpServerProps.put(smtpProtocolConfigurationPrefix + ".starttls.enable", "true");
          smtpServerProps.put(smtpProtocolConfigurationPrefix + ".starttls.required", "true");
      }
    }

    if (config.trustAll) {

      smtpServerProps.put(smtpProtocolConfigurationPrefix + ".ssl.trust", "*");
      smtpServerProps.put(smtpProtocolConfigurationPrefix + ".ssl.checkserveridentity", "false");

    }
    if (config.chunkSize != null) {
      smtpServerProps.put(smtpProtocolConfigurationPrefix + ".chunksize", config.chunkSize);
    }


    /**
     * Envelope: MAIL FROM
     * mail.smtp.from sets the envelope return address.
     * Defaults to msg.getFrom() or InternetAddress.getLocalAddress().
     */
    BMailInternetAddress bounceAddress = this.getSenderAddress();
    if (bounceAddress != null) {
      smtpServerProps.put(smtpProtocolConfigurationPrefix + ".from", bounceAddress.getAddress());
    }
    return smtpServerProps;

  }


  public BMailSmtpConnection getTransportConnection() throws MessagingException {
    return new BMailSmtpConnection(this);
  }


  public URI toUri() {
    try {
      return UriEnhanced
        .create()
        .setScheme((String) PROTOCOL.getDefaultValue())
        .setHost(config.smtpHost)
        .setPort(config.port)
        .addQueryProperty(BMailSmtpConnectionAttribute.USER, config.username)
        .addQueryProperty(BMailSmtpConnectionAttribute.PASSWORD, config.password == null ? "null" : "xxxx")
        .addQueryProperty(BMailSmtpConnectionAttribute.TLS, Booleans.createFromObject(config.startTls).toString())
        .toUri();
    } catch (IllegalStructure e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public String toString() {
    return toUri().toString();
  }

  public Boolean ping() {
    try {
      Transport transport = smtpSession.getTransport();
      try {
        transport.connect();
        transport.close();
      } catch (MessagingException e) {
        throw new RuntimeException("Unable to connect. Error: " + e.getMessage());
      }
      return true;
    } catch (NoSuchProviderException e) {
      throw new RuntimeException(e);
    }
  }

  protected Session getSession() {
    return this.smtpSession;
  }


  public BMailInternetAddress getSenderAddress() {
    return this.config.sender;
  }


  /**
   * Send a message with the recipients address of the message
   */
  public void sendMessage(BMailMimeMessage message) throws MessagingException {
    sendMessage(message, message.toMimeMessage().getAllRecipients());
  }

  public void sendMessage(BMailMimeMessage message, Address[] recipients) throws MessagingException {
    try (BMailSmtpConnection transportConnection = this.getTransportConnection()) {
      transportConnection.sendMessage(message, recipients);
    }
  }

  @SuppressWarnings("unused")
  public void sendMessagesInBatch(List<BMailMimeMessage> messages) throws MessagingException {
    try (BMailSmtpConnection transportConnection = this.getTransportConnection()) {
      for (BMailMimeMessage message : messages) {
        transportConnection.sendMessage(message, message.toMimeMessage().getAllRecipients());
      }
    }
  }

  public boolean isDirectTlsConnection() {
    return this.config.isDirectTlsConnection;
  }


  public static class config {

    public Boolean isDirectTlsConnection = null;
    int port = 25;
    String smtpHost = "localhost";
    String username;
    String password;
    BMailStartTls startTls = null;
    boolean debugLogging;
    Integer sessionTimeout;
    Integer chunkSize;
    Boolean trustAll = false;
    private BMailInternetAddress sender;

    public config() {

    }

    public config setHost(String hostname) {
      if (hostname == null) {
        // don't overwrite the localhost default
        return this;
      }
      this.smtpHost = hostname;
      return this;
    }

    public config setPort(int port) {
      this.port = port;
      return this;
    }

    public config setUsername(String username) {
      this.username = username;
      return this;
    }

    public config isSslConnection(boolean isSsl) {
      this.isDirectTlsConnection = isSsl;
      return this;
    }

    public config setPassword(String password) {
      this.password = password;
      return this;
    }

    public config setTrustAll(Boolean trustAll) {
      this.trustAll = trustAll;
      return this;
    }

    public config setStartTls(BMailStartTls startTls) {
      if (startTls == null) {
        // don't overwrite the default
        return this;
      }
      this.startTls = startTls;
      return this;
    }


    public config setDebug(boolean b) {
      this.debugLogging = b;
      return this;
    }

    public config setSessionTimeout(int timeout) {
      this.sessionTimeout = timeout;
      return this;
    }

    public config setWithChunkingInSize(int sizeInBytes) {
      this.chunkSize = sizeInBytes;
      return this;
    }

    public BMailSmtpClient build() {

      /**
       * Default SSL by port
       */
      if (this.isDirectTlsConnection == null) {
        this.isDirectTlsConnection = (this.port == PORT_465);
      }
      /**
       * Default StartTls by port
       */
      if (this.startTls == null) {
        if (!isDirectTlsConnection) {
          this.startTls = BMailStartTls.ENABLE;
        }
      }

      return new BMailSmtpClient(this);


    }

    public BMailSmtpClient.config setSenderEmail(BMailInternetAddress sender) {
      this.sender = sender;
      return this;
    }
  }
}
