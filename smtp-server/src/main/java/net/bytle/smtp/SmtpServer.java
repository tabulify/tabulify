package net.bytle.smtp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.SocketAddress;
import net.bytle.exception.CastException;
import net.bytle.java.JavaEnvs;
import net.bytle.smtp.command.SmtpEhloCommandHandler;
import net.bytle.type.Casts;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytle.email.LoggerEmail.LOGGER;
import static net.bytle.smtp.SmtpSyntax.LOG_TAB;

public class SmtpServer {

  /**
   * The software name is given in the {@link SmtpEhloCommandHandler Ehlo command}
   */
  private final String softwareName;

  private final Map<SmtpSession, SmtpSession> activeSessions = new HashMap<>();
  /**
   * Not fewer than 100
   * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-4.5.3.1.8">Recipients Buffer limits</a>
   */
  private static final int MAXIMUM_RECIPIENT = 100;
  private final int maxTotalConnections;
  private final int maxConnectionBySource;
  /**
   * The maximum number of exception by session
   */
  private final int maximumExceptionCountBySession;
  /**
   * Max Size of a Message
   */
  private final int maxMessageSizeInBytes;
  private final int maxRecipientsByEmail;
  /**
   * {@link SocketAddress} has a hash code making it unique
   * The {@link SmtpSocket#getRemoteAddress()}
   */
  private final ConcurrentMap<SocketAddress, Integer> totalConnectionsByIp = new ConcurrentHashMap<>();

  private final Map<String, SmtpHost> hostedDomains = new HashMap<>();
  private final SmtpHost defaultHostedHost;
  private final int idleTimeoutSecond;
  private final long handShakeTimeoutSecond;
  private final boolean localhostAuthenticationRequired;
  private final Map<String, SmtpDomain> smtpDomains = new HashMap<>();
  private final HashMap<String, SmtpMailbox> mailboxes = new HashMap<>();

  public List<SmtpService> getSmtpServices() {
    return services;
  }

  private final List<SmtpService> services = new ArrayList<>();


  // Session Purge
  Integer DEFAULT_IDLE_TIMEOUT_SECOND = 5 * 60;

