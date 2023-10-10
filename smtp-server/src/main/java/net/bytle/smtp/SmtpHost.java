package net.bytle.smtp;

import jakarta.mail.internet.AddressException;
import net.bytle.smtp.command.SmtpEhloCommandHandler;
import net.bytle.smtp.command.SmtpHeloCommandHandler;
import net.bytle.smtp.command.SmtpQuitCommandHandler;
import net.bytle.smtp.command.SmtpRcptCommandHandler;
import net.bytle.vertx.ConfigIllegalException;

/**
 * A class that represents a domain with its data (hostname, postmaster)
 */
public class SmtpHost {

  /**
   * Domain is advertised in
   * * {@link SmtpHeloCommandHandler}
   * * {@link SmtpEhloCommandHandler}
   * * {@link SmtpQuitCommandHandler}
   * and is used for the postmaster email (ie postmaster@domain)
   * that can be received in a {@link SmtpRcptCommandHandler}
   */
  private final SmtpDomain domain;

  /**
   * The server name (used to sign
   * and add trace information such as the {@link net.bytle.email.BMailMimeMessageHeader#RECEIVED}
   * header
   * The hostname that reaches this server
   * It's advertised in the {@link net.bytle.smtp.command.SmtpHeloCommandHandler}
   */
  private final String hostedHostName;

  /**
   * The postmaster that should receive email
   * for any problem
   */
  private final SmtpPostMaster postmaster;

  public SmtpHost(conf conf) throws ConfigIllegalException {
    this.hostedHostName = conf.hostName.toLowerCase();
    this.domain = conf.hostedDomainName;
    try {
      postmaster = SmtpPostMaster.create(this, conf.postmaster);
    } catch (AddressException e) {
      throw new ConfigIllegalException("The postmaster email configuration for the host (" + this.hostedHostName + ") has a email value (" + conf.postmaster + ") that is not valid", e);
    }
  }

  public static SmtpHost.conf createOf(String cname) {
    return new SmtpHost.conf(cname);
  }

  @Override
  public String toString() {
    return hostedHostName;
  }


  public SmtpDomain getDomain() {
    return this.domain;
  }


  public String getHostedHostname() {
    return this.hostedHostName;
  }

  public SmtpPostMaster getPostmaster() {
    return postmaster;
  }

  public static class conf {
    private final String hostName;
    private SmtpDomain hostedDomainName;
    private String postmaster;

    public conf(String hostName) {
      this.hostName = hostName;
    }

    public conf setHostedDomain(SmtpDomain hostedDomain) {
      this.hostedDomainName = hostedDomain;
      return this;
    }

    public conf setPostmasterEmail(String postMasterEmailConf) {
      this.postmaster = postMasterEmailConf;
      return this;
    }

    public SmtpHost build() throws ConfigIllegalException {
      return new SmtpHost(this);
    }

  }
}
