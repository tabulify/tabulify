package net.bytle.vertx.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.Base64Utility;
import net.bytle.type.UriEnhanced;


/**
 * A pojo to:
 * * define authentication state
 * * encode the auth state for OAuth to pass around and enhance the user profile
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthState {


  /**
   * The list guid
   */
  private static final String LIST_GUID = "listGuid";
  /**
   * The random value to stop any replay
   */
  private static final String RANDOM_VALUE = "randomValue";
  private static final String REALM_GUID = "realmGuid";
  private static final String REALM_HANDLE = "realmHandle";
  private static final String APP_GUID = "appIdentifier";
  private static final String CLIENT_ID = "clientId";
  private static final String APP_HANDLE = "appHandle";
  private static final String OWNER_ORG_GUID = "ownerOrgGuid";
  private static final String OWNER_ORG_HANDLE = "ownerOrgHandle";

  private static final String REDIRECT_URI = "redirectUri";
  private final JsonObject jsonObject;

  /**
   * The external provider name that authenticate the
   */
  private String providerHandle;
  /**
   * The provider id
   */
  private String providerGuid;


  public static OAuthState createFromStateString(String state) throws CastException {
    String jsonState = Base64Utility.base64UrlStringToString(state);
    return new OAuthState(new JsonObject(jsonState));
  }


  /**
   *
   */
  public OAuthState(JsonObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public static OAuthState createEmpty() {
    return new OAuthState(new JsonObject());
  }

  /**
   * If the registration occurs for a list
   *
   * @param listGuid - the list id
   */
  public OAuthState setListGuid(String listGuid) {
    this.jsonObject.put(LIST_GUID, listGuid);
    return this;
  }

  public String getListGuid() {
    return this.jsonObject.getString(LIST_GUID);
  }


  /**
   * @param randomValue - a random value can be injected to mitigate replay attack
   */
  public void setRandomValue(String randomValue) {

    this.jsonObject.put(RANDOM_VALUE, randomValue);
  }

  public String toUrlValue() {
    return Base64Utility.stringToBase64UrlString(this.jsonObject.toString());
  }

  /**
   * @param realmIdentifier - the realm Identifier (used in analytics and to control the realm)
   */
  public OAuthState setRealmIdentifier(String realmIdentifier) {
    this.jsonObject.put(REALM_GUID, realmIdentifier);
    return this;
  }

  public String getRealmGuid() {
    return this.jsonObject.getString(REALM_GUID);
  }

  /**
   * @param appIdentifier - an identifier for the app (used in analytics)
   */
  public OAuthState setAppGuid(String appIdentifier) {
    this.jsonObject.put(APP_GUID, appIdentifier);
    return this;
  }

  public String getAppGuid() {
    return this.jsonObject.getString(APP_GUID);
  }

  /**
   * @param orgIdentifier - an identifier for the org (used in analytics)
   */
  public OAuthState setOrganisationOwnerGuid(String orgIdentifier) {
    this.jsonObject.put(OWNER_ORG_GUID, orgIdentifier);
    return this;
  }

  public String getOwnerOrganisationGuid() {
    return this.jsonObject.getString(OWNER_ORG_GUID);
  }

  /**
   * @param orgHandle - an identifier handle for the org (used in analytics)
   */
  public OAuthState setOrganisationOwnerHandle(String orgHandle) {
    this.jsonObject.put(OWNER_ORG_HANDLE, orgHandle);
    return this;
  }

  public String getOwnerOrgHandle() {
    return this.jsonObject.getString(OWNER_ORG_HANDLE);
  }

  /**
   * @param appHandle - an identifier handle for the app (used in analytics)
   */
  public OAuthState setAppHandle(String appHandle) {
    this.jsonObject.put(APP_HANDLE, appHandle);
    return this;
  }

  public String getAppHandle() {
    return this.jsonObject.getString(APP_HANDLE);
  }

  /**
   * @param realmHandle - an identifier handle for the realm (used in analytics)
   */
  public OAuthState setRealmHandle(String realmHandle) {
    this.jsonObject.put(REALM_HANDLE, realmHandle);
    return this;
  }

  public String getRealmHandle() {
    return this.jsonObject.getString(REALM_HANDLE);
  }

  public OAuthState setAuthProviderHandle(String providerHandle) {
    this.providerHandle = providerHandle;
    return this;
  }

  public OAuthState setAuthProviderGuid(String providerGuid) {
    this.providerGuid = providerGuid;
    return this;
  }

  public String getProviderHandle() {
    return this.providerHandle;
  }

  public String getProviderGuid() {
    return this.providerGuid;
  }

  public OAuthState setClientId(String clientId) {
    this.jsonObject.put(CLIENT_ID, clientId);
    return this;
  }

  public String getClientId() {
    return this.jsonObject.getString(CLIENT_ID);
  }

  public UriEnhanced getRedirectUri() {
    String redirectUriString = this.jsonObject.getString(REDIRECT_URI);
    if (redirectUriString == null) {
      return null;
    }
    try {
      return UriEnhanced.createFromString(redirectUriString);
    } catch (IllegalStructure e) {
      throw new RuntimeException("It should not happen because the set use the URIEnhanced object as signature", e);
    }
  }

  public OAuthState setRedirectUri(UriEnhanced redirectUri) {
    this.jsonObject.put(REDIRECT_URI, redirectUri.toString());
    return this;
  }
}
