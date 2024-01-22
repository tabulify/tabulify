package net.bytle.vertx.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import net.bytle.type.Base64Utility;

import static net.bytle.vertx.auth.AuthQueryProperty.REALM_IDENTIFIER;

/**
 * A pojo to:
 * * define authentication state
 * * encode the auth state for OAuth to pass around
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthState {


  /**
   * The list guid
   */
  private static final String LIST_GUID = "listGuid";
  /**
   * The random value to stop any replay
   */
  private static final String RANDOM_VALUE = "randomValue";
  private final JsonObject jsonObject;

  public static AuthState createFromStateString(String state) {
    String jsonState = Base64Utility.base64UrlStringToString(state);
    return new AuthState(new JsonObject(jsonState));
  }


  /**
   *
   */
  public AuthState(JsonObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public static AuthState createEmpty() {
    return new AuthState(new JsonObject());
  }

  /**
   * If the registration occurs for a list
   *
   * @param listGuid - the list id
   */
  public void setListGuid(String listGuid) {
    this.jsonObject.put(LIST_GUID, listGuid);
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

  public void setRealmIdentifier(String realmIdentifier) {
    this.jsonObject.put(REALM_IDENTIFIER.toString(), realmIdentifier);
  }

  public String getRealmIdentifier() {
    return this.jsonObject.getString(REALM_IDENTIFIER.toString());
  }

}
