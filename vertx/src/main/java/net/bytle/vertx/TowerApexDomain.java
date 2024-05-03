package net.bytle.vertx;

import net.bytle.type.Handle;

/**
 * This class represents the apex domain of an app
 * ie in member.xxx.combostrap.com, the apex is combostrap.com
 * also known as top-level site or apex site
 * <p>
 * To be exact, the top-level domain name is `com`, we use therefore `apex`
 * The apex name are the 2 firsts label (ie the root, the tld and a label)
 */
public class TowerApexDomain {


  private final String apexNameWithoutPort;

  private final String authority;
  private final String prefixName;
  private final Handle handle;


  public TowerApexDomain(TowerApexDomain.Builder builder) {
    this.apexNameWithoutPort = builder.publicHost;
    int publicPort = builder.httpServer.getPublicPort();
    if (publicPort != 80) {
      this.authority = this.apexNameWithoutPort + ":" + publicPort;
    } else {
      this.authority = this.apexNameWithoutPort;
    }
    this.prefixName = builder.prefixName;
    this.handle = builder.handle;

  }

  public static TowerApexDomain.Builder create(HttpServer httpServer, String defaultVhost) {
    return new TowerApexDomain.Builder(httpServer, defaultVhost);
  }

  /**
   * The prefix of the domain
   * `cs` for combostrap for instance
   * It's used to add scope to cookie, variable, ...
   */
  public String getPrefixName(){
    return this.prefixName;
  }

  /**
   * The name of the domain
   * `combo` for combostrap for instance
   */
  public String getFileSystemPathName(){
    return this.handle.getValue();
  }

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
    return "/" + this.getFileSystemPathName();
  }


  @Override
  public String toString() {
    return apexNameWithoutPort;
  }


  public String getName(){
    return this.handle.getValue();
  }


  public static class Builder {

    private String publicHost;
    private final HttpServer httpServer;
    private final String defaultVhost;
    private String prefixName;
    private Handle handle;

    public Builder(HttpServer httpServer, String defaultVhost) {
      this.httpServer = httpServer;
      this.defaultVhost = defaultVhost;
    }

    public TowerApexDomain.Builder setPrefixName(String prefixName) {
      this.prefixName = prefixName;
      return this;
    }

    public TowerApexDomain.Builder setHandle(Handle handle) {
      this.handle = handle;
      return this;
    }

    public TowerApexDomain build() {
      /**
       * On dev, the domain is named: eraldy.dev
       * On prod, the domain is named: eraldy.com
       */
      String COMBO_APEX_DOMAIN_CONFIG_KEY = handle.getValue()+".apex.domain";
      this.publicHost = httpServer.getServer().getConfigAccessor().getString(COMBO_APEX_DOMAIN_CONFIG_KEY, defaultVhost);
      return new TowerApexDomain(this);
    }
  }

}
