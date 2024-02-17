package net.bytle.vertx;

/**
 * This class represents the apex domain of an app
 * ie in member.xxx.combostrap.com, the apex is combostrap.com
 * also known as top-level site or apex site
 * <p>
 * To be exact, the top-level domain name is `com`, we use therefore `apex`
 * The apex name are the 2 firsts label (ie the root, the tld and a label)
 */
public abstract class TowerApexDomain {


  private final String apexNameWithoutPort;

  private final String authority;
  //private final HttpServer httpServer;


  public TowerApexDomain(String apexName, int publicPort) {
    this.apexNameWithoutPort = apexName;
    if (publicPort != 80) {
      this.authority = this.apexNameWithoutPort + ":" + publicPort;
    } else {
      this.authority = this.apexNameWithoutPort;
    }

  }

  /**
   * The prefix of the domain
   * `cs` for combostrap for instance
   * It's used to add scope to cookie, variable, ...
   */
  public abstract String getPrefixName();

  /**
   * The name of the domain
   * `combo` for combostrap for instance
   */
  public abstract String getPathName();

  /**
   * The authority (ie dns name + optional port)
   * The authority contains the port, if not 80
   */
  public String getUrlAuthority() {
    return authority;
  }

  /**
   * @return the apex name without the port (cookie scope)
   */
  public String getDnsApexName() {
    return apexNameWithoutPort;
  }


  public String getAbsoluteLocalPath() {
    return "/" + this.getPathName();
  }


  @Override
  public String toString() {
    return apexNameWithoutPort;
  }

  public abstract String getRealmHandle();

  /**
   * @return the realm local id
   */
  public abstract Long getRealmLocalId();

  public abstract String getOwnerName();

  public abstract String getOwnerEmail();

  public abstract String getName();

}
