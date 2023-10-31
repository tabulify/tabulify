package net.bytle.vertx;

import io.vertx.core.Vertx;

/**
 * This class represents the apex domain of an app
 * ie in member.xxx.combostrap.com, the apex is combostrap.com
 * also known as top-level site or apex site
 * <p>
 * To be exact, the top-level domain name is `com`, we use therefore `apex`
 */
public abstract class TowerApexDomain {


  private final String apexNameWithoutPort;

  private final String apexName;
  private final HttpServer httpServer;


  public TowerApexDomain(String apexName, HttpServer httpServer) {
    this.apexNameWithoutPort = apexName;
    int publicPort = httpServer.getPublicPort();
    if (publicPort != 80) {
      this.apexName = this.apexNameWithoutPort + ":" + publicPort;
    } else {
      this.apexName = this.apexNameWithoutPort;
    }
    this.httpServer = httpServer;

  }

  public HttpServer getHttpServer(){
    return this.httpServer;
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
   * The public domain host
   * for instance `combostrap.com`
   * <p>
   * The apex is the top level domain + its subdomain
   * <p>
   * We follow most of the web server out there, where
   * the apex name contains the port, if not 80
   */
  public String getApexName() {
    return apexName;
  }

  /**
   * @return the apex name without the port (cookie scope)
   */
  public String getApexNameWithoutPort() {
    return apexNameWithoutPort;
  }


  public Vertx getVertx() {
    return this.httpServer.getVertx();
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
   * @return the organisation local id
   */
  public abstract Long getOrganisationId();

  /**
   * @return the realm local id
   */
  public abstract Long getRealmLocalId();

  public abstract String getOwnerName();

  public abstract String getOwnerEmail();

  public abstract String getName();

}
