package net.bytle.vertx;

import io.vertx.core.AbstractVerticle;
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
  private final AbstractVerticle verticle;
  private final String apexName;


  public TowerApexDomain(String apexName, AbstractVerticle verticle) {
    this.apexNameWithoutPort = apexName;
    this.verticle = verticle;
    int publicPort = ServerConfig.getPublicPort(verticle.config());
    if (publicPort != 80) {
      this.apexName = this.apexNameWithoutPort + ":" + publicPort;
    } else {
      this.apexName = this.apexNameWithoutPort;
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
    return this.verticle.getVertx();
  }

  public AbstractVerticle getVerticle() {
    return this.verticle;
  }

  public String getAbsoluteLocalPath() {
    return "/" + this.getPathName();
  }


  @Override
  public String toString() {
    return apexNameWithoutPort;
  }

  public abstract String getRealmHandle();

}
