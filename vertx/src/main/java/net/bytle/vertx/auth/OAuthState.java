package net.bytle.vertx.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import net.bytle.type.Base64Utility;

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
  private static final String REALM_IDENTIFIER = "realmIdentifier";
  private static final String REALM_HANDLE = "realmHandle";
  private static final String APP_GUID = "appIdentifier";
  private static final String CLIENT_ID = "clientId";
  private static final String APP_HANDLE = "appHandle";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String ORG_HANDLE = "orgHandle";
  private final JsonObject jsonObject;

  /**
   * The external provider name that authenticate the
   */
  private String providerHandle;
  /**
   * The provider id
   */
  private String providerGuid;

  public static OAuthState createFromStateString(String state) {
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
    this.jsonObject.put(REALM_IDENTIFIER, realmIdentifier);
    return this;
  }

  public String getRealmIdentifier() {
    return this.jsonObject.getString(REALM_IDENTIFIER);
  }

  /**
   * @param appIdentifier - an identifier for the app (used in analytics)
   */
  public OAuthState setAppIdentifier(String appIdentifier) {
    this.jsonObject.put(APP_GUID, appIdentifier);
    return this;
  }

  public String getAppGuid() {
    return this.jsonObject.getString(APP_GUID);
  }

  /**
   * @param orgIdentifier - an identifier for the org (used in analytics)
   */
  public OAuthState setOrganisationGuid(String orgIdentifier) {
    this.jsonObject.put(ORG_IDENTIFIER, orgIdentifier);
    return this;
  }

  public String getOrganisationGuid() {
    return this.jsonObject.getString(ORG_IDENTIFIER);
  }

  /**
   * @param orgHandle - an identifier handle for the org (used in analytics)
   */
  public OAuthState setOrganisationHandle(String orgHandle) {
    this.jsonObject.put(ORG_HANDLE, orgHandle);
    return this;
  }

  public String getOrgHandle() {
    return this.jsonObject.getString(ORG_HANDLE);
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

}