  public SmtpServer(AbstractVerticle smtpVerticle, ConfigAccessor configAccessor) throws ConfigIllegalException {


    idleTimeoutSecond = configAccessor.getInteger("idle.session.timeout.second", DEFAULT_IDLE_TIMEOUT_SECOND);
    smtpVerticle.getVertx().setPeriodic(idleTimeoutSecond + 20, this::removeIdleSessions);

    long defaultSslHandshakeTimeout = SSLOptions.DEFAULT_SSL_HANDSHAKE_TIMEOUT;
    if (JavaEnvs.IS_IDE_DEBUGGING) {
      /**
       * One hour: used when debugging {@link net.bytle.smtp.command.SmtpStartTlsCommandHandler STARTTLS}
       * command
       */
      defaultSslHandshakeTimeout = 60 * 60;
    }
    handShakeTimeoutSecond = configAccessor.getLong("handshake.timeout.second", defaultSslHandshakeTimeout);

    /**
     * General conf
     */
    this.softwareName = configAccessor.getString("software.name", "Eraldy");
    LOGGER.info(SmtpSyntax.LOG_TAB + "Software Name set to " + this.softwareName);

    /**
     * Authentication from Localhost is not required by default
     */
    this.localhostAuthenticationRequired = configAccessor.getBoolean("auth.localhost.required", false);

    /**
     * Max, Limit Settings, Quotas
     */
    LOGGER.info(LOG_TAB + "Smtp Max, Limit Settings:");
    this.maxTotalConnections = configAccessor.getInteger("max.sessions", 50);
    LOGGER.info(LOG_TAB + "Max total connections set to " + this.maxTotalConnections);
    this.maxConnectionBySource = configAccessor.getInteger("max.sessions.by.ip", 3);
    LOGGER.info(LOG_TAB + "Max connection count by IP set to " + this.maxConnectionBySource);
    this.maxMessageSizeInBytes = configAccessor.getInteger("max.message.size.bytes", 1024);
    LOGGER.info(SmtpSyntax.LOG_TAB + "Max message size in bytes set to " + this.maxMessageSizeInBytes);
    this.maximumExceptionCountBySession = configAccessor.getInteger("max.exception.count.by.session", 3);
    LOGGER.info(SmtpSyntax.LOG_TAB + "Max exceptions by session set to " + this.maximumExceptionCountBySession);
    this.maxRecipientsByEmail = configAccessor.getInteger("max.recipients", MAXIMUM_RECIPIENT);
    LOGGER.info(SmtpSyntax.LOG_TAB + "Max recipients by email set to " + this.maxRecipientsByEmail);

    /**
     * Host(s) settings
     */
    LOGGER.info("Host(s) Settings:");
    String smtpHostKey = "host";
    String host = configAccessor.getString(smtpHostKey);
    if (host == null) {
      throw new ConfigIllegalException("The host configuration (" + smtpHostKey + ") is mandatory and was not found");
    }
    String hostedDomainsConfKey = "hosts";
    JsonObject hostedDomainsConfiguration = configAccessor.getJsonObject(hostedDomainsConfKey);
    if (hostedDomainsConfiguration == null) {
      throw new ConfigIllegalException("The hosted domains configuration (" + hostedDomainsConfKey + ") is mandatory and was not found");
    }
    for (String virtualHostnameString : hostedDomainsConfiguration.getMap().keySet()) {
      SmtpHost.conf smtpVirtualHostConf = SmtpHost.createOf(virtualHostnameString);
      JsonObject hostedDomainConfigurationData = hostedDomainsConfiguration.getJsonObject(virtualHostnameString);
      String domainName = hostedDomainConfigurationData.getString("domain");
      if (domainName == null) {
        throw new ConfigIllegalException("The domain for the hostname (" + virtualHostnameString + ") is mandatory");
      }
      SmtpDomain smtpDomain = this.getOrCreateDomainByName(domainName);
      smtpVirtualHostConf.setHostedDomain(smtpDomain);
      String postMasterEmailConf = hostedDomainConfigurationData.getString("postmaster");
      if (postMasterEmailConf == null) {
        throw new ConfigIllegalException("The postmaster email for the domain (" + virtualHostnameString + ") is mandatory");
      }
      smtpVirtualHostConf.setPostmasterEmail(postMasterEmailConf);

      SmtpHost smtpHost = smtpVirtualHostConf.build();

      this.hostedDomains.put(virtualHostnameString, smtpHost);
      LOGGER.info(LOG_TAB + "Virtual Host added: " + smtpHost.getHostedHostname() + " (Domain: " + smtpHost.getDomain() + ", Postmaster: " + smtpHost.getPostmaster().getPostmasterAddressInConfiguration() + ")");
    }
    this.defaultHostedHost = this.hostedDomains.get(host);
    if (this.defaultHostedHost == null) {
      throw new ConfigIllegalException("The main host (" + host + ") was not found in the hosts. It is mandatory");
    }

    /**
     * Smtp Services
     */
    String servicesConfKey = "services";
    JsonObject servicesConfiguration = configAccessor.getJsonObject(servicesConfKey);
    if (servicesConfiguration == null) {
      throw new ConfigIllegalException("The service configuration (" + servicesConfKey + ") is mandatory and was not found");
    }
    for (String serviceKey : servicesConfiguration.getMap().keySet()) {
      Integer servicePort;
      try {
        servicePort = Casts.cast(serviceKey, Integer.class);
      } catch (CastException e) {
        throw new ConfigIllegalException("The key name of a service should be a port number. " + serviceKey + " is not an integer", e);
      }
      SmtpService smtpService = new SmtpService(this, servicePort, configAccessor.getSubConfigAccessor(servicesConfKey, serviceKey));
      this.services.add(smtpService);
      LOGGER.info(LOG_TAB + "Service added: " + smtpService);
    }

    /**
     * Create the mailboxes
     */
    SmtpMailboxStdout defaultStdOutMailBox = new SmtpMailboxStdout();
    this.mailboxes.put(defaultStdOutMailBox.getName(), defaultStdOutMailBox);
    SmtpMailboxForward forwardMailbox = new SmtpMailboxForward();
    this.mailboxes.put(forwardMailbox.getName(), forwardMailbox);

    /**
     * Define the users
     * Smtp Users
     */
    String usersConfKey = "users";
    JsonObject usersConfiguration = configAccessor.getJsonObject(usersConfKey);
    if (usersConfiguration == null) {
      throw new ConfigIllegalException("The users configuration (" + usersConfKey + ") is mandatory and was not found");
    }
    for (String domain : usersConfiguration.getMap().keySet()) {

      SmtpDomain smtpDomain = this.smtpDomains.get(domain.toLowerCase());
      if (smtpDomain == null) {
        throw new ConfigIllegalException("The users domain  (" + domain + ") was not found in the hosts");
      }
      JsonObject users = usersConfiguration.getJsonObject(domain);
      for (String userName : users.getMap().keySet()) {

        ConfigAccessor userConfigAccessor = configAccessor.getSubConfigAccessor(usersConfKey, domain, userName);
        String mailBoxString = userConfigAccessor.getString("mailbox");
        SmtpMailbox smtpMailbox;
        if (mailBoxString == null) {
          smtpMailbox = defaultStdOutMailBox;
        } else {
          smtpMailbox = this.mailboxes.get(mailBoxString.toLowerCase());
          if (smtpMailbox == null) {
            throw new ConfigIllegalException("The mailbox (" + mailBoxString + ") of the user (" + userName + ")does not exist");
          }
        }
        String password = userConfigAccessor.getString("password");
        SmtpUser smtpUser = SmtpUser.createFrom(smtpDomain, userName, smtpMailbox, password);
        smtpDomain.addUser(smtpUser);
        LOGGER.info(LOG_TAB + "User added: " + smtpUser);
      }


    }

  }

