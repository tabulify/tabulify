package net.bytle.vertx;

import net.bytle.exception.InternalException;
import net.bytle.type.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the eraldy domain
 */
public class EraldyDomain extends TowerApexDomain {

  protected static final Logger LOGGER = LoggerFactory.getLogger(EraldyDomain.class);

  private static final String NAME = "eraldy";
  private static final String DEFAULT_VHOST = "eraldy.com";
  /**
   * On dev, the domain is named: eraldy.dev
   * On prod, the domain is named: eraldy.com
   */
  private static final String COMBO_APEX_DOMAIN_CONFIG_KEY = "eraldy.apex.domain";
  private static final long ERALDY_REALM_ID = 1L;
  private static final String REALM_HANDLE = "eraldy";
  private static final String REALM_NAME = "Eraldy";

  private static final String USER_OWNER_NAME = "Nico";
  private static final EmailAddress USER_OWNER_EMAIL = EmailAddress.ofFailSafe("nico@eraldy.com");


  private static EraldyDomain eraldyDomain;


  public EraldyDomain(String dnsName, int publicPort) {
    super(dnsName, publicPort);
  }

  public static EraldyDomain getOrCreate(HttpServer httpServer) {
    if (eraldyDomain != null) {
      return eraldyDomain;
    }
    String publicHost = httpServer.getServer().getConfigAccessor().getString(COMBO_APEX_DOMAIN_CONFIG_KEY, DEFAULT_VHOST);
    eraldyDomain = new EraldyDomain(publicHost, httpServer.getPublicPort());
    return eraldyDomain;
  }

  public static EraldyDomain get() {
    if (eraldyDomain == null) {
      throw new InternalException("The Eraldy domain should have been build");
    }
    return eraldyDomain;
  }

  @Override
  public String getPrefixName() {
    return "ey";
  }

  @Override
  public String getFileSystemPathName() {
    return NAME;
  }

  @Override
  public String getRealmHandle() {
    return REALM_HANDLE;
  }

  @Override
  public Long getRealmLocalId() {
    return ERALDY_REALM_ID;
  }

  @Override
  public String getOwnerName() {
    return USER_OWNER_NAME;
  }

  @Override
  public EmailAddress getOwnerEmail() {
    return USER_OWNER_EMAIL;
  }

  @Override
  public String getName() {
    return REALM_NAME;
  }


}
