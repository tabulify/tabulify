package net.bytle.email;

import jakarta.mail.*;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.Booleans;
import net.bytle.type.UriEnhanced;

import java.net.URI;
import java.util.List;
import java.util.Properties;

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

  private static final String SMTP_PROTOCOL = "smtp";
  private static final String SMTPS_PROTOCOL = "smtps";
  private static final String MAIL_PROPERTY_PREFIX = "mail";
  private static final String MAIL_SMTP_PROPERTY_PREFIX = MAIL_PROPERTY_PREFIX + "." + SMTP_PROTOCOL;

  private static final String MAIL_SMTPS_PROPERTY_PREFIX = MAIL_PROPERTY_PREFIX + "." + SMTPS_PROTOCOL;
  private final Session smtpSession;
  private final BMailSmtpClient.config config;

  private BMailInternetAddress bounceAddress;


  public BMailSmtpClient(config config) {
    this.config = config;
    Properties mailProps = this.getSmtpTransportProperties();
    Authenticator authenticator = null;
    if (config.auth) {

      /**
       * Transmit the username, password
       * It must be given explicitly in order
       * to use the static function {@link javax.mail.Transport#send(Message)}
       *
       * See  https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
       */
      if (config.username == null) {
        throw new RuntimeException("Smtp Authentication is required but the username is null");
      }
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
    if (this.isSSL()) {

      // mail.transport.protocol specifies the default message transport protocol
      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".transport.protocol", SMTPS_PROTOCOL);

      smtpProtocolConfigurationPrefix = MAIL_SMTPS_PROPERTY_PREFIX;
      /**
       * The {@link BMailTransportConnection} use the {@link Transport#send(Message)} that
       * will use the default transport protocol, which remains "smtp" ie {@link com.sun.mail.smtp.SMTPTransport}
       * To enable SMTP connections over SSL, set the "mail.smtp.ssl.enable" property to "true".
       * It will use the {@link com.sun.mail.smtp.SMTPSSLTransport}
       */
      smtpServerProps.put(MAIL_SMTP_PROPERTY_PREFIX + ".ssl.enable", true);

    } else {

      smtpServerProps.put(MAIL_PROPERTY_PREFIX + ".transport.protocol", SMTP_PROTOCOL);
      smtpProtocolConfigurationPrefix = MAIL_SMTP_PROPERTY_PREFIX;

    }

    // smtpServerProps.put(smtpProtocolConfigurationPrefix +".connectiontimeout", "");
    // smtpServerProps.put(smtpProtocolConfigurationPrefix +".timeout", "");
    // smtpServerProps.put(smtpProtocolConfigurationPrefix +".writetimeout", "");
    smtpServerProps.put(smtpProtocolConfigurationPrefix + ".quitwait", "false");

    smtpServerProps.put(smtpProtocolConfigurationPrefix + ".host", config.smtpHost);
    smtpServerProps.put(smtpProtocolConfigurationPrefix + ".port", config.port);

    if (config.auth) {
      smtpServerProps.put(smtpProtocolConfigurationPrefix + ".auth", true);
    }

    if (config.username != null) {
      smtpServerProps.put(smtpProtocolConfigurationPrefix + ".user", config.username);
    }

    switch (config.requireStartTls) {
      case ENABLE:
        smtpServerProps.put(smtpProtocolConfigurationPrefix + ".starttls.enable", "true");
        break;
      case REQUIRE:
        // STARTTLS plaintext fallback is disabled by setting mail.smtp.starttls.required
        smtpServerProps.put(smtpProtocolConfigurationPrefix + ".starttls.required", "true");
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
    BMailInternetAddress bounceAddress = this.getBounceAddress();
    if (bounceAddress != null) {
      smtpServerProps.put(smtpProtocolConfigurationPrefix + ".from", bounceAddress.getAddress());
    }
    return smtpServerProps;

  }


  public BMailTransportConnection getTransportConnection() throws MessagingException {
    return new BMailTransportConnection(this);
  }


  public URI toUri() {
    try {
      return UriEnhanced
        .create()
        .setScheme((String) SmtpConnectionAttribute.SMTP.getDefaultValue())
        .setHost(config.smtpHost)
        .setPort(config.port)
        .addQueryProperty(SmtpConnectionAttribute.USER, config.username)
        .addQueryProperty(SmtpConnectionAttribute.PASSWORD, config.password == null ? "null" : "xxxx")
        .addQueryProperty(SmtpConnectionAttribute.AUTH, Booleans.createFromObject(config.auth).toString())
        .addQueryProperty(SmtpConnectionAttribute.TLS, Booleans.createFromObject(config.requireStartTls).toString())
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

  public boolean isSSL() {
    return this.config.isSsl;
  }

  public BMailInternetAddress getBounceAddress() {
    return this.bounceAddress;
  }

  @SuppressWarnings("unused")
  public void setBounceAddress(BMailInternetAddress bounceAddress) {
    this.bounceAddress = bounceAddress;
  }

  /**
   * Send a message with the recipients address of the message
   */
  public void sendMessage(BMailMimeMessage message) throws MessagingException {
    sendMessage(message, message.toMimeMessage().getAllRecipients());
  }

  public void sendMessage(BMailMimeMessage message, Address[] recipients) throws MessagingException {
    try (BMailTransportConnection transportConnection = this.getTransportConnection()) {
      transportConnection.sendMessage(message, recipients);
    }
  }

  @SuppressWarnings("unused")
  public void sendMessagesInBatch(List<BMailMimeMessage> messages) throws MessagingException {
    try (BMailTransportConnection transportConnection = this.getTransportConnection()) {
      for (BMailMimeMessage message : messages) {
        transportConnection.sendMessage(message, message.toMimeMessage().getAllRecipients());
      }
    }
  }


  public static class config {

    public boolean isSsl = false;
    Integer port = 25;
    String smtpHost = "localhost";
    String username;
    String password;
    Boolean auth = false;
    BMailStartTls requireStartTls = BMailStartTls.NONE;
    boolean debugLogging;
    Integer sessionTimeout;
    Integer chunkSize;
    Boolean trustAll = false;

    public config() {

    }

    public config setHost(String hostname) {
      this.smtpHost = hostname;
      return this;
    }

    public config setPort(int port) {
      this.port = port;
      return this;
    }

    public config setUsername(String username) {
      this.username = username;
      this.auth = true;
      return this;
    }

    public config isSsl(boolean isSsl) {
      this.isSsl = isSsl;
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

    public config setAuth(Boolean auth) {
      this.auth = auth;
      return this;
    }

    public config setStartTls(BMailStartTls startTls) {
      this.requireStartTls = startTls;
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


      return new BMailSmtpClient(this);


    }


  }
}
