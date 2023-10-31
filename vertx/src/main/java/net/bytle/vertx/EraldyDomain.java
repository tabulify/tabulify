package net.bytle.vertx;

import net.bytle.exception.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the eraldy domain
 *
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
  private static final long ERALDY_ORGANIZATION_ID = 1L;
  private static final long ERALDY_REALM_ID = ERALDY_ORGANIZATION_ID;
  private static final String REALM_HANDLE = "eraldy";
  private static final String REALM_NAME = "Eraldy";

  private static final String USER_OWNER_NAME = "Nico";
  private static final String USER_OWNER_EMAIL = "nico@eraldy.com";


  private static EraldyDomain eraldyDomain;


  public EraldyDomain(String publicHost, HttpServer httpServer) {
    super(publicHost, httpServer);
  }

  public static EraldyDomain getOrCreate(HttpServer httpServer, ConfigAccessor configAccessor) {
    if (eraldyDomain != null) {
      return eraldyDomain;
    }
    String publicHost = configAccessor.getString(COMBO_APEX_DOMAIN_CONFIG_KEY, DEFAULT_VHOST);
    eraldyDomain = new EraldyDomain(publicHost, httpServer);
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
  public String getPathName() {
    return NAME;
  }

  @Override
  public String getRealmHandle() {
    return REALM_HANDLE;
  }

  @Override
  public Long getOrganisationId() {
    return ERALDY_ORGANIZATION_ID;
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
  public String getOwnerEmail() {
    return USER_OWNER_EMAIL;
  }

  @Override
  public String getName() {
    return REALM_NAME;
  }


  public boolean isEraldyId(Long realmId) {
    return getRealmLocalId().equals(realmId);
  }

  public void assertIsEraldyUser(Long realmId) {

    boolean isEraldyUser = eraldyDomain.isEraldyId(realmId);
    if (!isEraldyUser) {
      throw new IllegalArgumentException("This is not a " + eraldyDomain.getRealmHandle() + " user");
    }

  }

}
