package net.bytle.smtp;

import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import net.bytle.exception.NotFoundException;
import net.bytle.java.JavaEnvs;

/**
 * A class to greet and hold connection information
 * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-3.1">Session Initiation</a>
 * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-4.3.1">Sequencing Overview</a>
 */
public class SmtpGreeting implements SmtpSessionInteraction {


  private final SmtpSession smtpSession;
  private String sslprotocol;
  private SmtpHost requestedHost;
  private SocketAddress clientAddress;

  public SmtpGreeting(SmtpSession smtpSession) {
    this.smtpSession = smtpSession;
  }

  public static SmtpGreeting create(SmtpSession smtpSession) {
    return new SmtpGreeting(smtpSession);
  }

  public void updateRequestedHost() throws SmtpException {
    try {
      this.requestedHost = this.determineRequestedHost();
    } catch (NotFoundException e) {
      if(this.smtpSession.getSmtpSocket().getNetSocket().isSsl()){
        throw SmtpException.create(SmtpReplyCode.SSL_REQUIRED_538,"SNI is required with SSL. We couldn't find the hostname");
      }
    }
  }

  /**
   * Determine the Requested host
   * <p>
   */
  private SmtpHost determineRequestedHost() throws NotFoundException, SmtpException {


    String indicatedServerName = this.smtpSession.getSmtpSocket().getIndicatedServerName();


    SmtpHost smtpHost = this.smtpSession.getSmtpService().getSmtpServer().getHostedHosts().get(indicatedServerName);
    if (smtpHost != null) {
      return smtpHost;
    }
    SmtpException smtpException = SmtpException.create(SmtpReplyCode.CONNECTION_WITH_BAD_HOSTNAME_899, "The requested hostname (" + indicatedServerName + ") is unknown")
      .setShouldQuit(true);
    if (!JavaEnvs.IS_DEV) {
      smtpException.setShouldBeSilentQuit(true);
    }
    throw smtpException;


  }


  /**
   * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-3.1">...</a>
   * We may answer with:
   * * {@link SmtpReplyCode#GREETING_220}
   * * {@link SmtpReplyCode#TRANSACTION_FAILED_554}
   */
  public SmtpReply greet() throws SmtpException {

    /**
     * Check IP
     */
    SmtpFiltering.checkIp(this.smtpSession);


    /**
     * Determine, set the requested host
     */
    this.updateRequestedHost();


    SmtpSocket smtpSocket = this.smtpSession.getSmtpSocket();
    NetSocket netSocket = smtpSocket.getNetSocket();
    if(!netSocket.isSsl()) {
      this.sslprotocol = "plain";
    } else {
      this.sslprotocol = netSocket.sslSession().getProtocol();
    }
    this.clientAddress = smtpSocket.getRemoteAddress();


    this.smtpSession.getSessionHistory().addInteraction(this);

    /**
     * Greetings
     */
    return SmtpReply.create(
      SmtpReplyCode.GREETING_220,
      this.getRequestedHostOrDefault().getHostedHostname() + " " +
        this.smtpSession.getSmtpProtocol() + " " +
        this.smtpSession.getSmtpService().getSmtpServer().getSoftwareName()
    );
  }

  @Override
  public String getSessionHistoryLine() {
    return "Connection to " + this.getRequestedHostOrDefault().getHostedHostname() + " in " + this.sslprotocol + " with " + this.clientAddress + SmtpSyntax.LINE_DELIMITER;
  }

  /**
   * Without TLS, we don't know the server requested
   */
  public SmtpHost getRequestedHostOrDefault() {

    try {
      return getRequestedHost();
    } catch (NotFoundException e) {
      return this.smtpSession.getSmtpService().getSmtpServer().getDefaultHostedHost();
    }

  }

  /**
   * Without TLS, we don't know the server requested
   */
  public SmtpHost getRequestedHost() throws NotFoundException {
    if(this.requestedHost==null){
      throw new NotFoundException();
    }
    return this.requestedHost;
  }

  /**
   * This function return:
   * * the default hostname if we don't know the requested hostname
   * * the domain if we know the requested hostname
   * Why?
   * In the documentation, the hostname of the server
   * is given in the greeting and the domain is given afterward.
   * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.1.1">Domain in EHLO</a>
   * and Domain in QUIT
   * <p>
   * Hosting: in case you host multiple, it's completely futile.
   * You can't advertise more than one domain or hosts.
   * <p>
   * Example: With Smtp In Server from SES
   * * the hosted domain is: inbox.mailbrew.com
   * * the MX is: inbound-smtp.us-east-1.amazonaws.com
   * On this MX, a connection gives:
   * 250-inbound-smtp.us-east-1.amazonaws.com
   * 250-8BITMIME
   * 250-STARTTLS
   * 250 Ok
   */
  public String getDomainOrHostName() {
    try {
      return this.getRequestedHost().getDomain().toString();
    } catch (NotFoundException e) {
      return this.getRequestedHostOrDefault().getHostedHostname();
    }
  }
}