  private SmtpDomain getOrCreateDomainByName(String domainName) {
    String domainNameNormalization = domainName.toLowerCase();
    SmtpDomain smtpDomain = this.smtpDomains.get(domainNameNormalization);
    if (smtpDomain == null) {
      smtpDomain = SmtpDomain.createFromName(domainName);
      this.smtpDomains.put(domainNameNormalization, smtpDomain);
    }
    return smtpDomain;
  }

  public static SmtpServer create(AbstractVerticle smtpVerticle, ConfigAccessor configAccessor) throws ConfigIllegalException {
    return new SmtpServer(smtpVerticle, configAccessor);
  }

  /**
   * See timeout by phase:
   * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.5.3.2">Timeout</a>
   */
  protected void removeIdleSessions(Long aLong) {

    LocalDateTime now = LocalDateTime.now();
    LOGGER.fine("There is " + activeSessions.size() + " session");
    if (!JavaEnvs.IS_IDE_DEBUGGING) {
      for (SmtpSession smtpSession : activeSessions.values()) {
        LocalDateTime deadlineTime = smtpSession.getLastInteractiveTime().plusSeconds(this.idleTimeoutSecond);
        if (deadlineTime.isBefore(now)) {
          smtpSession.closeSessionWithReply(SmtpReply.create(SmtpReplyCode.SERVICE_NOT_AVAILABLE_421, "Connection was idle for more than " + this.idleTimeoutSecond + " seconds. Bye."));
        }
      }
    }
  }

  void removeSession(SmtpSession smtpSession) {
    this.activeSessions.remove(smtpSession);
    SocketAddress source = smtpSession.getSmtpSocket().getRemoteAddress();
    Integer connectionNumber = this.totalConnectionsByIp.getOrDefault(source, 0);
    if (connectionNumber.equals(1)) {
      this.totalConnectionsByIp.remove(source);
    } else {
      this.totalConnectionsByIp.put(source, connectionNumber - 1);
    }
  }

  public boolean tooMuchConnection() {
    return activeSessions.size() > maxTotalConnections;
  }

  /**
   * @param smtpSession - the connection to add
   * @throws SmtpException - an exception if there is too much connection
   */
  public void connectionRateLimiter(SmtpSession smtpSession) throws SmtpException {

    if (tooMuchConnection()) {
      throw SmtpException.create(SmtpReplyCode.SERVICE_NOT_AVAILABLE_421, "Too many connections on the server, try again later")
        .setShouldQuit(true);
    }

    Integer maxBySource = this.totalConnectionsByIp.getOrDefault(smtpSession.getSmtpSocket().getRemoteAddress(), 0);
    if (maxBySource > maxConnectionBySource) {
      throw SmtpException.create(SmtpReplyCode.SERVICE_NOT_AVAILABLE_421, "Too many connections for your ip, try again later")
        .setShouldQuit(true);
    }
    this.totalConnectionsByIp.put(smtpSession.getSmtpSocket().getRemoteAddress(), maxBySource + 1);
    activeSessions.put(smtpSession, smtpSession);

  }

  public Map<String, SmtpHost> getHostedHosts() {
    return this.hostedDomains;
  }


  public SmtpHost getDefaultHostedHost() {
    return this.defaultHostedHost;
  }

  public int getIdleTimeoutSecond() {
    return this.idleTimeoutSecond;
  }

  public String getSoftwareName() {
    return this.softwareName;
  }

  public int maximumExceptionBySession() {
    return this.maximumExceptionCountBySession;
  }

  public int getMaxMessageSizeInBytes() {
    return this.maxMessageSizeInBytes;
  }

  public int getMaxRecipientsByEmail() {
    return this.maxRecipientsByEmail;
  }

  public long getHandShakeTimeoutSecond() {
    return this.handShakeTimeoutSecond;
  }

  public boolean getLocalHostAuthenticationRequired() {
    return localhostAuthenticationRequired;
  }


}
